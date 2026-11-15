# QamqorMed

QamqorMed — учебная медицинская full-stack платформа. Текущий этап поддерживает структуру Казахстан → регион → город → медицинская организация → специальность → врач/медсестра, а также семейные профили пациента: один PATIENT-аккаунт может управлять несколькими подопечными и записывать каждого на приём отдельно.

## Стек и структура

- Frontend: React 19 + Vite
- Backend: Java 21 + Spring Boot 4.1.1 + Spring Security 7
- Database: PostgreSQL 14+
- JWT: JJWT 0.12.6
- Пароли: BCrypt

```text
backend/src/main/java/com/qamqormed/
├── config/       # CORS и Spring Security
├── controller/   # auth, patient/clinical workflow, medical structure и admin API
├── data/         # development-only demo users и medical structure
├── dto/          # request/response DTO
├── entity/       # User, Dependent, Appointment, MedicalRecord, NurseIntake и staff profiles
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

`JWT_SECRET` обязателен и не должен храниться в коде или Git. В production также задайте `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ALLOWED_ORIGINS`, `JWT_SECRET` и `JWT_EXPIRATION` явно. Production использует `ddl-auto=validate` и Flyway (`V1`–`V4`); перед первым запуском примените базовые миграции в порядке `medical_structure_postgresql.sql`, `family_patient_model_postgresql.sql`, `clinical_workflow_postgresql.sql`, `stage7_postgresql.sql`, `medical_data_postgresql.sql`, затем Flyway применит versioned migrations из `db/migration`. Development использует `ddl-auto=update`, а `dev_schema_update_postgresql.sql` содержит только безопасные compatibility-изменения для уже существующей локальной базы.

Если при `POST /api/doctor/appointments/{id}/start` появляется `Internal server error`, а в backend-логе есть `SQLState 42703` и `column r1_0.updated_at does not exist`, существующая база отстаёт от модели медицинских данных. Остановите backend, примените [medical_data_postgresql.sql](backend/src/main/resources/db/medical_data_postgresql.sql) к той же базе PostgreSQL, затем запустите backend снова. Эта миграция идемпотентна: она добавляет недостающие поля через `IF NOT EXISTS` и сохраняет записи.

Если при `POST /api/patient/appointments` в логе есть `column "confirmation_status" of relation "appointments" does not exist`, проверьте `DB_URL` и активный профиль. Для production запускайте backend с профилем `prod`: Flyway применит `V2` и repair-миграцию `V4__repair_appointment_notifications_schema.sql` к той же базе. Не исправляйте это через `ddl-auto=update` и не удаляйте таблицы или данные.

Для дополнительной загрузки выгруженного официального набора eGov задайте в development путь к JSON-файлу в `QAMQORMED_EGOV_CLINICS_FILE`. Поддерживаются массив строк или объект `{ "data": [] }`; строки со статусом deleted/removed/inactive/архив/удалённый пропускаются. Внешние строки должны содержать `id`, `name` (или `title`/`orgName`) и `cityCode` (или название города). Без этой переменной используется локальный снимок `backend/src/main/resources/data/clinics.json`.

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

На `/login` в development доступна компактная ссылка на `/dev`. Старого quick-login блока на странице входа нет: поиск пользователей, просмотр данных для входа и вход под DEMO выполняются в DEV Panel. Обхода Spring Security и отдельного `/api/demo-login` нет.

## QamqorMed Development Panel

При `VITE_APP_ENV=development` доступна отдельная страница `/dev`. Она не смешивается с обычным `/login` и позволяет server-side искать DEMO-врачей, медсестёр, пациентов и администраторов, фильтровать медицинскую иерархию, проверять `doctor_nurse_assignments`, видеть технические ID, телефон и пароль DEMO-пользователя и входить выбранным пользователем.

Dev API зарегистрирован только в Spring profile `dev` (`@Profile("dev")`):

```text
GET  /api/dev/users/search?role=DOCTOR&query=Ayan&regionId=...&cityId=...&clinicId=...&specialtyId=...&page=0&size=20
GET  /api/dev/users/{userId}/relation
POST /api/dev/login/{userId}
```

Quick login проверяет существование и активность пользователя, а для DOCTOR/NURSE — DEMO-профиль, затем возвращает обычный `AuthResponse` с JWT. В production dev-controller и dev-seed не создаются, а frontend route `/dev` выключен.

## API examples

## Medical structure

JPA-сущности и таблицы:

- `regions` — `code`, `name_ru/name_kk/name_en`, `type`, `active`;
- `cities` — `code`, `region_id -> regions.id`, локализованные названия;
- `clinics` — `city_id -> cities.id`, `region_id`, `source`, `external_source_id`, `is_real`, адрес и `active`;
- `specialties` — стабильный `code`, локализованные названия и `active`;
- `clinic_specialties` — связь клиники со специальностью, `demo=true` для учебных связей;
- `doctor_profiles` — `user_id -> users.id`, `specialty_id`, `clinic_id`, `seed_key`, `demo`, стаж, bio, кабинет;
- `nurse_profiles` — `user_id -> users.id`, `clinic_id`, `seed_key`, `demo`, `active`;
- `doctor_nurse_assignments` — уникальные `doctor_id` и `nurse_id`, активная связь 1:1.

Связи используют внешние ключи, а для `cities.region_id`, `clinics.city_id`, `doctor_profiles.clinic_id` и `doctor_profiles.specialty_id` созданы индексы. DTO не возвращают JPA-связи напрямую и не содержат пароль или hash.

Публичный (для любого авторизованного пользователя) API:

```text
GET /api/regions
GET /api/regions/{id}
GET /api/regions/{regionId}/cities
GET /api/cities/{id}
GET /api/cities/{cityId}/clinics
GET /api/clinics/{clinicId}/specialties
GET /api/clinics?page=0&size=50
GET /api/clinics/page?page=0&size=50
GET /api/clinics/{id}
GET /api/specialties
GET /api/specialties/{id}
GET /api/doctors?regionId=...&cityId=...&clinicId=...&specialtyId=...&search=...
GET /api/doctors/page?regionId=...&cityId=...&clinicId=...&specialtyId=...&search=...&page=0&size=20
GET /api/doctors/{id}
GET /api/doctors/{id}/nurse
```

Публичные списки показывают только активную иерархию. Admin API защищён `ROLE_ADMIN`:

```text
GET/POST/PUT /api/admin/regions
GET/POST/PUT /api/admin/cities
GET/POST/PUT /api/admin/clinics
GET/POST/PUT /api/admin/specialties
GET/POST/PUT /api/admin/doctors
GET/POST/PUT /api/admin/nurses
GET /api/admin/clinics/page?page=0&size=50
GET /api/admin/doctors/page?regionId=...&cityId=...&clinicId=...&specialtyId=...&search=...&page=0&size=20
GET /api/admin/nurses/page?regionId=...&cityId=...&clinicId=...&doctorId=...&search=...&page=0&size=20
```

`GET /api/doctors` и списочные admin-совместимые endpoint'ы возвращают только запрошенную страницу; для метаданных pagination используйте `/page`. `POST /api/admin/doctors` и `POST /api/admin/nurses` транзакционно создают `User` с принудительной ролью `DOCTOR`/`NURSE` и BCrypt-паролем. Обычная регистрация по-прежнему создаёт только `PATIENT`. Изменение `active=false` скрывает запись без удаления и не ломает внешние ключи.

Frontend:

- `/doctors` — каскадные select'ы региона, города, клиники и специальности, поиск по имени и карточки врачей;
- `/doctors/:id` — профиль врача с клиникой, городом, регионом, стажем, bio и закреплённой DEMO-медсестрой;
- Patient Dashboard содержит «Найти врача»;
- Admin Dashboard `/admin` содержит CRUD-разделы регионов, городов, клиник, специальностей, врачей и медсестёр; персонал ищется по ФИО и фильтруется по региону, городу, клинике, специальности/врачу с pagination.

Dev seed идемпотентно импортирует JSON-справочники: 20 регионов (17 областей + 3 города республиканского значения), 90 городов, 53 специальности и 31 активную официально названную медицинскую организацию из локального snapshot. Для каждой активной реальной организации создаются все 53 связи `Clinic × Specialty`, а также ровно один DEMO-врач и ровно одна отдельная DEMO-медсестра на каждую пару: ожидаемо `31 × 53 = 1643` пары в текущем локальном наборе. Seed запускается только в `dev`, использует стабильные `DEMO_DOCTOR_{clinicId}_{specialtyCode}` / `DEMO_NURSE_{clinicId}_{specialtyCode}` ключи, BCrypt и PostgreSQL-ограничения one-to-one; реальные ФИО сотрудников не импортируются.

Важно: `FULL CLINIC IMPORT NOT COMPLETED`. Доступный без API key локальный файл — это не полный реестр eGov, а официальный-name snapshot из 31 организации. Для полного импорта передайте eGov-совместимый JSON-экспорт через переменную `QAMQORMED_EGOV_CLINICS_FILE`; неактивные/удалённые записи будут пропущены. Проект не выдаёт этот локальный набор за полный реестр и не утверждает, что DEMO-сотрудники работают в реальных организациях.

Источники справочников: [Бюро национальной статистики — административно-территориальные единицы на 1 июля 2026 года](https://stat.gov.kz/ru/industries/social-tatistics/demography/publications/335821/) и [eGov Open Data — «Медицинские организации»](https://data.egov.kz/datasets/view?index=medical_organizations). Внешний eGov-набор требует API key для прямого API-доступа, поэтому локальная разработка использует сохранённый официальный-name snapshot и поддерживает импорт предоставленного JSON.

Быстрая проверка иерархии после входа:

```powershell
$regions = Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/regions -Headers @{ Authorization = "Bearer $token" }
$cities = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/regions/$($regions[0].id)/cities" -Headers @{ Authorization = "Bearer $token" }
$clinics = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/cities/$($cities[0].id)/clinics" -Headers @{ Authorization = "Bearer $token" }
$page = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/doctors/page?clinicId=$($clinics[0].id)&size=20" -Headers @{ Authorization = "Bearer $token" }
$nurse = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/doctors/$($page.content[0].id)/nurse" -Headers @{ Authorization = "Bearer $token" }
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

Свободные слоты и интеграции с внешними системами пока не входят в объём. Запись на выбранную дату/время, семейные профили, медицинская история и nurse intake реализованы внутри текущей модели.

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

Семейный patient workflow:

- `GET /api/patient/profile` — профиль владельца аккаунта — `PATIENT`;
- `PUT /api/patient/profile` — редактирование разрешённых полей профиля — `PATIENT`;
- `GET /api/patient/dependents` — активные подопечные текущего JWT-пользователя — `PATIENT`;
- `POST /api/patient/dependents` — создать подопечного без передачи `ownerId` — `PATIENT`;
- `GET/PUT/DELETE /api/patient/dependents/{id}` — профиль/изменение/soft-delete подопечного с ownership-check — `PATIENT`;
- `POST /api/patient/appointments` — запись за себя или за `dependentId` — `PATIENT`;
- `GET/DELETE /api/patient/appointments[/{id}]` — свои записи и отмена только созданных текущим пользователем — `PATIENT`;
- `GET /api/patient/medical-records` — только собственная история — `PATIENT`;
- `GET /api/patient/dependents/{id}/medical-records` — история выбранного подопечного — `PATIENT`;
- `GET /api/doctor/appointments` и `POST /api/doctor/appointments/{id}/medical-record` — приёмы и медицинская запись реального субъекта — `DOCTOR`;
- `GET /api/nurse/appointments` и `POST /api/nurse/appointments/{id}/intake` — приёмы и показатели реального субъекта — `NURSE`.

Patient location and scoped booking:

- `GET/PUT /api/patient/location` — preferred region/city from the current JWT patient; the backend rejects a city that belongs to another region;
- `GET /api/locations/regions` and `GET /api/locations/regions/{regionId}/cities` — onboarding/location selectors;
- `GET /api/specialties/by-city/{cityId}` — only specialties with active doctors in active clinics of the city;
- `GET /api/clinics/search?cityId=...&specialtyId=...` — only clinics with an active doctor of that specialty;
- `GET /api/doctors/search?cityId=...&specialtyId=...&clinicId=...&query=...` — paginated database-side doctor search; doctor results are not loaded on booking page before specialty selection;
- `GET /api/doctors/{doctorId}/time-slots` — persistent available slots, with deterministic demo slots created idempotently for demo doctors when requested;
- appointment creation validates city, specialty, clinic, doctor ownership of the slot, slot availability, and dependent ownership on the backend;
- `GET /api/dev/audit` reports missing Doctor/Nurse/Clinic/City links and Nurse/Doctor clinic mismatches in the development profile.

`Appointment.bookedBy` хранит владельца аккаунта, а nullable `Appointment.dependent` — реального пациента. При `dependent = null` субъектом является `bookedBy`; при наличии `dependent` история и отображение врача/медсестры относятся только к этому подопечному. `MedicalRecord` повторяет эти нормальные FK-связи и не использует polymorphic `subjectId`.

Запуск и проверки остаются прежними:

```powershell
cd backend
mvn test

cd ..\frontend
npm install
npm run build
```

## Этап 7: связанная медицинская система

Этап 7 добавляет каноническую медицинскую сущность `PatientProfile` и связывает её с владельцем аккаунта, подопечным, записью, приёмом, врачом и клиникой. Старые поля `User` и legacy-статусы сохранены для чтения уже существующих данных; новые записи используют канонические поля и статусы.

Основные сущности и таблицы:

- `patient_profiles` — ФИО, ИИН, дата рождения, пол, телефон, регион/город/клиника, timestamps;
- `dependents.patient_profile_id` и `appointments.patient_profile_id` — субъект лечения с ownership-check;
- `medical_visits.patient_profile_id/doctor_profile_id/clinic_id` — связанный приём;
- `icd10_codes`, `visit_diagnoses`, `prescriptions`, `medication_prescriptions` — структурированный диагноз и назначения;
- `lab_results`, `medical_certificates` — модели лабораторных результатов и справок;
- `appointments` — отменивший пользователь, причина отмены и timestamps переходов;
- `doctor_nurse_assignments` — ADMIN-управляемая активная пара врач–медсестра.

Для PostgreSQL применяйте миграции после базовых схем в следующем порядке:

```text
backend/src/main/resources/db/medical_structure_postgresql.sql
backend/src/main/resources/db/family_patient_model_postgresql.sql
backend/src/main/resources/db/clinical_workflow_postgresql.sql
backend/src/main/resources/db/stage7_postgresql.sql
```

Новый lifecycle записи: `BOOKED → CONFIRMED → ARRIVED → PREPARING → READY → IN_APPOINTMENT → COMPLETED`; из активных состояний разрешены `CANCELLED` и `NO_SHOW`. Переходы проверяются backend-сервисом, а действия врача и медсестры ограничены собственной ролью, клиникой и назначенной парой. Отмена пациента доступна через `DELETE /api/patient/appointments/{id}` или `POST /api/patient/appointments/{id}/cancel` с необязательной причиной; публикуется событие `AppointmentCancelled` для будущего waitlist.

Ключевые API этапа 7:

```text
GET  /api/patient/profile
PUT  /api/patient/profile
GET  /api/patient/appointments
POST /api/patient/appointments
POST /api/patient/appointments/{id}/cancel
GET  /api/icd10?query=...
POST/GET /api/doctor/appointments/{id}/diagnoses
POST /api/doctor/appointments/{id}/prescription-orders
POST /api/doctor/appointments/{id}/medications
GET  /api/patient/diagnoses?subjectId=...
GET  /api/patient/prescriptions?subjectId=...
GET  /api/patient/medications?subjectId=...
GET  /api/patient/lab-results?subjectId=...
GET  /api/patient/certificates?subjectId=...
POST /api/doctor/appointments/{id}/lab-results
POST /api/doctor/appointments/{id}/certificates
PATCH /api/nurse/appointments/{id}/status
GET/POST/DELETE /api/admin/doctor-nurse-pairs[/{doctorId}]
```

Frontend добавляет patient-разделы `/patient/diagnoses`, `/patient/prescriptions`, `/patient/medications`, выбор «я/подопечный», отображение структурированных медицинских данных и обновлённые workflow-статусы. Все patient/doctor/nurse/admin endpoint'ы защищены `SecurityConfig`; JWT role нельзя изменить через frontend. ИИН в DTO маскируется, пароли и password hash не возвращаются.

В dev автоматически создаются `PatientProfile` для старых demo-пользователей и небольшой ICD-10 каталог. Demo-доступ остаётся прежним: `+77000000001 / Patient123!`, `+77000000002 / Doctor123!`, `+77000000003 / Nurse123!`, `+77000000004 / Admin123!`.

## Этап 8: локальный QamqorMed AI

AI-ассистент подключён отдельным пакетом `com.qamqormed.ai` и доступен только после JWT-аутентификации. Он не получает медицинские записи, не имеет инструментов для SQL/ОС/HTTP-действий и получает роль только из проверенного `AuthenticatedUser`.

Backend API:

```text
GET    /api/ai/conversations
POST   /api/ai/conversations
GET    /api/ai/conversations/{id}/messages
DELETE /api/ai/conversations/{id}
POST   /api/ai/chat                 # обычный JSON fallback
POST   /api/ai/chat/stream          # text/event-stream для интерфейса
```

История хранится в PostgreSQL таблицах `ai_conversations` и `ai_messages`. Миграция `V5__ai_conversations_and_messages.sql` аддитивная: существующие пациентские данные не удаляются и не переписываются. В dev-профиле таблицы создаются через Hibernate `ddl-auto: update`.

Переменные окружения:

```text
AI_ENABLED=true
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen3:8b
OLLAMA_NUM_CTX=4096
AI_REQUEST_TIMEOUT=45s
AI_MAX_MESSAGE_LENGTH=4000
AI_MAX_HISTORY_MESSAGES=12
AI_MAX_CONCURRENT_REQUESTS=2
AI_MAX_REQUESTS_PER_MINUTE=20
```

Локальный запуск модели:

```powershell
ollama serve
ollama pull qwen3:8b
```

Ollama должна слушать только локальный интерфейс или защищённую внутреннюю сеть; её порт не следует публиковать в интернет. Если Ollama выключена, остальные разделы сайта продолжают работать, а AI показывает понятное сообщение о недоступности.

Для подключения backend на Render к Ollama через существующий HTTPS-туннель ngrok задайте в окружении backend:

- `OLLAMA_BASE_URL` — HTTPS-адрес туннеля без `/api/chat`.
- `OLLAMA_BASIC_AUTH_USERNAME` и `OLLAMA_BASIC_AUTH_PASSWORD` — логин и пароль Basic Auth, настроенные для туннеля. Храните их в секретных переменных окружения Render; не добавляйте в репозиторий или frontend.

Оба профиля (`dev` и `prod`) поддерживают эти переменные. Клиент отправляет `Authorization: Basic ...` только при заполненных логине и пароле, кодируя `username:password` в UTF-8 и Base64. Для локальной Ollama без авторизации оставьте обе переменные незаданными. Все запросы к Ollama, включая streaming, также отправляют `ngrok-skip-browser-warning: true` для обхода страницы предупреждения ngrok.

В production по умолчанию сохраняется `AI_ENABLED=false`; настройка туннеля сама по себе не включает ИИ. JWT-аутентификация пользователей QamqorMed продолжает работать независимо от Basic Auth туннеля.
