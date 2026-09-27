# Food Diary

Веб-приложение для ведения дневника питания: учите съеденное, считайте калории и БЖУ, находите состав продуктов во внешних базах (USDA, Open Food Facts) и получайте персональную норму нутриентов.

## Описание

Spring Boot-приложение на Kotlin с серверным веб-интерфейсом (FreeMarker) и REST API, описанным в OpenAPI-спецификации (клиентские интерфейсы и DTO генерируются автоматически). Пользователь регистрируется, заполняет профиль (пол, возраст, уровень активности, цель), и приложение рассчитывает дневную норму калорий и макронутриентов. Записи дневника привязываются к продуктам; состав продукта можно найти во внешних сервисах и сохранить в личную базу. Администратор управляет пользователями и общими продуктами через отдельную панель.

## Возможности

- **Дневник питания.** Записи по датам и типам приёмов пищи, создание, редактирование, удаление; расчёт калорий и БЖУ по 100 г и по весу порции, итоги за день, поиск «высококалорийных» записей.
- **Каталог продуктов.** Личные и публичные продукты, CRUD, поиск и сортировка, защита от удаления продуктов, использованных в дневнике.
- **Внешние источники нутриентов.** Поиск состава продукта в USDA FoodData Central и Open Food Facts; переключается флагами `USDA_ENABLED` / `OPEN_FOOD_FACTS_ENABLED`; результаты кешируются в Redis на 7 дней; названия переводятся через MyMemory.
- **Профиль и нормы.** Расчёт дневной нормы калорий, белков, жиров и углеводов по параметрам профиля (пол, уровень активности, цель).
- **Регистрация и роли.** Form login, валидация формы регистрации, роли `USER`/`ADMIN`, автоматическое создание администратора из переменных окружения при первом запуске.
- **Админ-панель.** Управление пользователями и продуктами из веб-интерфейса.
- **REST API.** `/api/diary`, `/api/diary/entries`, `/api/products`, `/api/products/nutrition-lookup` — спецификация в `src/main/resources/openapi/openapi.yaml`.
- **Миграции и тесты.** Flyway (V1–V6), unit- и интеграционные тесты контроллеров и сервисов, отчёт покрытия JaCoCo.

## Технологии

- Kotlin 2.3, Java 24
- Spring Boot 4.0 (Web MVC, Security, Data JPA, Validation, Redis, Flyway, WebClient)
- PostgreSQL 17, Redis 7
- FreeMarker (шаблоны), Bootstrap 5.3
- OpenAPI Generator 7.22 (kotlin-spring), Swagger-аннотации
- Gradle (Kotlin DSL), JaCoCo, Docker / Docker Compose, Traefik-лейблы для деплоя

## Запуск

### Локально (Gradle)

Понадобятся PostgreSQL (порт `5433`) и Redis (порт `6379`). Скопируйте `.env.example` в `.env` и заполните:

- `DB_NAME`, `DB_USER`, `DB_PASSWORD` — подключение к PostgreSQL;
- `FOOD_DATA_SERVICE_API_KEY` — ключ USDA FoodData Central ([получить](https://fdc.nal.usda.gov/api-key-signup.html));
- `USDA_ENABLED`, `OPEN_FOOD_FACTS_ENABLED` — включение внешних провайдеров;
- `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` — администратор, создаваемый при старте.

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

Поднимаются три сервиса: приложение (порт `8080`), PostgreSQL и Redis. Compose-файл содержит Traefik-лейблы (`DOMAIN`, `IP_HOST`, `CONTEXT_NAME`) для публикации за существующим Traefik-прокси.

### Тесты

```bash
./gradlew test
```

## Структура проекта

```text
src/main/kotlin/ru/kuzdikenov/fooddiary/
├── controller/
│   ├── api/        REST-контроллеры (дневник, продукты, поиск нутриентов)
│   └── web/        веб-интерфейс (авторизация, дневник, продукты, профиль, админка)
├── service/        бизнес-логика + nutrition/ (USDA, Open Food Facts) и translate/ (MyMemory)
├── repository/     Spring Data JPA
├── entity/         JPA-сущности (User, Product, FoodEntry, Goal, Role…)
├── mapper/, form/, dto/, converter/, validator/, exception/
├── config/         Security, Redis, WebClient, свойства провайдеров
src/main/resources/
├── db/migration/   Flyway-миграции V1–V6
├── openapi/        OpenAPI-спецификация REST API
├── templates/      FreeMarker-шаблоны
└── static/         CSS и JS
src/test/kotlin/    unit- и интеграционные тесты
```
