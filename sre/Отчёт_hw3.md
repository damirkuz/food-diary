# Отчёт: сбор логов food-diary в Victoria Logs

## 1. Что развёрнуто

Приложение food-diary 

В том же minikube-кластере развёрнута Victoria Logs в режиме single-node. Компоненты логирования находятся в namespace `logging`, приложение — в `food-diary-prod` и `food-diary-test`

| Компонент | Назначение | Версия Helm-чарта |
|---|---|---|
| Victoria Logs, релиз `vls` | Хранение и поиск логов | `vm/victoria-logs-single`, `0.13.10` |
| OpenTelemetry Collector, релиз `otel-logs` | Чтение контейнерных логов и доставка | `open-telemetry/opentelemetry-collector`, `0.176.0` |
| Grafana, релиз `grafana` | Дополнительный интерфейс поиска | `grafana-community/grafana`, `14.0.1` |
| vmalert и Alertmanager, релиз `vmalert-logs` | Проверка условия алерта и приём срабатываний | `vm/victoria-metrics-alert`, `0.51.0` |

Victoria Logs доступна внутри кластера через Service `vls-victoria-logs-single-server:9428`. Для данных используется PVC размером 3 GiB, срок хранения — семь дней

Манифесты приложения находятся в [real/base](real/base/), настройки окружений — в [real/overlays](real/overlays/). Конфигурация логирования:

- [victoria-logs-values.yaml](real/logs/victoria-logs-values.yaml);
- [otel-collector-values.yaml](real/logs/otel-collector-values.yaml);
- [grafana-values.yaml](real/logs/grafana-values.yaml);
- [vmalert-values.yaml](real/logs/vmalert-values.yaml).

Команды установки с закреплёнными версиями чартов приведены в [инструкции запуска](real/logs/README.md).

## 2. Формат логов приложения

Приложение пишет JSON в stdout через структурированное логирование Spring Boot и Logback. В `application.yaml` включён формат `logstash`, поле `@timestamp` переименовано в `timestamp`, добавлено поле `service.name`.

| Поле | Что содержит |
|---|---|
| `timestamp` | Время события с часовым поясом |
| `level` | Уровень: `INFO`, `WARN`, `ERROR` и другие |
| `message` | Текст сообщения |
| `service.name` | Имя приложения: `food-diary` |
| `logger_name` | Компонент, записавший сообщение |
| `event` | Тип прикладного события, если оно структурировано |
| `request_id` | Идентификатор HTTP-запроса для связи сообщений |

Для HTTP-запросов записывается событие `http_request_completed` с полями `http.method`, `http.route`, `http.status_code` и `duration_ms`. Ответы 4xx логируются на уровне `WARN`, 5xx — `ERROR`. Успешные запросы Kubernetes к health endpoints пропускаются, чтобы не засорять логи.

Дополнительно реализованы бизнес-события:

| Событие | Когда записывается | Дополнительные поля |
|---|---|---|
| `product_created` | Создан продукт | `product_id`, `outcome` |
| `food_entry_created` | Создана запись дневника | `food_entry_id`, `outcome` |
| `nutrition_lookup_completed` | Завершён поиск пищевой ценности | `outcome`, `source`, `cached`, `duration_ms` |


## 3. Как доставляются логи и почему выбран OTel

```text
food-diary → JSON в stdout
          → контейнерный runtime → /var/log/pods/.../*.log на ноде
          → OpenTelemetry Collector (DaemonSet)
          → OTLP/HTTP → Victoria Logs
          → vmui / Grafana
```

Collector развёрнут как DaemonSet: на каждой подходящей ноде работает один сборщик, читающий логи подов на этой ноде. В текущем minikube одна нода. Фильтр путей разрешает чтение только контейнера `food-diary` из namespaces `food-diary-test` и `food-diary-prod`.

Обработка состоит из нескольких шагов:

1. Receiver `file_log` читает файлы из `/var/log/pods`.
2. Оператор `container` разбирает контейнерную обёртку CRI/Docker.
3. Оператор `json_parser` извлекает поля JSON приложения, время и уровень сообщения. `service.name` переносится в resource attributes, текст сообщения — в body OTLP-записи.
4. Процессор `k8s_attributes` добавляет namespace, pod, ноду и имя контейнера. `memory_limiter` ограничивает потребление памяти, `batch` объединяет записи в пакеты.
5. Экспортёр отправляет записи на `/insert/opentelemetry/v1/logs` Victoria Logs по OTLP/HTTP со сжатием gzip.

Приложение уже пишет JSON: Collector не превращает обычный текст в JSON, а разбирает существующие поля и переводит запись в модель данных OpenTelemetry. Строки без JSON, например диагностика JVM, сохраняются как текст. В Victoria Logs время доступно как `_time`, сообщение — как `_msg`; прикладные поля остаются доступны для фильтрации.

**OTel выбран как стандарт телеметрии, потом подключим метрики и трейсы. В целом в будущем можно подумать над тем, чтобы приложение сразу отправляло в OTLP виде**.
DaemonSet позволяет собирать stdout всех реплик без дополнительного контейнера в каждом pod и без зависимости приложения от адреса Victoria Logs.

Позиции чтения и очередь отправки сохраняются в `/var/lib/otelcol` на ноде. Это помогает продолжить чтение после перезапуска Collector и пережить временную недоступность хранилища. Однако потеря ноды или удаление файлов до чтения может привести к потере логов; очередь не бесконечна. При первом обнаружении файла `start_at: end` читает только новые строки, поэтому для демонстрации нужно выполнить действия в приложении после запуска сборщика.

## 4. Работа с vmui

Проброс порта из кластера:

```bash
kubectl -n logging port-forward svc/vls-victoria-logs-single-server 9428:9428
```

Интерфейс: [http://localhost:9428/select/vmui/](http://localhost:9428/select/vmui/).

### 4.1. Сырые записи за последние 30 минут

```logsql
_time:30m service.name:food-diary k8s.namespace.name:food-diary-prod
| sort by (_time) desc
| limit 100
```

Запрос показывает до 100 последних записей приложения со всеми исходными полями, начиная с самых новых. Он нужен для первичной проверки доставки и просмотра структуры событий. В одной из строк следует раскрыть поля `level`, `service.name`, `event` и `k8s.pod.name`.

![img.png](img.png)

### 4.2. Фильтр по полю: поиск пищевой ценности

```logsql
_time:24h service.name:food-diary k8s.namespace.name:food-diary-prod
event:nutrition_lookup_completed
| sort by (_time) desc
```

Запрос выбирает только события завершения поиска по полю `event`. По `outcome` видно, нашлись ли данные, а по `source`, `cached` и `duration_ms` — откуда получен результат, использован ли кеш и сколько времени занял поиск. При проверке получена запись с `outcome=success`, `source=external_api`, `cached=false`.

![img_1.png](img_1.png)

### 4.3. Stats с группировкой по сервису и уровню

```logsql
_time:24h service.name:food-diary k8s.namespace.name:food-diary-prod
| stats by (service.name, level) count() as logs_count
| sort by (logs_count) desc
```

Запрос подсчитывает число записей каждого уровня для сервиса. Он показывает соотношение информационных сообщений, предупреждений и ошибок
![img_2.png](img_2.png)

### 4.4. Агрегация top: самые частые HTTP-маршруты

```logsql
_time:24h service.name:food-diary k8s.namespace.name:food-diary-prod
event:http_request_completed
| top 10 by (http.route)
```

Запрос показывает десять наиболее часто встречающихся маршрутов и число запросов в поле `hits`. Фильтр `event:http_request_completed` оставляет только записи завершения HTTP-запросов

![img_3.png](img_3.png)

### Дополнительные запросы

95-й перцентиль длительности HTTP-запросов по маршрутам:

```logsql
_time:24h service.name:food-diary k8s.namespace.name:food-diary-prod
event:http_request_completed
| stats by (http.route) count() as requests, quantile(0.95, duration_ms) as p95_ms
```

`p95_ms` — оценка длительности, ниже которой находится примерно 95% запросов данного маршрута

![img_4.png](img_4.png)


## 5. Бонус: Grafana и алерт

### Grafana

Установлен официальный плагин `victoriametrics-logs-datasource` версии `0.33.1`
```bash
kubectl -n logging port-forward svc/grafana 3000:80
```

В [Grafana](http://localhost:3000/) нужно открыть Explore и выбрать `VictoriaLogs`. Можно использовать те же LogsQL-запросы. Логин — `admin`, пароль берётся из Secret

Пароль:
```bash
kubectl -n logging get secret grafana \
>   -o jsonpath='{.data.admin-password}' | base64 --decode; echo
```

### Алерт на повторяющиеся HTTP 5xx

vmalert каждые 30 секунд выполняет запрос:

```logsql
_time:5m service.name:food-diary
k8s.namespace.name:food-diary-prod
event:http_request_completed
http.status_code:>=500 http.status_code:<600
| stats by (service.name) count() as errors
| filter errors:>=3
```

Если за последние пять минут обнаружено минимум три ответа 5xx и условие сохраняется одну минуту (`for: 1m`), алерт `FoodDiaryHttpErrors` переходит в `firing` и передаётся в Alertmanager. Фильтр по `event` исключает повторный подсчёт других сообщений об одной ошибке.

```bash
kubectl -n logging port-forward svc/vmalert-logs 8880:8880
kubectl -n logging port-forward svc/alertmanager-logs 9093:9093
```

Команды запускаются в разных терминалах. Интерфейсы: [vmalert](http://localhost:8880/vmalert/) и [Alertmanager](http://localhost:9093/)
