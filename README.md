# 💳 Bank Cards — Система управления банковскими картами

Backend-сервис на **Java 17 + Spring Boot** для управления банковскими картами, проведения внутренних транзакций и администрирования пользователей.

## 📋 Содержание

- [Функциональные возможности](#-функциональные-возможности)
- [Технологический стек](#-технологический-стек)
- [Архитектура проекта](#-архитектура-проекта)
- [Запуск приложения](#-запуск-приложения)
- [API Эндпоинты](#-api-эндпоинты)
- [Безопасность](#-безопасность)
- [Модели данных](#-модели-данных)
- [Разработка](#-разработка)

---

## 🚀 Функциональные возможности

### 👑 Администратор
- **Управление картами:** создание, блокировка, активация, удаление
- **Просмотр всех карт** с фильтрацией по владельцу и статусу
- **Управление пользователями:** просмотр списка, получение по логину, удаление
- **Защита от удаления:** администратора нельзя удалить; пользователя с положительным балансом — нельзя

### 👤 Пользователь
- **Просмотр своих карт** с пагинацией и фильтрацией по статусу
- **Запрос блокировки** собственной карты
- **Переводы между своими картами** (с пессимистической блокировкой)
- **Просмотр баланса** по номеру карты

### 🔒 Безопасность
- JWT-аутентификация (Access + Refresh токены)
- Ролевая модель: `ROLE_ADMIN`, `ROLE_USER`
- Шифрование номеров карт (AES-256)
- Маскирование номеров карт (`**** **** **** 1234`)
- Защита от race condition при переводах (`SELECT ... FOR UPDATE`)

---

## 🛠 Технологический стек

| Категория | Технология |
|-----------|-----------|
| **Язык** | Java 17 |
| **Фреймворк** | Spring Boot 3.5.x |
| **Безопасность** | Spring Security, JWT (jjwt 0.12.6) |
| **БД** | PostgreSQL 16 |
| **ORM** | Spring Data JPA, Hibernate |
| **Миграции** | Liquibase |
| **Контейнеризация** | Docker, Docker Compose |
| **Документация API** | SpringDoc OpenAPI (Swagger UI) |
| **Шифрование** | AES/ECB/PKCS5Padding |
| **Сборщик** | Maven |
| **Тестирование** | JUnit, Spring Boot Test, H2 (test) |

---

## 📂 Архитектура проекта

```
src/
├── main/
│   ├── java/eyeshead/bankcards/
│   │   ├── config/              # Конфигурации (Security, OpenAPI, CORS)
│   │   │   ├── OpenApiConfig.java
│   │   │   └── SecurityConfig.java
│   │   ├── controller/          # REST-контроллеры
│   │   │   ├── admin/
│   │   │   │   ├── AdminCardController.java
│   │   │   │   └── AdminUserController.java
│   │   │   ├── auth/
│   │   │   │   └── AuthController.java
│   │   │   └── user/
│   │   │       └── UserCardController.java
│   │   ├── dto/                 # DTO (request/response)
│   │   ├── entity/              # JPA-сущности
│   │   │   ├── BaseEntity.java
│   │   │   ├── Card.java
│   │   │   ├── RefreshToken.java
│   │   │   ├── Role.java
│   │   │   └── User.java
│   │   ├── exception/           # Исключения и глобальный обработчик
│   │   ├── mapper/              # Мапперы сущностей в DTO
│   │   ├── repository/          # Spring Data JPA репозитории
│   │   ├── security/            # JWT-фильтры, UserDetailsService
│   │   ├── service/             # Бизнес-логика
│   │   │   ├── admin/
│   │   │   ├── auth/
│   │   │   ├── user/
│   │   │   └── impl/
│   │   └── util/                # Шифрование, генерация, маскирование
│   └── resources/
│       ├── db/migration/        # Liquibase changelog'и
│       ├── application.yml
│       ├── application-dev.yml
│       └── application-prod.yml
└── test/
    └── java/eyeshead/bankcards/
```

---

## 🏃 Запуск приложения

### Предварительные требования
- Docker & Docker Compose
- Java 17+ (для локальной разработки)
- Maven 3.9+ (для локальной разработки)

### Быстрый старт (Docker Compose)

```bash
# 1. Клонировать репозиторий
git clone https://github.com/your-username/bank-cards.git
cd bank-cards

# 2. Запустить PostgreSQL + приложение
docker compose up -d --build

# 3. Проверить логи
docker compose logs -f app
```

Приложение будет доступно на `http://localhost:8080`.

### Локальный запуск (без Docker)

```bash
# 1. Запустить PostgreSQL (например, через Docker)
docker run -d \
  --name bank-cards-db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=bank_cards \
  -p 5432:5432 \
  postgres:16-alpine

# 2. Собрать и запустить приложение
mvn clean package -DskipTests
java -jar target/bank-cards-0.0.1-SNAPSHOT.jar
```

### Переменные окружения

| Переменная | Описание | Значение по умолчанию |
|-----------|----------|----------------------|
| `DB_URL` | JDBC URL PostgreSQL | `jdbc:postgresql://localhost:5432/bank_cards` |
| `DB_USERNAME` | Пользователь БД | `postgres` |
| `DB_PASSWORD` | Пароль БД | `postgres` |
| `JWT_SECRET` | Секретный ключ JWT (256 бит) | `01234567890123456789012345678901` |
| `ENCRYPTION_SECRET` | Ключ шифрования карт (256 бит) | `01234567890123456789012345678901` |

---

## 📡 API Эндпоинты

> **Swagger UI:** http://localhost:8080/swagger-ui.html  
> **OpenAPI JSON:** http://localhost:8080/api-docs

### 🔐 Аутентификация (`/api/auth`)

| Метод | Путь | Описание | Доступ |
|-------|------|----------|--------|
| `POST` | `/api/auth/register` | Регистрация нового пользователя | Все |
| `POST` | `/api/auth/login` | Вход в систему (получение JWT) | Все |
| `POST` | `/api/auth/refresh` | Обновление Access Token | Все (по Refresh Token) |

### 👑 Администрирование карт (`/api/admin/cards`)

| Метод | Путь | Описание | Доступ |
|-------|------|----------|--------|
| `POST` | `/api/admin/cards` | Создать карту для пользователя | `ADMIN` |
| `GET` | `/api/admin/cards` | Список всех карт (с фильтрацией) | `ADMIN` |
| `PATCH` | `/api/admin/cards/{cardNumber}/block` | Заблокировать карту | `ADMIN` |
| `PATCH` | `/api/admin/cards/{cardNumber}/activate` | Активировать/разблокировать карту | `ADMIN` |
| `DELETE` | `/api/admin/cards/{cardNumber}` | Удалить карту | `ADMIN` |

### 👑 Администрирование пользователей (`/api/admin/users`)

| Метод | Путь | Описание | Доступ |
|-------|------|----------|--------|
| `GET` | `/api/admin/users` | Список пользователей | `ADMIN` |
| `GET` | `/api/admin/users/{username}` | Информация о пользователе | `ADMIN` |
| `DELETE` | `/api/admin/users/{username}` | Удалить пользователя | `ADMIN` |

### 👤 Пользовательские операции (`/api/users`)

| Метод | Путь | Описание | Доступ |
|-------|------|----------|--------|
| `GET` | `/api/users/cards` | Мои карты (с пагинацией) | `USER` |
| `GET` | `/api/users/cards/{cardNumber}/balance` | Баланс карты | `USER` (владелец) |
| `POST` | `/api/users/cards/transfer` | Перевод между своими картами | `USER` |
| `PATCH` | `/api/users/cards/{cardNumber}/block` | Заблокировать свою карту | `USER` (владелец) |

---

## 🔐 Безопасность

### JWT-аутентификация
- **Access Token** — 15 минут, содержит username и роли
- **Refresh Token** — 7 дней, хранится в БД
- Подпись HMAC-SHA256 (секретный ключ 256 бит)

### Шифрование номеров карт
- Алгоритм: **AES/ECB/PKCS5Padding** (256 бит)
- Номера карт хранятся только в зашифрованном виде
- При запросах номера маскируются: `**** **** **** 1234`

### Ролевая модель
```
ROLE_ADMIN → полный доступ ко всем ресурсам
ROLE_USER → доступ только к собственным картам
```

### Защита транзакций
- Пессимистическая блокировка строки (`SELECT ... FOR UPDATE`)
- Транзакционность: `@Transactional` на все операции с финансами
- Валидация владения картой перед операцией

---

## 📊 Модели данных

### `users`
| Поле | Тип | Описание |
|------|-----|----------|
| `id` | BIGINT PK | Идентификатор |
| `username` | VARCHAR(50) UNIQUE | Логин |
| `password` | VARCHAR | Хэш пароля |
| `created_at` | TIMESTAMP | Дата создания |
| `updated_at` | TIMESTAMP | Дата обновления |

### `cards`
| Поле | Тип | Описание |
|------|-----|----------|
| `id` | BIGINT PK | Идентификатор |
| `encrypted_number` | VARCHAR UNIQUE | Зашифрованный номер карты |
| `owner_id` | BIGINT FK → users | Владелец |
| `expiry_date` | DATE | Срок действия |
| `status` | VARCHAR(20) | ACTIVE / BLOCKED / EXPIRED |
| `balance` | DECIMAL(19,2) | Баланс |
| `created_at` | TIMESTAMP | Дата создания |
| `updated_at` | TIMESTAMP | Дата обновления |

### `roles`
| Поле | Тип | Описание |
|------|-----|----------|
| `id` | BIGINT PK | Идентификатор |
| `name` | VARCHAR(20) UNIQUE | Название роли |

### `refresh_tokens`
| Поле | Тип | Описание |
|------|-----|----------|
| `id` | BIGINT PK | Идентификатор |
| `token` | VARCHAR UNIQUE | Refresh token |
| `expiry_date` | TIMESTAMP | Срок действия |
| `user_id` | BIGINT FK → users | Владелец токена |

---

## 🧪 Разработка и тестирование

```bash
# Сборка проекта
mvn clean compile

# Запуск тестов
mvn test

# Сборка JAR
mvn clean package -DskipTests

# Линтер (checkstyle)
mvn checkstyle:check
```

### Профили Spring
- **`dev`** — PostgreSQL, Liquibase `drop-first: true`, SQL логирование, Swagger включён
- **`prod`** — production-настройки (без `drop-first`, Swagger выключен)

```bash
# Запуск с dev-профилем
java -jar app.jar --spring.profiles.active=dev
```