# Отчёт: сбор метрик food-diary в VictoriaMetrics (ДЗ №4)

## 1. Что развёрнуто

Приложение food-diary развёрнуто в minikube. Для проверки метрик используется
окружение `food-diary-prod`: три реплики приложения, PostgreSQL и Redis.
VictoriaMetrics установлена отдельно в namespace `metrics` через Helm-чарт.

| Компонент | Назначение | Версия |
|---|---|---|
| food-diary | Дневник питания: пользователи, продукты, записи питания, поиск пищевой ценности | Образ `food-diary:1.0.4` |
| Kotlin, Java, Spring Boot | Стек приложения | Kotlin `2.3.21`, Java `25`, Spring Boot `4.0.6` |
| Spring Boot Actuator + Micrometer | Экспорт метрик JVM, фреймворка и бизнес-метрик | Micrometer `1.16.5` |
| PostgreSQL | Хранение доменных данных | `postgres:17-alpine` |
| postgres-exporter | Чтение статистики PostgreSQL и HTTP-экспорт метрик | `prometheuscommunity/postgres-exporter:v0.20.1` |
| VictoriaMetrics single-node, релиз `vmsingle` | Обнаружение таргетов, сбор и хранение временных рядов | Чарт `vm/victoria-metrics-single`, `0.49.0`; приложение `v1.154.0` |

VictoriaMetrics доступна внутри кластера через Service
`vmsingle-victoria-metrics-single-server.metrics.svc.cluster.local:8428`.
Данные хранятся на PVC объёмом 16 GiB; при проверке PVC находится в состоянии `Bound`.

Основные файлы:

- [vmsingle.yaml](../manifests/real/metrics/vmsingle.yaml) — Helm values и правила сбора;
- [deployment.yaml](../manifests/real/base/deployment.yaml) — приложение, порты, пробы и аннотации discovery;
- [statefulset.yaml](../manifests/real/base/statefulset.yaml) — PostgreSQL с дополнительным контейнером exporter;
- [application.yaml](../src/main/resources/application.yaml) — настройки Actuator;
- [BusinessMetrics.kt](../src/main/kotlin/ru/kuzdikenov/fooddiary/metrics/BusinessMetrics.kt) — регистрация и обновление бизнес-счётчиков.

## 2. Как собираются метрики и зачем нужен vmagent

Используется Pull-модель: сборщик периодически отправляет HTTP-запросы, получает
текущие значения метрик и сохраняет их в TSDB. Интервал сбора — 15 секунд.

```text
Запросы сбора:
VictoriaMetrics → каждый Pod food-diary:8081/actuator/prometheus
VictoriaMetrics → Pod PostgreSQL:9187/metrics → postgres-exporter → PostgreSQL:5432

Данные:
HTTP-ответы экспортёров → VictoriaMetrics → хранение и запросы через VMUI / Grafana
```

В этом развёртывании single-node VictoriaMetrics совмещает сборщик и хранилище:
включён `server.scrape.enabled`, а правила передаются через `-promscrape.config`.
Поэтому отдельный vmagent не устанавливается.

## 3. Метрики рантайма и фреймворка

В [build.gradle.kts](../build.gradle.kts) подключены готовые библиотеки:

```kotlin
implementation("org.springframework.boot:spring-boot-starter-actuator")
runtimeOnly("io.micrometer:micrometer-registry-prometheus")
```

Actuator предоставляет HTTP endpoint, Micrometer ведёт измерения приложения,
а Prometheus registry преобразует их в формат экспорта Prometheus/OpenMetrics.
Основной сервер приложения слушает 8080, management-сервер — отдельный порт 8081:

```yaml
management:
  server:
    port: 8081
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

Для сбора используется `/actuator/prometheus`, а не диагностический JSON endpoint
`/actuator/metrics`. В [SecurityConfig.kt](../src/main/kotlin/ru/kuzdikenov/fooddiary/config/SecurityConfig.kt)
доступ к Prometheus endpoint разрешён без пользовательской сессии.
Service приложения направляет трафик с 80 на 8080; Gateway также ведёт на этот
Service. Поэтому порт метрик через внешний маршрут приложения не публикуется.
При проверке endpoint на 8081 возвращает метрики, а на основном 8080 — `404`.
Доступ между Pod внутри кластера при необходимости дополнительно ограничивается NetworkPolicy.

| Метрика | Что показывает |
|---|---|
| `jvm_memory_used_bytes` | Используемая память JVM по областям и пулам |
| `jvm_threads_live_threads` | Число живых потоков JVM |
| `process_uptime_seconds` | Время работы процесса |
| `http_server_requests_seconds_count` | Число обработанных HTTP-запросов с метками маршрута, метода и статуса |

Это готовые технические метрики: их подсчёт не реализуется вручную в контроллерах.

Формат OpenMetrics проверяется запросом с соответствующим заголовком:

```bash
kubectl -n food-diary-prod exec deployment/food-diary -- \
  wget -qO- --header='Accept: application/openmetrics-text; version=1.0.0' \
  http://127.0.0.1:8081/actuator/prometheus
```

В проверенном ответе присутствуют описания `# TYPE`, значения метрик и завершающая
строка `# EOF` формата OpenMetrics.

## 4. Бизнес-метрики приложения

Реализованы четыре семейства метрик типа `Counter`:

| Метрика в VictoriaMetrics | Что считает | Где обновляется |
|---|---|---|
| `food_diary_registrations_total` | Успешные регистрации пользователей | [RegisterService](../src/main/kotlin/ru/kuzdikenov/fooddiary/service/RegisterService.kt) |
| `food_diary_products_creations_total` | Создание личных продуктов | [ProductService](../src/main/kotlin/ru/kuzdikenov/fooddiary/service/ProductService.kt) |
| `food_diary_entries_creations_total` | Создание записей дневника питания | [FoodEntryService](../src/main/kotlin/ru/kuzdikenov/fooddiary/service/FoodEntryService.kt) |
| `food_diary_nutrition_lookups_total` | Завершённые поиски пищевой ценности, включая кеш и ошибки | [NutritionLookupService](../src/main/kotlin/ru/kuzdikenov/fooddiary/service/NutritionLookupService.kt) |

Первые три счётчика увеличиваются только после успешного commit

У food_diary_nutrition_lookups_total есть две метки:

| Метка | Значения и смысл |
|---|---|
| `outcome` | `success` — продукт найден; `not_found` — результата нет; `external_error` — внешний провайдер завершился ошибкой и результата нет; `error` — необработанная ошибка поиска |
| `cached` | `true` — успешный ответ взят из Redis; `false` — ответ не из кеша либо поиск завершился ошибкой |


## 5. Экспорт метрик PostgreSQL

В Pod `postgres-0` работают два контейнера: PostgreSQL и postgres-exporter.
У них общий IP и сетевое пространство, поэтому exporter подключается к БД через
`localhost:5432`, а HTTP-сервер exporter слушает 9187.

Настройки подключения в [StatefulSet](../manifests/real/base/statefulset.yaml):

```yaml
- name: DATA_SOURCE_URI
  value: "localhost:$(POSTGRES_PORT)/$(POSTGRES_DB)?sslmode=disable"
- name: DATA_SOURCE_USER
  valueFrom:
    secretKeyRef:
      name: food-diary-secret
      key: DB_USER
- name: DATA_SOURCE_PASS
  valueFrom:
    secretKeyRef:
      name: food-diary-secret
      key: DB_PASSWORD
```

`POSTGRES_DB` и `POSTGRES_PORT` объявлены выше из ConfigMap; Kubernetes подставляет
их значения в URI. Пароль в манифест exporter не вписывается.

При обращении к `/metrics` exporter читает статистику PostgreSQL и возвращает
её сборщику. Сама PostgreSQL накапливает статистику независимо от этих запросов.
Например, доступны `pg_stat_database_numbackends` — число соединений с БД,
и `pg_stat_database_xact_commit` — количество завершённых транзакций.

## 6. Service discovery по аннотациям Pod

В шаблоне Pod приложения заданы аннотации:

```yaml
spec:
  template:
    metadata:
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8081"
        prometheus.io/path: "/actuator/prometheus"
```

У Pod PostgreSQL те же аннотации, но порт — `9187`, путь — `/metrics`.
Это описание для сборщика: сами аннотации не запускают HTTP-сервер и не включают опрос.

В VictoriaMetrics настроен общий job `kubernetes-pods`:

```yaml
- job_name: kubernetes-pods
  kubernetes_sd_configs:
    - role: pod
  relabel_configs:
    - action: keep
      source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
      regex: "true"
    - action: keep_if_equal
      source_labels:
        - __meta_kubernetes_pod_annotation_prometheus_io_port
        - __meta_kubernetes_pod_container_port_number
    - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
      regex: "(.+)"
      target_label: __metrics_path__
    - source_labels: [__meta_kubernetes_namespace]
      target_label: namespace
    - source_labels: [__meta_kubernetes_pod_name]
      target_label: pod
```

Сборщик получает список Pod из Kubernetes API во всех доступных namespace,
оставляет Pod с `scrape=true`, выбирает объявленный в аннотации порт и HTTP-путь.
У приложения остаётся 8081, у PostgreSQL — порт exporter 9187; SQL-порт 5432 исключается.
Метки `namespace` и `pod` сохраняются в полученных временных рядах.

Каждая реплика приложения становится отдельным таргетом. При смене IP или
создании нового Pod адрес обновляется через discovery, вручную редактировать
`targets` не нужно. Service для этого HTTP-опроса не требуется: запрос идёт на IP Pod.
Необходимые права `get/list/watch` для Pod выданы ServiceAccount VictoriaMetrics
через ClusterRole и ClusterRoleBinding Helm-чарта.

Правила сбора сохраняются в ConfigMap. Параметр
`promscrape.configCheckInterval: 30s` включает перечитывание изменённого файла.
Перезагрузка нужна при изменении правил; новые Pod по уже загруженным правилам
обнаруживаются автоматически.

## 7. Установка и проверка

Команды выполняются из корня проекта. Используется существующий minikube-кластер
с приложением и Gateway API из предыдущих домашних заданий.

```bash
docker build -t food-diary:1.0.4 .
minikube image load food-diary:1.0.4
kubectl apply -k manifests/real/overlays/prod
kubectl -n food-diary-prod rollout status deployment/food-diary --timeout=300s

helm repo add vm https://victoriametrics.github.io/helm-charts/
helm repo update
helm upgrade --install vmsingle vm/victoria-metrics-single \
  --version 0.49.0 -n metrics --create-namespace \
  -f manifests/real/metrics/vmsingle.yaml --wait
```

Для просмотра данных:

```bash
kubectl -n metrics get pods
kubectl -n metrics get svc
kubectl -n food-diary-prod get pods
kubectl -n metrics port-forward svc/vmsingle-victoria-metrics-single-server 8428:8428
```

Интерфейсы: [VMUI](http://localhost:8428/vmui/), [Targets](http://localhost:8428/targets),
[Service discovery](http://localhost:8428/service-discovery),
[загруженная конфигурация](http://localhost:8428/config).

### Доступность таргетов

```promql
up{job="kubernetes-pods", namespace="food-diary-prod"}
```

При проверке 25.10.2026 в Targets присутствуют три реплики food-diary на
`:8081/actuator/prometheus` и `postgres-0` на `:9187/metrics`. Все четыре таргета
имеют состояние `UP`, поле `lastError` пустое. Deployment приложения готов `3/3`,
у PostgreSQL готовы оба контейнера (`2/2`), Pod VictoriaMetrics готов `1/1`.

### Метрики JVM

```promql
sum by (pod) (jvm_memory_used_bytes{namespace="food-diary-prod", area="heap"})
```

Запрос показывает объём используемой heap-памяти каждой реплики.
`jvm_memory_used_bytes` сохраняется в VM: при проверке присутствуют 24 серии
с разными Pod, областями и пулами памяти.

```promql
jvm_threads_live_threads{namespace="food-diary-prod"}
```

Число живых потоков доступно отдельной серией для каждой из трёх реплик.

### Метрики PostgreSQL

```promql
pg_up{namespace="food-diary-prod"}
```

При проверке получено значение `1`: exporter подключается к PostgreSQL.

```promql
pg_stat_database_numbackends{namespace="food-diary-prod"}
```

Показывает число соединений по базам данных. Для проверки интенсивности транзакций:

```promql
rate(pg_stat_database_xact_commit{namespace="food-diary-prod"}[5m])
```

### Бизнес-метрики

Каждая строка ниже — отдельный запрос в VMUI или Grafana Explore:

```promql
sum by (namespace) (food_diary_registrations_total)
sum by (namespace) (food_diary_products_creations_total)
sum by (namespace) (food_diary_entries_creations_total)
sum by (namespace, outcome, cached) (food_diary_nutrition_lookups_total)
```

Результат проверки сохранённых метрик 25.10.2026:

| Событие, сумма по репликам `food-diary-prod` | Значение |
|---|---|
| Успешные регистрации | 1 |
| Созданные продукты | 1 |
| Созданные записи питания | 1 |
| Поиск: `success`, `cached=false` | 1 |
| Поиск: `external_error`, `cached=false` | 3 |
| Остальные комбинации меток поиска | 0 |

Ошибки внешнего поиска тоже попали в бизнес-метрику. Наличие `external_error`
не означает сбой скрейпа: состояние таргетов при этом `UP`.
Значения отражают момент проверки и меняются после новых действий или перезапуска Pod.

Количество созданных записей питания за последние 15 минут:

```promql
sum by (namespace) (increase(food_diary_entries_creations_total[15m]))
```

Для проверки HTTP API вместо VMUI можно использовать:

```bash
curl -fsSG http://localhost:8428/api/v1/query \
  --data-urlencode 'query=pg_up{namespace="food-diary-prod"}'

curl -fsSG http://localhost:8428/api/v1/query \
  --data-urlencode 'query=sum(food_diary_entries_creations_total{namespace="food-diary-prod"})'
```

## 8. Дополнительный просмотр и демонстрация

Можно использовать уже установленную Grafana в namespace `logging`:

```bash
kubectl -n logging port-forward svc/grafana 3000:80
```

В [Grafana](http://localhost:3000/) добавляется источник типа `Prometheus`
с URL `http://vmsingle-victoria-metrics-single-server.metrics.svc.cluster.local:8428`.
В Explore выполняются те же запросы, что в VMUI. Grafana не участвует в сборе:
она запрашивает уже сохранённые в VictoriaMetrics данные.

Для короткого скринкаста достаточно показать:

1. Pod и Service VictoriaMetrics в namespace `metrics`, готовность приложения и PostgreSQL.
2. Страницу Targets: три endpoint приложения и postgres-exporter в состоянии `UP`.
3. Запросы `pg_up`, метрик JVM и четырёх бизнес-счётчиков.
4. Создание записи питания через интерфейс приложения и увеличение соответствующего
   счётчика после очередного скрейпа.

Описание настройки и дополнительные команды находятся в
[инструкции по метрикам](../manifests/real/metrics/README.md).
Установка VictoriaMetrics выполнена на основе
[официального руководства](https://docs.victoriametrics.com/guides/k8s-monitoring-via-vm-single/).
