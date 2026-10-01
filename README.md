# QamqorMed

QamqorMed — учебная медицинская full-stack платформа. Этап 3 добавляет медицинскую структуру Казахстан → регион → город → клиника → специальность → врач, не переходя к расписанию и записи на приём.

## Стек и структура

- Frontend: React 19 + Vite
- Backend: Java 21 + Spring Boot 4.1.1 + Spring Security 7
- Database: PostgreSQL 14+
- JWT: JJWT 0.12.6
- Пароли: BCrypt

```text
backend/src/main/java/com/qamqormed/
├── config/       # CORS и Spring Security
├── controller/   # auth, medical structure и admin API
├── data/         # development-only demo users и medical structure
├── dto/          # request/response DTO
├── entity/       # User, Region, City, Clinic, Specialty и staff profiles
├── exception/    # единый JSON error handling
├── repository/   # repositories пользователей, структуры и профилей
├── security/     # JWT filter/service/UserDetails
├── service/      # AuthService
└── util/         # нормализация телефона

frontend/src/
├── api/          # централизованный API client
├── auth/         # AuthContext
├── components/   # guards и health status
├── pages/        # login, register, dashboards, поиск врача и admin CRUD
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

## Medical structure (Этап 3)

JPA-сущности и таблицы:

- `regions` — `name`, уникальный `code`, `active`, timestamps;
- `cities` — `region_id -> regions.id`;
- `clinics` — `city_id -> cities.id`, адрес, телефон, описание;
- `specialties` — уникальное имя и `active`;
- `doctor_profiles` — `user_id -> users.id`, `specialty_id`, `clinic_id`, стаж, bio, кабинет;
- `nurse_profiles` — `user_id -> users.id`, `clinic_id`, `active`.

Связи используют внешние ключи, а для `cities.region_id`, `clinics.city_id`, `doctor_profiles.clinic_id` и `doctor_profiles.specialty_id` созданы индексы. DTO не возвращают JPA-связи напрямую и не содержат пароль или hash.

Публичный (для любого авторизованного пользователя) API:

```text
GET /api/regions
GET /api/regions/{id}
GET /api/regions/{regionId}/cities
GET /api/cities/{id}
GET /api/cities/{cityId}/clinics
GET /api/clinics
GET /api/clinics/{id}
GET /api/specialties
GET /api/specialties/{id}
GET /api/doctors?regionId=...&cityId=...&clinicId=...&specialtyId=...&search=...
GET /api/doctors/{id}
```

Публичные списки показывают только активную иерархию. Admin API защищён `ROLE_ADMIN`:

```text
GET/POST/PUT /api/admin/regions
GET/POST/PUT /api/admin/cities
GET/POST/PUT /api/admin/clinics
GET/POST/PUT /api/admin/specialties
GET/POST/PUT /api/admin/doctors
GET/POST/PUT /api/admin/nurses
```

`POST /api/admin/doctors` и `POST /api/admin/nurses` транзакционно создают `User` с принудительной ролью `DOCTOR`/`NURSE` и BCrypt-паролем. Обычная регистрация по-прежнему создаёт только `PATIENT`. Изменение `active=false` скрывает запись без удаления и не ломает внешние ключи.

Frontend:

- `/doctors` — каскадные select'ы региона, города, клиники и специальности, поиск по имени и карточки врачей;
- `/doctors/:id` — профиль врача с клиникой, городом, регионом, стажем и bio;
- Patient Dashboard содержит «Найти врача»;
- Admin Dashboard `/admin` содержит CRUD-разделы регионов, городов, клиник, специальностей, врачей и медсестёр с loading/error/success состояниями.

В dev seed добавляются Жамбылская область → Тараз → `QamqorMed Demo Clinic`, специальности `Кардиолог`, `Терапевт`, `Педиатр`, а существующие demo doctor/nurse получают профили. Seed идемпотентен и не запускается в `prod`.

Быстрая проверка иерархии после входа:

```powershell
$regions = Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/regions -Headers @{ Authorization = "Bearer $token" }
$cities = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/regions/$($regions[0].id)/cities" -Headers @{ Authorization = "Bearer $token" }
$clinics = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/cities/$($cities[0].id)/clinics" -Headers @{ Authorization = "Bearer $token" }
$doctors = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/doctors?clinicId=$($clinics[0].id)" -Headers @{ Authorization = "Bearer $token" }
```

Проверка security: Patient/Doctor/Nurse на `POST /api/admin/clinics` получают `403`; Admin получает `201`. Повторный region code или specialty name даёт `409`, отрицательный/слишком большой стаж — `400`. После деактивации клиники через `PUT /api/admin/clinics/{id}` она и её врачи исчезают из публичных списков.

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

Расписание, свободные слоты, Appointment, медицинские карты и другие функции следующего этапа здесь не реализуются.

## Этап 3.5: кабинеты, role routing и языки

После успешного `POST /api/auth/login` frontend сохраняет Bearer JWT, получает канонические данные через `GET /api/auth/me` и направляет пользователя по роли:

```text
PATIENT -> /patient
DOCTOR  -> /doctor
NURSE   -> /nurse
ADMIN   -> /admin
```

`/api/auth/me` возвращает только безопасные поля пользователя. Для врача добавляются `specialty` и `clinic`, для медсестры — `clinic`; пароль, hash и security secrets в DTO отсутствуют. Роль назначается и проверяется backend, frontend не принимает роль от пользователя.

Frontend использует общий `DashboardLayout` внутри `AppLayout`: sidebar строится по роли, активный пункт выделяется, header содержит имя, профиль, выбор языка и выход. Меню и страницы-заглушки разделены по ролям:

- Patient: `/patient` и разделы записей, подопечных, карты и документов;
- Doctor: `/doctor` и разделы расписания, пациентов, приёмов, карт и направлений;
- Nurse: `/nurse` и разделы расписания, сегодняшних пациентов, показателей и приёмов;
- Admin: `/admin` и существующий CRUD медицинской структуры, подключённый к общему layout без второй реализации.

`ProtectedRoute` ждёт завершения проверки `/api/auth/me`, не показывает flash до загрузки и отправляет неавторизованных на `/login`. `RoleRoute` отправляет пользователя с неподходящей ролью на `/403`. Выход очищает token и заменяет текущую history entry, поэтому защищённый кабинет нельзя вернуть кнопкой Back без повторной авторизации.

Локализация находится в `frontend/src/i18n/`:

```text
frontend/src/i18n/
├── index.jsx
└── locales/
    ├── ru.json
    ├── kk.json
    └── en.json
```

Это небольшой i18n-провайдер без ручных `if` в JSX: язык меняется мгновенно, сохраняется в `localStorage` под ключом `qamqormed.language`, а по умолчанию используется `ru`. Чтобы добавить перевод, добавьте одинаковый dot-path в три JSON-файла и используйте `t('section.key')`. Для новой системной специальности можно расширить `localizeSpecialty` в `frontend/src/i18n/index.jsx`.

Чтобы добавить пункт меню роли, добавьте запись `[translationKey, path, icon]` в соответствующий массив `menus` в `frontend/src/layouts/AppLayout.jsx`, затем добавьте маршрут в `roleSectionRoutes` в `frontend/src/App.jsx` и ключ в JSON-словари.

Изменённые/добавленные backend endpoints этапа 3.5:

- `GET /api/auth/me` сохранён и расширен безопасными `specialty`/`clinic` для staff-профилей;
- существующие `GET/POST/PUT /api/admin/**` по-прежнему доступны только `ADMIN`;
- существующие `/api/patient/**`, `/api/doctor/**`, `/api/nurse/**` по-прежнему ограничены соответствующими ролями в `SecurityConfig`.

Запуск и проверки остаются прежними:

```powershell
cd backend
mvn test

cd ..\frontend
npm install
npm run build
```
