# QamqorMed

QamqorMed — учебная медицинская full-stack платформа. Этап 2 добавляет foundation для пользователей, авторизации и безопасности, не переходя к клиникам, врачам, расписанию или записи на приём.

## Стек и структура

- Frontend: React 19 + Vite
- Backend: Java 21 + Spring Boot 4.1.1 + Spring Security 7
- Database: PostgreSQL 14+
- JWT: JJWT 0.12.6
- Пароли: BCrypt

```text
backend/src/main/java/com/qamqormed/
├── config/       # CORS и Spring Security
├── controller/   # health, auth и временные role-test endpoints
├── data/         # development-only demo users
├── dto/          # request/response DTO
├── entity/       # User и Role
├── exception/    # единый JSON error handling
├── repository/   # UserRepository
├── security/     # JWT filter/service/UserDetails
├── service/      # AuthService
└── util/         # нормализация телефона

frontend/src/
├── api/          # централизованный API client
├── auth/         # AuthContext
├── components/   # guards и health status
├── pages/        # login, register, dashboards
├── utils/        # navigation
└── styles/
```

## Authentication

Публичные endpoint'ы:

- `GET /api/health`
- `POST /api/auth/register`
- `POST /api/auth/login`

Защищённые endpoint'ы:

- `GET /api/auth/me`
- `PUT /api/auth/change-password`
- `GET /api/patient/test`
- `GET /api/doctor/test`
- `GET /api/nurse/test`
- `GET /api/admin/test`

Регистрация всегда создаёт только `PATIENT`; роль из request DTO не принимается. Телефон нормализуется к виду `+77001234567`. Пароль до записи в БД хешируется `BCryptPasswordEncoder`. JWT stateless, живёт по умолчанию 2 часа и содержит `userId` и `role`.

Роли: `PATIENT`, `DOCTOR`, `NURSE`, `ADMIN`. Backend определяет authority из пользователя и JWT, поэтому изменение `role` во frontend не даёт доступа к чужому endpoint'у.

CSRF отключён только для API с Bearer JWT и stateless-сессиями: cookie-сессии для авторизации не используются. CORS принимает только настроенные origins и явно разрешённые заголовки `Authorization`, `Content-Type`, `Accept`, `Origin`.

Frontend хранит только access token в `localStorage`, централизованно добавляет `Authorization: Bearer ...`, при reload вызывает `/api/auth/me`, а при `401` очищает токен и переводит пользователя на `/login`. Для production медицинской системы следует рассмотреть `HttpOnly; Secure` cookies и дополнительные меры защиты от XSS.

## Environment variables

Скопируйте `backend/.env.example` в собственный источник переменных окружения. `.env` в Git не добавляется.

```text
DB_URL=jdbc:postgresql://localhost:5432/qamqormed
DB_USERNAME=qamqormed
DB_PASSWORD=your-local-password
CORS_ALLOWED_ORIGINS=http://localhost:5173
JWT_SECRET=at-least-32-random-characters-for-local-development
JWT_EXPIRATION=7200000
```

`JWT_SECRET` обязателен и не должен храниться в коде или Git. В production также задайте `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ALLOWED_ORIGINS`, `JWT_SECRET` и `JWT_EXPIRATION` явно. Production использует `ddl-auto=validate`; development использует существующий `ddl-auto=update`.

Frontend: `frontend/.env.example` содержит `VITE_API_URL=http://localhost:8080`. Production demo-панель в собранном frontend отсутствует: она создаётся только при `import.meta.env.DEV`.

## PostgreSQL и запуск

Локальная база (пример):

```sql
CREATE USER qamqormed WITH PASSWORD 'change_me_only_for_local_development';
CREATE DATABASE qamqormed OWNER qamqormed;
```

Из корня проекта в PowerShell:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/qamqormed"
$env:DB_USERNAME = "qamqormed"
$env:DB_PASSWORD = "your-local-password"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173"
$env:JWT_SECRET = "local-development-secret-change-me-32-chars"
$env:JWT_EXPIRATION = "7200000"

cd backend
mvn spring-boot:run
```

По умолчанию активен профиль `dev`. Для production:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Frontend:

```powershell
cd frontend
Copy-Item .env.example .env.local
npm install
npm run dev
```

Проверка сборок:

```powershell
cd backend
mvn test

cd ..\frontend
npm run build
```

## Development demo users

`DevDataInitializer` имеет `@Profile("dev")`, проверяет `existsByPhone` и создаёт пользователей только один раз. В `prod` demo users автоматически не создаются. Все данные ниже вымышленные и предназначены ONLY FOR LOCAL DEVELOPMENT.

| Роль | Телефон | Пароль |
|---|---|---|
| PATIENT | `+77000000001` | `Patient123!` |
| DOCTOR | `+77000000002` | `Doctor123!` |
| NURSE | `+77000000003` | `Nurse123!` |
| ADMIN | `+77000000004` | `Admin123!` |

На `/login` в development доступна панель «Тестовый вход», которая только заполняет обычную форму. Обхода Spring Security и отдельного `/api/demo-login` нет.

## API examples

Регистрация пациента:

```powershell
$body = @{ firstName="Demo"; lastName="Patient"; phone="+77001234567"; password="Password123!"; confirmPassword="Password123!" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/register -ContentType "application/json" -Body $body
```

Login:

```powershell
$body = @{ phone="+77000000001"; password="Patient123!" } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login -ContentType "application/json" -Body $body
$token = $login.accessToken
```

Текущий пользователь и role:

```powershell
Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/auth/me -Headers @{ Authorization = "Bearer $token" }
```

Ожидаемый `user` не содержит пароль или password hash.

## Security audit checklist

```powershell
# PATIENT получает 403 на doctor endpoint
Invoke-WebRequest -Method Get -Uri http://localhost:8080/api/doctor/test -Headers @{ Authorization = "Bearer $token" }

# Без токена /api/auth/me возвращает 401
Invoke-WebRequest -Method Get -Uri http://localhost:8080/api/auth/me
```

Проверьте также doctor token на `/api/admin/test` — должен быть `403`; каждый token на собственный role endpoint — `200`. В PostgreSQL значение `users.password_hash` должно начинаться с `$2a$` или `$2b$`, а не содержать исходный пароль.

Изменение `role` в сохранённом frontend user не меняет authority: доступ проверяется backend по подписанному JWT и записи пользователя. Ручное поле `role` в `/api/auth/register` игнорировать невозможно, потому что его нет в `RegisterRequest`; новый аккаунт всегда `PATIENT`.

Смена пароля:

```http
PUT /api/auth/change-password
Authorization: Bearer <token>
Content-Type: application/json

{
  "currentPassword": "Patient123!",
  "newPassword": "NewPassword123!",
  "confirmNewPassword": "NewPassword123!"
}
```

Будущее восстановление пароля можно добавить через одноразовый token, связанный с email или SMS. Реальных SMS/email сервисов на Этапе 2 нет.

## Git перед commit

```powershell
git status --short
git diff --check
git ls-files | Select-String -Pattern '(^|/)(\.env|\.env\.(local|development|production))$'
```

Убедитесь, что `.env`, production secrets, database passwords, JWT secret и реальные credentials не попали в Git. Не коммитьте `frontend/dist`, `frontend/node_modules` или `backend/target`.

Этап 2 заканчивается на authentication/security foundation. Клиники, профили специалистов, расписание, Appointment, медицинские карты и другие функции следующих этапов здесь не реализуются.
