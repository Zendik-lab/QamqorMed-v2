# QamqorMed UI

## Основа

Дизайн использует существующие React, CSS и i18n. Дополнительных runtime dependencies нет. `tokens.css` — единственный источник семантических цветов: primary — медицинский синий, secondary — бирюзовый, success/warning/danger/info — статусы. В этом же файле находятся шкалы текста, отступов, радиусов, теней, длительностей и слоёв. Breakpoint-токены документируют значения; CSS не поддерживает `var()` в условиях `@media`, поэтому условия используют те же литералы.

Порядок стилей в `main.jsx`: tokens → существующие clinical styles → shell → workspace → UI. Старые стили страниц пока сохраняются. При миграции конкретного кабинета переносить его геометрию на tokens и удалять только действительно заменённые правила; не удалять весь global.css сразу.

## Темы и чтение

`public/theme-init.js` сохраняет light/dark/system и разрешает system по `prefers-color-scheme`. Существующий store и `ThemeProvider` сохранены.

`public/accessibility-init.js` синхронно применяет `data-text-size` и `data-contrast` до React. Настройки сохраняются в `qamqormed.accessibility`, синхронизируются между вкладками и работают в памяти при недоступном storage. Увеличенный текст = 120% базового rem. Контрастный режим дополняет обе темы. При печати эти экранные настройки не изменяют медицинские документы.

## Компоненты

Экспорты находятся в `src/components/ui/index.js`. Для страниц можно импортировать конкретный модуль, чтобы не связывать независимые bundles.

| Модуль | Компоненты | Поведение |
| --- | --- | --- |
| Fields.jsx | Input, TextArea, Select, Checkbox, Radio, DatePicker | Связанные label/id, required, hint/error с aria-describedby, aria-invalid; нативные поля. DatePicker использует native date, без изменения timezone/business logic. |
| Feedback.jsx | Button, Card, Badge, Alert, Toast, LoadingSpinner, Skeleton, PageSkeleton, EmptyState, ErrorState, Tooltip | Localized default messages, semantic roles, busy/disabled states; tooltip открывается по hover/focus и закрывается Escape. |
| Dialog.jsx | Modal, Drawer, ConfirmDialog | Native dialog с portal, modal focus containment, Escape, закрытием по backdrop и возвратом фокуса. ConfirmDialog ждёт async operation и показывает ошибку, не закрывая подтверждение. |
| Popover.jsx | Dropdown (экспорт), Popover | Нативный disclosure с ordinary Tab navigation; Escape возвращает фокус, outside pointer закрывает. Не присваивать role=menu содержимому с полями. |
| Tabs.jsx | Tabs | Tablist/panels с устойчивыми id, roving tabindex и ArrowLeft/ArrowRight/Home/End. |
| Table.jsx | Table, Pagination | Поиск по value accessor, числовая/локализованная сортировка без мутации массива, pagination и empty/error/loading states. На мобильных — строки-карточки и отдельный select сортировки. |
| Icon.jsx | Icon | Inline SVG; декоративные иконки скрыты от screen reader. Название действия задаётся на кнопке/ссылке. |

Пример полей:

```jsx
<Input label={t('admin.name')} value={form.name} onChange={updateName}
  required error={errors.name} />
<Select label={t('admin.city')} value={cityId} onChange={updateCity}
  options={cities} optionLabel={(city) => city.name} />
```

Select принимает objects `{ id, name }`, strings или tuples `[value, label]`. `emptyLabel` заменяет локализованный placeholder, `allowEmpty={false}` исключает пустой option. Все пользовательские тексты передавать через существующий `t`; не записывать медицинские значения в UI logs.

Table принимает `caption`, `rows`, `columns` (`key`, `label`, необязательные `value(row)`, `render(row)`, `searchable`, `sortable`). `render` отвечает за отображение, `value` — за поиск и сортировку; не пытаться сортировать React nodes. `getRowKey` по умолчанию возвращает `row.id`. Клиентская Table предназначена для целиком загруженных справочников. Серверную пагинацию врачей/медсестёр и их API-фильтры сохранять.

## Навигация

`config/navigation.js` — общий список разрешённых ссылок четырёх ролей и mobile primary items. `WorkspaceSidebar` используется desktop и внутри mobile Drawer. Desktop collapse сохраняется отдельно в `qamqormed.sidebar.collapsed`. На ширине ≤900 px desktop sidebar имеет `display:none`, поэтому закрытая навигация не попадает в Tab order. На ≤700 px появляется нижнее меню всех четырёх ролей.

Это организация интерфейса; права доступа продолжают обеспечивать прежние RouteGuards и backend. Существующие пути, aliases и медицинские API не изменены. Dev-панель ограничена `import.meta.env.DEV`, её lazy import исключается из production bundle.

## Миграция

На общие поля переведены PatientPages, StaffPages, StaffWorkflowPages, StaffSectionPages, AdminDashboardPage и AdminDocumentTemplatesPage. Общий EmptyState используется на dashboard пациента и в медицинских разделах. В AdminDashboardPage на Table переведены регионы, города, клиники и специальности. Новая библиотека не означает завершения миграции всех клинических экранов: локальные drawer, documents и calendar interactions остаются задачей следующих этапов.
