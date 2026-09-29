# Food Diary

Веб-приложение для ведения дневника питания: учите съеденное, считайте калории и БЖУ, находите состав продуктов в открытой базе Open Food Facts и получайте персональную норму нутриентов.

## Описание

Spring Boot-приложение на Kotlin с серверным веб-интерфейсом (FreeMarker) и REST API, описанным в OpenAPI-спецификации (клиентские интерфейсы и DTO генерируются автоматически). Пользователь регистрируется, заполняет профиль (пол, возраст, уровень активности, цель), и приложение рассчитывает дневную норму калорий и макронутриентов. Записи дневника привязываются к продуктам; состав продукта можно найти во внешней базе и сохранить в личную. Администратор управляет пользователями и общими продуктами через отдельную панель.

## Возможности

- **Дневник питания.** Записи по датам и типам приёмов пищи, создание, редактирование, удаление; расчёт калорий и БЖУ по 100 г и по весу порции, итоги за день, поиск «высококалорийных» записей.
- **Каталог продуктов.** Личные и публичные продукты, CRUD, поиск и сортировка, защита от удаления продуктов, использованных в дневнике.
- **Внешний источник нутриентов.** Поиск состава продукта в Open Food Facts (без API-ключа, включается флагом `OPEN_FOOD_FACTS_ENABLED`); результаты кешируются в Redis на 7 дней.
- **Сессии в Redis.** HTTP-сессии аутентификации хранятся в Redis (Spring Session) — процессы приложения не держат состояния, реплики работают за балансировщиком без sticky-сессий.
- **Профиль и нормы.** Расчёт дневной нормы калорий, белков, жиров и углеводов по параметрам профиля (пол, уровень активности, цель).
- **Регистрация и роли.** Form login, валидация формы регистрации, роли `USER`/`ADMIN`, автоматическое создание администратора из переменных окружения при первом запуске.
- **Админ-панель.** Управление пользователями и продуктами из веб-интерфейса.
- **Мониторинг.** Spring Boot Actuator: `/actuator/health` (включая liveness/readiness), `/actuator/metrics`; healthcheck контейнера приложения в docker compose.
- **REST API.** `/api/diary`, `/api/diary/entries`, `/api/products`, `/api/products/nutrition-lookup` — спецификация в `src/main/resources/openapi/openapi.yaml`.
- **Миграции и тесты.** Flyway (V1–V6), unit-, слайс- и сквозной интеграционный тест (Testcontainers: PostgreSQL + Redis), отчёт покрытия JaCoCo.

## Технологии

- Kotlin 2.3, Java 25 (LTS)
- Spring Boot 4.0 (Web MVC, Security, Data JPA, Validation, Data Redis, Spring Session, Actuator, Flyway)
- PostgreSQL 17, Redis 7
- FreeMarker (шаблоны), Bootstrap 5.3
- OpenAPI Generator 7.22 (kotlin-spring), Swagger-аннотации
- Gradle (Kotlin DSL), JaCoCo, Testcontainers, Docker / Docker Compose

## Запуск

### Локально (Gradle)

Понадобятся PostgreSQL (порт `5433`) и Redis (порт `6379`). Скопируйте `.env.example` в `.env` и заполните:

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` — подключение к PostgreSQL;
- `REDIS_HOST`, `REDIS_PORT` — подключение к Redis (нужен и для сессий, и для кеша);
- `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` — администратор, создаваемый при старте;
- `OPEN_FOOD_FACTS_ENABLED` — внешний поиск продуктов (по умолчанию включён).

```bash
cp .env.example .env   # заполнить переменные
set -a; source .env; set +a   # экспортировать переменные в окружение
./gradlew bootRun
```

Миграции Flyway применяются при старте; отдельная цель `./gradlew flywayMigrate` читает те же переменные из `.env`.

### Через Docker Compose

```bash
cp .env.example .env   # заполнить переменные
docker compose up --build
```

Поднимаются три сервиса: приложение, PostgreSQL и Redis. Приложение доступно на `http://localhost:8080`, порт настраивается переменной `APP_PORT`.

### Прод-деплой за Traefik

Traefik-обвязка (лейблы, external-сеть, context-path) вынесена в `docker-compose.prod.yaml`:

```bash
docker compose -f docker-compose.yaml -f docker-compose.prod.yaml up -d --build
```

Файл ожидает переменные `CONTEXT_NAME`, `DOMAIN`, `IP_HOST` и внешнюю сеть `web_network` (Traefik). Чтобы порт 8080 не был опубликован на хосте (доступ только через прокси), задайте в `.env`: `APP_PORT=127.0.0.1:8080`.

### Тесты

```bash
./gradlew test
```

Сквозной интеграционный тест поднимает реальные PostgreSQL и Redis через Testcontainers — запущенный Docker обязателен.

## Структура проекта

```text
src/main/kotlin/ru/kuzdikenov/fooddiary/
├── controller/
│   ├── api/        REST-контроллеры (дневник, продукты, поиск нутриентов)
│   └── web/        веб-интерфейс (авторизация, дневник, продукты, профиль, админка)
├── service/        бизнес-логика + nutrition/ (Open Food Facts)
├── repository/     Spring Data JPA
├── entity/         JPA-сущности (User, Product, FoodEntry, Goal, Role…)
├── mapper/, form/, dto/, converter/, validator/, exception/
├── config/         Security, Redis, HTTP-клиент внешнего API, свойства
src/main/resources/
├── db/migration/   Flyway-миграции V1–V6
├── openapi/        OpenAPI-спецификация REST API
├── templates/      FreeMarker-шаблоны
└── static/         CSS и JS
src/test/kotlin/    unit-, слайс- и интеграционные тесты
```
