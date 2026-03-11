# Финальный план закрытия мобильного приложения

Документ заменяет прежнюю сухую матрицу и используется как основной рабочий план для доведения мобильного приложения до состояния, в котором его можно сдавать заказчику как законченный mobile-продукт.

Основания:
- оригинальное ТЗ: `app/ТЗ_Смарт-часы_Вариант 1.md`
- внутренний mobile spec: `app/MOBILE_APP_SPEC.md`
- текущая Android-кодовая база: `app/src/main`, `app/src/test`, `app/src/androidTest`
- TS-проект дизайнера: `typescript_mobile_app`
- Figma: `SMART - Макеты`, секция `Сырые макеты`, узел `112:1769`

Ключевая установка:
- прямой интеграции с часами пока нет
- backend ещё в разработке
- значит часть работ можно закрыть полностью сейчас, а часть можно довести только до integration-ready состояния и отложить финальную приёмку на момент появления боевых часов и API

---

## 1. Как трактовать объём работ

Чтобы не спорить про роли и границы, ниже используется жёсткое правило:

1. Всё, что относится к Android mobile app и может реально жить в приложении, считается частью mobile-контура.
2. Всё, что относится к watch app, backend и web-платформе, не считается задачей этого Android-репозитория, но отмечается как внешняя зависимость.
3. Если дизайн в Figma или TS даёт дополнительный mobile-слой поверх ТЗ, он включается в план, если:
   - не ломает операторский процесс,
   - полезен для мобильного продукта,
   - реалистичен без отдельного web-контура.

Из этого следует:
- `Login`, `Context`, `Issue`, `Return`, `Upload`, `Journal`, `DeviceList`, `Summary`, `Settings` - это ядро mobile из ТЗ.
- `Monitoring`, `Workers`, `Alerts`, `Maps`, `Worker Detail` - это расширенный mobile-слой из TS/Figma, который имеет смысл довести до консистентного состояния внутри одного приложения.
- web-admin, web-analytics и логика часов как устройства не входят в объём этой кодовой базы.

---

## 2. Что уже есть в приложении

### 2.1. Реализованные маршруты

По текущей навигации Android уже существуют:
- `Login`
- `ContextSelection`
- `Home`
- `DeviceList`
- `EmployeeSearch`
- `Maps`
- `WorkerDetail`
- `Issue`
- `Return`
- `Upload`
- `Journal`
- `Alerts`
- `Summary`
- `Settings`

### 2.2. Что уже покрыто по ТЗ

В основном уже есть:
- авторизация оператора;
- выбор площадки и смены;
- выдача часов;
- возврат часов;
- выгрузка пакетов по BLE;
- локальная очередь;
- фоновая синхронизация;
- журнал операций;
- операционная сводка;
- экран часов на площадке;
- поиск сотрудника;
- экран инцидентов;
- базовая профильная и сервисная часть.

### 2.3. Что уже перенесено из TS/Figma

По текущему состоянию уже адаптированы или частично перенесены:
- общий визуальный язык;
- shell, card-система, status badges;
- `Alerts`;
- `Maps`;
- `Worker Detail`;
- часть `Monitoring`-паттернов через `Home` и `Summary`;
- часть `Workers`-паттернов через `EmployeeSearch`.

### 2.4. Что важно про Figma

Из Figma секции `112:1769` подтверждены как отдельные mobile-референсы:
- `Мониторинг`
- `Алерты`
- `Карта`
- `Работники`
- `Карточка работника`

Это значит, что для полного mobile-контура по факту у нас есть два слоя:
- операторский слой из ТЗ;
- monitoring-layer из Figma/TS.

---

## 3. Что ещё не закрыто в мобильном приложении

Ниже только то, что относится именно к mobile app.

## 3.1. Missing screens и missing flows

### A. Экраны, которых не хватает как отдельных mobile-единиц

1. `MonitoringScreen`
- Сейчас его функции размазаны между `Home`, `Summary`, `Alerts`, `Maps`.
- Но по Figma это отдельный экран с:
  - контекстом зоны/смены;
  - KPI-сводкой;
  - блоком alert preview;
  - блоком active workers preview.
- Если цель закрыть mobile-контур полностью, такой экран стоит собрать явно.

2. `WorkersScreen`
- Сейчас роль этого экрана частично несёт `EmployeeSearch`.
- Но в Figma это не просто поиск, а monitoring-list с quick status и быстрым переходом в worker detail.
- Нужен отдельный режим списка работников:
  - фильтры `Все / Активны / Простой / Оффлайн`;
  - compact status row;
  - быстрый вход в карточку работника.

3. `IncidentDetailScreen` или полноценный `Alert Workflow`
- `AlertsScreen` уже есть, но Figma показывает два уровня:
  - summary + list;
  - list с action/status.
- Сейчас не хватает полного жизненного цикла события:
  - `новое`;
  - `в работе`;
  - `закрыто`;
  - комментарий;
  - явное действие оператора.

4. `RFID/NFC Scan Screen`
- В ТЗ сотрудник должен идентифицироваться табельным номером и/или RFID-пропуском.
- Сейчас есть поиск вручную.
- Нужен отдельный mobile-flow:
  - экран ожидания метки;
  - обработка ошибки чтения;
  - подтверждение найденного сотрудника.

5. `QR Scan Screen`
- ТЗ и пользовательские ожидания требуют сценариев через QR для часов.
- Нужен единый reusable scanner flow для:
  - выдачи;
  - возврата;
  - выбора часов на выгрузку;
  - перехода к устройству по `device_id`.

6. `Return Problem Sheet`
- При возврате сейчас не закрыт полностью блок причин проблем.
- Нужен отдельный экран, bottom sheet или dialog:
  - `поломка`;
  - `потеря`;
  - `нет связи`;
  - `не читается QR`;
  - `прочее`;
  - комментарий.

7. `Assignment Mode Selector`
- ТЗ требует режимы `по очереди / случайно / вручную`.
- Сейчас это не оформлено как явный пользовательский режим.
- Нужен отдельный UI-блок:
  - на экране выдачи;
  - возможно в настройках смены;
  - с запоминанием выбранного режима.

8. `Notification Settings` как часть профиля
- Не критично как отдельный большой экран, но как section или mini-screen нужно, если сдавать продукт законченно:
  - критичные инциденты;
  - ошибки выгрузки;
  - очередь неотправленных пакетов;
  - push/local уведомления.

### B. Flows, которые есть, но не завершены

1. `Issue flow`
- Есть поиск, выбор сотрудника, выбор часов, подтверждение.
- Не хватает:
  - RFID;
  - QR;
  - полного выбора режима назначения;
  - более прозрачного объяснения причин, почему часы недоступны;
  - полного сценария правки назначения до подтверждения.

2. `Return flow`
- Есть список активных привязок, возврат и `mark lost`.
- Не хватает:
  - возврата по QR;
  - возврата по device scan;
  - полного набора причин проблемы;
  - более явного post-return summary;
  - отдельного сценария "сначала выгрузить, потом закрыть".

3. `Upload flow`
- Архитектурно почти готов.
- Не хватает:
  - реального run с часами;
  - edge-state экрана для partial read/reconnect;
  - явного post-upload handoff на `Journal` или `Worker Detail`;
  - режима повторной выгрузки по конкретному failed packet.

4. `Alerts flow`
- Есть summary/list/filter по severity.
- Не хватает:
  - статуса обработки события;
  - комментариев;
  - audit trail по событию;
  - поиска по сотруднику/устройству;
  - deep-link в контекстный экран по типу проблемы.

5. `Maps flow`
- Экран есть, assets подтянуты.
- Не хватает:
  - переключения heatmap/worker layer с полной интерактивностью;
  - фильтра по статусам работников;
  - перехода в workers monitoring list;
  - подтверждённых реальных координат/зон, а не derived-модели.

6. `Worker Detail flow`
- Экран есть и уже близок к Figma.
- Не хватает:
  - полного рабочего сценария действий по инциденту;
  - возможного ручного комментирования/принятия решения;
  - реальных данных от сервера и часов;
  - верификации всех секций на production-данных.

---

## 4. Чего не хватает именно по экранам относительно Figma

Ниже не просто требования ТЗ, а именно расхождения с референсным mobile-дизайном.

### 4.1. Figma screen -> текущее состояние -> что добить

| Figma screen | Текущее состояние Android | Что осталось |
| --- | --- | --- |
| `Мониторинг` | Частично закрыт через `Home + Summary` | Собрать отдельный `MonitoringScreen` как самостоятельный экран с секциями `KPI + alerts preview + active workers preview`. |
| `Алерты` | Есть `AlertsScreen` | Довести workflow: поиск, статус обработки, комментарии, action-state. |
| `Карта` | Есть `MapsScreen` | Довести интерактивность, фильтры, переходы и привязку к реальным данным зон. |
| `Работники` | Частично закрыт через `EmployeeSearch` | Собрать отдельный `WorkersScreen` в monitoring-стиле, не завязанный только на поиск. |
| `Worker Detail` | Есть `WorkerDetailScreen` | Довести действия по инцидентам, финальную консистентность с Figma и реальные данные. |

### 4.2. Figma-specific детали, которые важно не потерять

Из сверки Figma сейчас особенно важно сохранить или довести:
- самостоятельный `Monitoring` как first-class screen;
- самостоятельный `Workers` как monitoring-list;
- alert list с обработкой статуса;
- route/timeline presentation в карточке работника;
- единый mobile rhythm между `Monitoring`, `Alerts`, `Maps`, `Workers`, `Worker Detail`.

---

## 5. Что можно закрыть сейчас без часов и backend

Это блок работ, который можно делать уже сейчас и довести до финального UI/UX и архитектурной готовности.

### 5.1. Экраны и UI

Можно полностью доделать:
- `MonitoringScreen`
- `WorkersScreen`
- `RFID/NFC Scan Screen` как UI и state machine
- `QR Scan Screen` как UI и state machine
- `IncidentDetailScreen`
- `Return Problem Sheet`
- `Assignment Mode Selector`
- `Notification Settings`

### 5.2. Архитектура и состояние

Можно полностью подготовить:
- контракты состояний и интенты;
- навигационные маршруты;
- локальные fake data и demo-источники;
- mapping из DAO/domain в monitoring UI models;
- событийную модель инцидентов;
- локальный lifecycle alert-status;
- отдельные use-case слои под scan / issue / return / incident actions.

### 5.3. Дизайн и визуальная консистентность

Можно полностью довести:
- spacing;
- typography;
- active/inactive states;
- search;
- pills;
- badges;
- empty/loading/error states;
- планшетные detail panes;
- реальные assets из TS/Figma.

---

## 6. Что нельзя честно считать завершённым без часов и backend

Ниже задачи, которые можно подготовить, но нельзя назвать окончательно закрытыми без интеграции.

### 6.1. Интеграция с часами

Нужно отложить на integration phase:
- реальное BLE discovery;
- реальное GATT-соединение;
- чтение чанков с часов;
- повторная передача при обрыве;
- ACK/NACK на реальных устройствах;
- корректная работа на нескольких моделях часов.

### 6.2. Интеграция с backend

Нужно отложить на integration phase:
- реальная авторизация против production-like API;
- реальный refresh token flow;
- `202/409/4xx/5xx` на gateway endpoint;
- реальная синхронизация справочников;
- реальные конфликты привязок;
- живой offline->online recovery.

### 6.3. Нефункциональная приёмка

Только после интеграции:
- производительность;
- crash-free;
- 8-часовой прогон смены;
- стабильность queue/sync;
- работа на целевых девайсах Samsung.

---

## 7. Финальный backlog до состояния "mobile ready"

Ниже не абстрактные идеи, а практический порядок работ.

## Фаза 1. Закрыть missing screen layer

### 1. Собрать `MonitoringScreen`
Цель:
- довести Figma `Мониторинг` до отдельного экрана.

Состав:
- контекст зоны/смены;
- KPI-блок;
- preview alerts;
- preview workers;
- быстрые переходы в `Alerts`, `Workers`, `Maps`.

Источник:
- Figma `87:2154`
- TS `MonitoringPage`

Статус:
- обязательно.

### 2. Собрать `WorkersScreen`
Цель:
- отделить monitoring list работников от обычного поиска.

Состав:
- search;
- filters `Все / Активны / Простой / Оффлайн`;
- compact worker cards;
- quick vitals/status row;
- переход в `WorkerDetail`.

Источник:
- Figma `Работники`
- TS `WorkersPage`

Статус:
- обязательно.

### 3. Довести `AlertsScreen` до полного workflow
Цель:
- сделать из списка инцидентов полноценный operational tool.

Добавить:
- поиск;
- статус события;
- комментарий;
- action buttons;
- deep-link по destination;
- detail mode.

Источник:
- Figma `Алерты`
- TS `AlertsPage`

Статус:
- обязательно.

## Фаза 2. Закрыть missing operator flows

### 4. Добавить `RFID/NFC Scan`
Цель:
- закрыть идентификацию сотрудника по ТЗ.

Сделать:
- reusable scanner screen;
- fallback на ручной ввод;
- error states;
- success handoff в `Issue`.

Статус:
- обязательно для полного operator readiness.

### 5. Добавить `QR Scan`
Цель:
- закрыть device scan для выдачи, возврата и выгрузки.

Сделать:
- единый scanner flow;
- параметры входа: `issue / return / upload / find device`;
- распознавание device payload;
- навигация в целевой сценарий.

Статус:
- обязательно.

### 6. Добавить `Assignment Mode Selector`
Цель:
- выполнить требование ТЗ по режимам назначения.

Сделать:
- `по очереди`;
- `случайно`;
- `вручную`;
- хранение default mode;
- видимая индикация активного режима на `IssueScreen`.

Статус:
- обязательно.

### 7. Довести `Return Problem Flow`
Цель:
- закрыть возврат с проблемами.

Сделать:
- sheet с причинами;
- комментарий;
- журналирование;
- локальный и серверный статус;
- действия при `не выгружено`, `нет связи`, `поломка`, `потеря`.

Статус:
- обязательно.

## Фаза 3. Довести monitoring-extension layer

### 8. Довести `MapsScreen`
Сделать:
- status filters;
- zone overlay toggle;
- worker selection;
- переходы между картой и workers list;
- реальную связь с incident context.

Статус:
- обязательно, если хотим закрыть mobile-layer по Figma.

### 9. Довести `WorkerDetailScreen`
Сделать:
- action area по инцидентам;
- комментарии и статус обработки;
- timeline polish по Figma;
- связку с `Alerts` и `Maps`.

Статус:
- обязательно.

### 10. Довести `Settings/Profile`
Сделать:
- notification settings;
- security/service section;
- возможно смену пароля, если это требуется backend flow.

Статус:
- желательно, для product polish.

## Фаза 4. Интеграционная готовность

### 11. Подготовить BLE integration harness
Сделать:
- mock/live режим;
- debug screens для BLE состояния;
- журналы подключения;
- simulation partial read/retry.

Статус:
- обязательно перед интеграцией.

### 12. Подготовить backend integration harness
Сделать:
- environment switching;
- API error fixtures;
- transport-level logging;
- staging config;
- feature flags для недоступных endpoint.

Статус:
- обязательно.

## Фаза 5. QA и сдача

### 13. Добить тестовый контур
Нужно добавить:
- unit tests для новых screen contracts;
- UI tests для login, issue, return, upload, alerts;
- integration-like tests для queue и sync;
- negative сценарии.

### 14. Провести device QA
Нужно прогнать:
- phone portrait;
- phone landscape;
- tablet portrait;
- tablet landscape;
- offline;
- slow network;
- BLE denied permissions;
- notification denied permissions;
- NFC unavailable device.

### 15. Финальный release layer
Нужно закрыть:
- release config;
- signing;
- crash reporting;
- production logging policy;
- финальный checklist UAT.

---

## 8. Приоритеты

### P0 - без этого нельзя говорить "mobile fully ready"

- `MonitoringScreen`
- `WorkersScreen`
- `RFID/NFC Scan`
- `QR Scan`
- `Assignment Mode Selector`
- `Return Problem Flow`
- `Alerts workflow`

### P1 - нужно для сильной сдачи и консистентного продукта

- доработка `MapsScreen`
- доработка `WorkerDetailScreen`
- notification settings
- production-quality QA

### P2 - можно делать после первого UAT, если заказчик не требует сразу

- расширенный supervisor-mode внутри mobile;
- дополнительные аналитические карточки;
- тонкая персонализация уведомлений;
- расширенный audit UI.

---

## 9. Что считать Done для mobile-приложения

Можно говорить, что mobile-контур закрыт, когда одновременно выполнены следующие условия.

### 9.1. Screen completeness

В приложении есть и работают:
- `Login`
- `ContextSelection`
- `Monitoring`
- `Workers`
- `Alerts`
- `Maps`
- `WorkerDetail`
- `Issue`
- `Return`
- `Upload`
- `Journal`
- `DeviceList`
- `Summary`
- `Settings/Profile`
- `RFID/NFC Scan`
- `QR Scan`
- `Incident detail/workflow`
- `Return problem flow`

### 9.2. Operator completeness

Оператор может:
- войти;
- выбрать контекст;
- найти сотрудника вручную, по RFID;
- выбрать часы вручную, по очереди, случайно;
- найти часы по списку и по QR;
- выдать часы;
- вернуть часы;
- выгрузить данные;
- обработать ошибку;
- увидеть очередь;
- увидеть инциденты;
- закрыть инцидент с комментарием.

### 9.3. Monitoring completeness

Мобильный monitoring-layer даёт:
- monitoring dashboard;
- worker list;
- maps;
- worker detail;
- alerts;
- связные переходы между этими экранами.

### 9.4. Integration readiness

Даже до живой интеграции в кодовой базе должны быть готовы:
- feature flags;
- mock/live switches;
- contracts под backend;
- contracts под BLE;
- обработка ошибок и edge-cases.

### 9.5. Final production readiness

Перед сдачей заказчику обязательно:
- реальные тесты с часами;
- реальные тесты с backend;
- оффлайн-проверка;
- release build;
- UAT checklist.

---

## 10. Рекомендуемый порядок следующей работы

Чтобы двигаться без хаоса, рекомендую идти так:

1. `MonitoringScreen`
2. `WorkersScreen`
3. `Alerts workflow`
4. `QR Scan`
5. `RFID/NFC Scan`
6. `Assignment Mode Selector`
7. `Return Problem Flow`
8. polish `Maps`
9. polish `WorkerDetail`
10. notification/profile settings
11. integration harness
12. QA и release

Это даст:
- сначала полный набор экранов;
- потом полный operator flow;
- потом integration-ready контур;
- потом финальную сдачу.

---

## 11. Финальный вывод

На текущий момент мобильное приложение:
- уже хорошо продвинуто по ядру operator app;
- уже частично захватило mobile monitoring-layer из TS/Figma;
- но ещё не закрыто как полностью готовый mobile-продукт для сдачи заказчику.

Чтобы закрыть mobile-контур полностью, ключевые оставшиеся дыры такие:
- нет отдельного `MonitoringScreen`;
- нет отдельного `WorkersScreen`;
- нет `RFID/NFC Scan`;
- нет `QR Scan`;
- нет полного `Alerts workflow`;
- нет полного `Return Problem Flow`;
- нет явного `Assignment Mode Selector`;
- нет завершённой integration-ready и UAT-ready стадии.

Именно эти блоки и должны стать следующим этапом разработки.
