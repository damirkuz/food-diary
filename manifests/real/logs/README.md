## Компоненты

Все компоненты логирования разворачиваются в namespace logging.

| Компонент | Релиз | Версия Helm-чарта | Файл |
|---|---|---|---|
| Victoria Logs | vls | 0.13.10 | victoria-logs-values.yaml |
| OTel Collector | otel-logs | 0.176.0 | otel-collector-values.yaml |
| Grafana с плагином Victoria Logs 0.33.1 | grafana | 14.0.1 | grafana-values.yaml |
| vmalert и Alertmanager | vmalert-logs | 0.51.0 | vmalert-values.yaml |

## Установка

Команды выполняются из корня проекта

~~~bash
helm repo add vm https://victoriametrics.github.io/helm-charts/
helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts/
helm repo add grafana-community https://grafana-community.github.io/helm-charts
helm repo update

helm upgrade --install vls vm/victoria-logs-single \
  --version 0.13.10 -n logging --create-namespace \
  -f manifests/real/logs/victoria-logs-values.yaml --wait

helm upgrade --install otel-logs open-telemetry/opentelemetry-collector \
  --version 0.176.0 -n logging \
  -f manifests/real/logs/otel-collector-values.yaml --wait

helm upgrade --install grafana grafana-community/grafana \
  --version 14.0.1 -n logging \
  -f manifests/real/logs/grafana-values.yaml --wait

helm upgrade --install vmalert-logs vm/victoria-metrics-alert \
  --version 0.51.0 -n logging \
  -f manifests/real/logs/vmalert-values.yaml --wait
~~~

Values передаются Helm и не включаются в resources Kustomize.

## Образ приложения и prod

~~~bash
./gradlew test bootJar
docker build -t food-diary:1.0.3 .
minikube image load food-diary:1.0.3
kubectl apply -k manifests/real/overlays/prod
kubectl -n food-diary-prod rollout status deployment/food-diary
~~~

## Доступ к интерфейсам

Каждая команда port-forward выполняется в отдельном терминале:

~~~bash
kubectl -n logging port-forward svc/vls-victoria-logs-single-server 9428:9428
kubectl -n logging port-forward svc/grafana 3000:80
kubectl -n logging port-forward svc/vmalert-logs 8880:8880
kubectl -n logging port-forward svc/alertmanager-logs 9093:9093
~~~

- Victoria Logs: http://localhost:9428/select/vmui/
- Grafana: http://localhost:3000/, источник VictoriaLogs доступен в Explore.
- vmalert: http://localhost:8880/vmalert/
- Alertmanager: http://localhost:9093/

Логин Grafana — admin. Пароль генерируется Helm и хранится в Secret:

~~~bash
kubectl -n logging get secret grafana \
  -o jsonpath='{.data.admin-password}' | base64 --decode
~~~