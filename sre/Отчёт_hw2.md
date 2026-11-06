# Отчёт: запуск приложения food-diary в Kubernetes (ДЗ №2)

## 1. Что развёрнуто

Приложение food-diary развёрнуто в локальном кластере **minikube** в **двух изолированных окружениях**:

| Окружение | Namespace | Реплики приложения | Дверь наружу | URL |
|---|---|---|---|---|
| test | `food-diary-test` | 1 | хост `:8080` | `http://test.food-diary.local:8080/` |
| prod | `food-diary-prod` | 3 | хост `:80` | `http://food-diary.local/` |

## 2. Абстракции Kubernetes, использованные для запуска

| Абстракция | Объекты | Зачем используется                                                                                                                                                                                                                                                          |
|---|---|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Namespace** | `food-diary-test`, `food-diary-prod` | изоляция окружений: раздельные наборы объектов, свои DNS-имена (короткие имена сервисов не конфликтуют), свои тома. Каждый overlay создаёт свой namespace (`namespace.yaml` + поле `namespace` в kustomization)                                                             |
| **ConfigMap** | `food-diary-configmap` | некритичная конфигурация      |
| **Secret** | `food-diary-secret` | чувствительные данные, подключается через `envFrom: secretRef`                                                                                                                                                                                                              |
| **StatefulSet** | `postgres` (1 реплика), `redis` (1 реплика) | для stateful-компонентов: стабильная сетевая идентичность (`postgres-0`), фиксированный `serviceName`, упорядоченное управление подами)                                     |
| **volumeClaimTemplates → PVC** | `postgres-data`, `redis-data` (1Gi, storageClass `standard`) | у каждой реплики свой постоянный том: данные переживают пересоздание пода                                                                                                                                                      |
| **Deployment** | `food-diary` | само приложение. Стратегия `RollingUpdate` с `maxSurge: 1, maxUnavailable: 0` — ёмкость не проседает во время выкатки. Число реплик задаётся overlay'ем (1 в тесте, 3 в проде)        |
| **Probe'ы** | `readinessProbe`, `livenessProbe` | приложение: `httpGet` на `/actuator/health/readiness` и `/actuator/health/liveness` (Spring Boot Actuator) — RollingUpdate направляет трафик только на готовые поды, liveness перезапускает зависший процесс |
| **Service (ClusterIP)** | `food-diary-service` (80→8080), `food-diary-db` (5432), `food-diary-redis` (6379) | стабильные DNS-имена и балансировка по selector. БД и Redis — только внутри кластера, наружу не публикуются                                                                                                                                                                 |
| **GatewayClass + Gateway + HTTPRoute** | класс `eg` (контроллер — Envoy Gateway), `food-diary-gateway` (listener :80), `food-diary-route` | внешний L7-вход: маршрутизация по заголовку `Host` на `food-diary-service:80`. Приложение отвечает **относительными** редиректами (`useRelativeRedirects`, `config/TomcatConfig.kt`), поэтому ему не нужен ни `x-forwarded-host`, ни привязка маршрута к порту двери        |
| **Kustomize** | `base` + `overlays` | сборка манифестов: одна база + патчи окружений (`replica-count`, configmap-значения, hostname). Применение: `kubectl apply -k overlays/test`                                                                                                                       |


## 3. Как сервисы приложения взаимодействуют между собой

### Внутри кластера — по DNS-именам Service'ов

Всё общение идёт через стабильные имена Service'ов одного namespace (полная форма: `<service>.<namespace>.svc.cluster.local`)

- **Приложение → PostgreSQL** (`food-diary-db:5432`): доменные данные; пул HikariCP до 10 соединений на под (3 реплики = до 30 против `max_connections=100`).
- **Приложение → Redis** (`food-diary-redis:6379`): HTTP-сессии аутентификации и кеш внешнего поиска. Сессии лежат в общем Redis, поэтому **любой под обслуживает любого пользователя**.

### Внешний трафик — через Gateway API

```
браузер: food-diary.local (прод) или test.food-diary.local:8080 (тест)
  → /etc/hosts → minikube (публикация портов Docker: 80→32443 — дверь прода, 8080→32444 — дверь теста)
  → nodePort «дверного» сервиса (food-diary-door-prod / food-diary-door-test, см. ниже)
  → Envoy: матч HTTPRoute по Host → rules.backendRefs
  → Service food-diary-service :80 (ClusterIP)
  → kube-proxy → один из готовых (readiness-probe) подов :8080
```

Каждое окружение имеет собственные Gateway и HTTPRoute в своём namespace — маршруты не пересекаются, окружения работают одновременно. Проблема «потерянного порта» в редиректах решена на уровне приложения: Tomcat настроен на **относительные** `Location` (`useRelativeRedirects`, `config/TomcatConfig.kt`) — браузер сам подставляет текущий `host:port`, поэтому приложению безразлично, за каким прокси и через какую дверь оно доступно; обработка `X-Forwarded-*` не требуется.

Envoy-поды живут в namespace контроллера (`envoy-gateway-system`), а Service выбирает поды только своего namespace, поэтому «дверные» сервисы `food-diary-door-test` (nodePort 32444) и `food-diary-door-prod` (nodePort 32443) созданы там же — `manifests/real/doors/` — с селектором по лейблам Envoy Gateway (`gateway.envoyproxy.io/owning-gateway-name/namespace`)
## 4. Запуск с нуля

```bash
docker build -t food-diary:1.0.1 .                    # образ приложения
minikube start --driver=docker --ports 80:32443 --ports 8080:32444
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/latest/download/standard-install.yaml
kubectl apply --server-side -f https://github.com/envoyproxy/gateway/releases/latest/download/install.yaml
minikube image load food-diary:1.0.1
kubectl apply -k manifests/real/overlays/test
kubectl apply -k manifests/real/overlays/prod
kubectl apply -k manifests/real/doors                  
# /etc/hosts: 127.0.0.1 test.food-diary.local, 127.0.0.1 food-diary.local
```
