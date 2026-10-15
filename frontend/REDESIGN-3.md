# QamqorMed — Redesign 3.0

## Исходное состояние и границы

Работа ведётся в существующем frontend, в ветке `codex/frontend-redesign-3`. До начала работы в checkout уже были незакоммиченные изменения backend и frontend, включая темы, footer и AI booking. Они сохранены. Backend, PostgreSQL, JWT, роли и контракты API не входят в этот этап.

## Аудит

- React 19, Vite, JavaScript/JSX. Только React и React DOM в runtime dependencies. Собственный history router (`utils/navigation.js`), guards в `RouteGuards.jsx`; добавлять второй router не требуется.
- Стили: `global.css` (~2940 строк), `tokens.css`, `shell.css`. Семантические цвета и ранний theme store уже существуют, но нет полной шкалы типографики, отступов, радиусов и слоёв. Плотность и формы различаются по страницам.
- Локализация: собственный контекст, JSON RU/KK/EN и inline fallback в `i18n/core.js`. 997 общих JSON-ключей. Проверка существующего скрипта проверяет равенство словарей, но не полноту всех текстов компонентов. Storage языка не защищён от исключений.
- Layout: общий footer и header, отдельный дублирующий AdminSidebar, Unicode/emoji иконки, отсутствуют desktop collapse, breadcrumbs и настройки доступности. Закрытый мобильный sidebar остаётся в DOM и требует исключения из tab order.
- Пациент: dashboard, профиль, регион/город, запись, приёмы, подопечные, история, диагнозы/назначения, документы, waitlist. Большой `PatientPages.jsx`; локальные Field/Select/EmptyState/Modal повторяются. Dashboard подавляет ошибки дополнительных запросов и может показывать их как пустые результаты. Требуется отдельный этап для подтверждения полноты booking и приватности ИИН.
- Врач: dashboard, профиль, day/week/month календарь, directory и карточка пациента, medical records, referrals, workflow приёма с МКБ-10 и документами. Формы и состояния переиспользуются слабо; drawer/modal реализованы локально.
- Медсестра: dashboard, профиль, календарь, очередь (`NurseTodayPage`) и показатели (`NurseVitalsPage`) уже имеют разное назначение. Сохранять существующие переходы статусов и API. `/nurse/schedule` перенаправляется в `/nurse/appointments`.
- Администратор: справочники, персонал с серверной пагинацией/фильтрами, расписания и document templates; dashboard одновременно выводит все формы справочников. Нет подтверждённого API общей статистики пользователей/пациентов и health status — вымышленные значения добавлять нельзя. `/admin/profile` — существующий placeholder.
- Состояния: локальные Loading/Empty/Field/Select/Status повторяются в DashboardPage, StaffPages, StaffSectionPages, StaffWorkflowPages, AdminDashboardPage, AdminDocumentTemplatesPage и PatientPages. Не все ошибки имеют retry или `role=alert`. Нужны общие компоненты и постепенная миграция.
- AI: существующий streaming/tool workflow сохраняется. Autoscroll без учёта позиции пользователя, удаление истории вложенным неклавиатурным элементом, нет повторной отправки после ошибки — последующий этап.
- Responsive: уже есть breakpoint-правила 320–1200px, нижнее меню для трёх ролей, полноэкранный AI. Нужна повторная проверка 320/375/768/1024/1440px после изменения shell. Таблицы, calendar, длинные поля и overlays требуют самостоятельного прохода.
- Performance: все страницы импортируются синхронно. Базовая сборка: JS 657.39 kB (gzip 174.24 kB), CSS 127.60 kB; предупреждение >500 kB. Dev-панель gated через `isDevelopment`, guards существуют.
- Базовая проверка: i18n, 6 AI/auth и 7 theme tests проходят. Production build проходит с разрешением доступа к локальным dependencies (sandbox блокирует `realpath`).

## Последовательность

1. Полные tokens и документация, light/dark/system с сохранением существующего store.
2. Общие Sidebar/Header/Footer: группы, SVG-иконки, collapse, breadcrumbs, профиль, мобильный drawer, доступность, Telegram Zendlless.
3. Библиотека небольших UI-компонентов: поля, статусы, состояния, popover, dialog/drawer, tabs, table и pagination. Мигрировать общие оболочки и повторяемые поля без изменения медицинских действий.
4. Пациент: booking, профиль/ИИН, dashboard с честными partial errors, документы и история.
5. Врач: dashboard, карточка, workflow и МКБ-10.
6. Медсестра: dashboard, очередь, быстрый ввод показателей.
7. Admin: отдельная главная и таблицы; доступность API пациентов/пользователей/ролей подтвердить по backend, не создавать фиктивных действий.
8. Календари, медицинские формы и документы, подтверждение опасных операций.
9. Полная проверка мобильных сценариев, доступности и RU/KK/EN; AI interactions.
10. Code splitting, интеграционные проверки на работающем backend, регрессии медицинского цикла.

## Выполненное и проверки

### Завершены этапы 1–3

- Аудит и план; CSS tokens типографики, spacing, radius, shadows, transitions, z-index и breakpoints. Медицинский синий primary и бирюзовый secondary; light/dark/system сохранены. Для полей усилены границы, для повышенного контраста добавлены отдельные override-палитры.
- Единый WorkspaceSidebar для четырёх ролей вместо дублирующей навигации. SVG-иконки, группы, active links, desktop collapse с сохранением и подсказками; настоящие ссылки поддерживают открытие в новой вкладке. Breadcrumbs, меню профиля, language/theme/accessibility controls, skip link и focus перехода к содержимому.
- Мобильный Drawer на native dialog: background inertness, keyboard focus containment, Escape, backdrop и focus restoration. Закрытый desktop sidebar исключён из tab order. Нижняя навигация теперь доступна и администратору. На 320 px с крупным текстом header controls не обрезаются.
- Режимы крупного текста и повышенного контраста применяются до React, сохраняются и синхронизируются между вкладками. Отказ localStorage не ломает настройки чтения и переключение языка.
- Footer дополнен реальными данными разработчика Zendlless/@Zendlless и Telegram-ссылкой. Непредоставленные контакты оператора не подменялись.
- Создана библиотека UI: Button, Input, TextArea, Select, Checkbox, Radio, DatePicker, Modal, Drawer, Tooltip, Dropdown, Tabs, Card, Badge, Alert, Toast, Table, Pagination, Skeleton, EmptyState, ErrorState, LoadingSpinner, ConfirmDialog и PageSkeleton. Контракты и область применения описаны в `DESIGN-SYSTEM.md`.
- Удалены дубли простых полей из шести модулей страниц. Общий EmptyState подключён к dashboard пациента и медицинским разделам. Регионы/города/клиники/специальности используют Table с поиском, статусным фильтром, сортировкой, pagination, mobile cards и empty state. Медицинские обработчики и API-вызовы сохранены.
- Добавлены lazy pages/calendar, Suspense skeleton и route error boundary. Production JS entry: **657.39 → 441.58 kB**, gzip **174.24 → 130.82 kB**; предупреждение Vite о chunk >500 kB устранено. CSS вырос с 127.60 до примерно 150 kB: удаление заменённых page-specific правил оставлено на миграцию страниц.
- Dev-панель gated через `import.meta.env.DEV`, исключена из production lazy imports даже при ошибочном `VITE_APP_ENV=development`.
- Все 38 новых строк добавлены в RU/KK/EN; прежние переводы и inline fallbacks сохранены.

### Изменённые в этом сеансе файлы

Созданы:

- `REDESIGN-3.md`, `DESIGN-SYSTEM.md`.
- `public/accessibility-init.js`, `scripts/test-design-system.mjs`.
- `src/config/navigation.js`.
- `src/components/WorkspaceSidebar.jsx`, `AccessibilitySettings.jsx`, `RouteErrorBoundary.jsx`.
- `src/components/ui/Icon.jsx`, `Popover.jsx`, `Dialog.jsx`, `Fields.jsx`, `Feedback.jsx`, `Tabs.jsx`, `Table.jsx`, `index.js`.
- `src/styles/workspace.css`, `src/styles/ui.css`.

Обновлены:

- `index.html`, `package.json`, `src/main.jsx`, `src/App.jsx`.
- `src/styles/tokens.css`, `src/layouts/AppLayout.jsx`.
- `src/components/AdminSidebar.jsx`, `Footer.jsx`, `NotificationCenter.jsx`.
- `src/config/environment.js`, `src/i18n/index.jsx`, `src/i18n/locales/{ru,kk,en}.json`.
- `src/pages/DashboardPage.jsx`, `PatientPages.jsx`, `StaffPages.jsx`, `StaffWorkflowPages.jsx`, `StaffSectionPages.jsx`, `AdminDashboardPage.jsx`, `AdminDocumentTemplatesPage.jsx`.
- `scripts/test-theme.mjs`.

Некоторые из этих файлов уже имели незакоммиченные изменения до этого сеанса. В этом списке не приписываются себе прежние изменения AI, API, backend и global.css.

### Проверки

- После этапов 1, 2 и 3 выполнены production builds. В sandbox Vite выдаёт EPERM на realpath; сборки успешно выполнены с разрешённым доступом к установленным dependencies.
- `npm test`: 1035 ключей × 3 языка; 6 AI/auth, 8 theme/contrast и 5 design-system tests. Все проходят. Новые тесты проверяют сохранение/нормализацию/синхронизацию reading preferences, отказы storage, ссылки/группы четырёх ролей, наличие literal translation keys и контраст high-contrast palettes/границ полей/focus.
- В браузере: публичный login/dev, вход в четыре роли через отдельный loopback API, корректные role navigation links, desktop collapse, light/dark, сохранение крупного текста и контраста после reload.
- На главной пациента измерены 320, 375, 768, 1024, 1440 px — горизонтального переполнения нет. Проверено открытие mobile Drawer, focus inside и закрытие Escape с возвратом к кнопке. Отдельно проверен admin header и mobile table при 320 px с крупным текстом в dark mode — controls не обрезаны, строки становятся карточками.
- Admin Table: поиск одной из 23 тестовых строк, следующая страница (строки 11–20), descending sort (первая строка 23), inactive filter (8 строк), no-results, переключение основного заголовка Regions/Өңірлер и возврат RU.
- `git diff --check` проходит.

### Границы проверки и оставшиеся задачи

**Весь Redesign 3.0 ещё не завершён.** Этапы 4–9 и интеграционная часть этапа 10 остаются обязательными. Наличие UI primitives не означает, что все прежние локальные modal/calendar/document interfaces уже мигрированы.

Работающий Spring Boot во время проверки был недоступен. Browser pass использовал временный read-only API `127.0.0.1:18085` и отдельный Vite `127.0.0.1:5175`; fixtures были помечены UI TEST и не подключены к production-коду, PostgreSQL или исходному Vite. Это проверка интерфейса, не подтверждение реального медицинского цикла. Booking, сохранение показателей, завершение приёма, выдачу документов и server authorization необходимо проверить с реальным backend.

Следующий этап — пациент:

1. Разделить ошибки дополнительных dashboard запросов и реальные пустые ответы; добавить retry и состояния данных.
2. Проверить/модернизировать полную цепочку booking и её подтверждение, сохранение выбора при ошибках, back navigation и disabled states.
3. Маскировать ИИН по умолчанию, проверить профиль и подопечных, убрать ненужные приватные сведения из summary.
4. Объединить card/empty/error/loading/document overlays на общей UI-библиотеке, сохранив существующие API.
5. Затем последовательно пройти врача, медсестру, admin, календари и документы. Admin profile остаётся существующим placeholder; API пользователей/пациентов/ролей и system health ещё требуется подтвердить.
6. AI: autoscroll с учётом позиции пользователя, retry, клавиатурное удаление истории и длинные сообщения; Ollama/auth конфигурацию сохранять.
7. Проверить populated calendars/forms/docs, все локализованные body texts и accessibility со screen reader; текущие проверки контраста не являются полной сертификацией WCAG 2.2 AA.
