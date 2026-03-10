# План редизайна Kotlin-приложения по референсу `typescript_mobile_app`

Дата: 2026-03-10  
Проект: `mobile_tracker`  
Референс-дизайн: `typescript_mobile_app`  
Цель: перенести визуальный язык и UX-паттерны из TypeScript-приложения в текущее Kotlin/Compose-приложение, не ломая существующую бизнес-логику, BLE-сценарии, offline-first поведение и адаптивность phone/tablet.

## 1. Что было проанализировано

### Kotlin-приложение

- Навигация: `app/src/main/java/com/example/mobile_tracker/presentation/navigation/AppNavGraph.kt`
- Маршруты: `app/src/main/java/com/example/mobile_tracker/presentation/navigation/Route.kt`
- Основные экраны:
  - `login/LoginScreen.kt`
  - `context_selection/ContextSelectionScreen.kt`
  - `home/HomeScreen.kt`
  - `devices/DeviceListScreen.kt`
  - `employees/EmployeeSearchScreen.kt`
  - `binding/issue/IssueScreen.kt`
  - `binding/return_device/ReturnScreen.kt`
  - `upload/UploadScreen.kt`
  - `journal/JournalScreen.kt`
  - `summary/SummaryScreen.kt`
  - `settings/SettingsScreen.kt`
- Theme/типографика:
  - `app/src/main/java/com/example/mobile_tracker/ui/theme/Theme.kt`
  - `app/src/main/java/com/example/mobile_tracker/ui/theme/Type.kt`
- Строки и пользовательская терминология:
  - `app/src/main/res/values/strings.xml`

### TypeScript-приложение

- Роутинг:
  - `typescript_mobile_app/src/app/routes.ts`
  - `typescript_mobile_app/src/app/App.tsx`
- Общий shell:
  - `typescript_mobile_app/src/app/components/Layout.tsx`
  - `typescript_mobile_app/src/app/components/BottomNav.tsx`
- Данные и контекст:
  - `typescript_mobile_app/src/app/context/AppContext.tsx`
  - `typescript_mobile_app/src/app/data/mockData.ts`
- Страницы:
  - `MonitoringPage.tsx`
  - `WorkersPage.tsx`
  - `WorkerDetailPage.tsx`
  - `MapsPage.tsx`
  - `AlertsPage.tsx`
- Визуальная система:
  - `typescript_mobile_app/src/styles/theme.css`
  - `typescript_mobile_app/src/styles/fonts.css`

## 2. Главный вывод после анализа

TypeScript-приложение и текущее Kotlin-приложение не являются двумя реализациями одного и того же продукта.

### Фактически сейчас

- `typescript_mobile_app`:
  - продукт про мониторинг строительной площадки;
  - сущности: работники, алерты, карта, тепловая карта, маршрут, телеметрия;
  - навигация: `Мониторинг`, `Работники`, `Карты`, `Алерты`.
- Kotlin-приложение:
  - продукт оператора выдачи/возврата часов;
  - сущности: площадка, смена, сотрудники, часы, привязки, выгрузка BLE, журнал операций, сводка, настройки;
  - навигация: `Выдача`, `Возврат`, `Выгрузка`, `Журнал`, `Ещё`.

### Следствие

Перенос должен быть не "портом экранов один-в-один", а переносом:

1. визуального языка;
2. дизайн-токенов;
3. паттернов карточек, метрик, статусов, списков и detail-блоков;
4. композиции экранов;
5. поведения шапки/нижней навигации/индикаторов статуса.

Нельзя бездумно подменять Kotlin-навигацию навигацией TS-приложения, потому что это сломает текущий рабочий сценарий оператора.

## 3. Матрица соответствия экранов

| TypeScript экран | Kotlin аналог | Тип соответствия | Комментарий |
|---|---|---|---|
| `MonitoringPage` | `HomeScreen` + частично `SummaryScreen` | Частичное | В TS это дашборд мониторинга; в Kotlin нужно перенести стиль dashboard/panel layout, но сохранить действия выдачи/возврата/выгрузки/журнала. |
| `WorkersPage` | `EmployeeSearchScreen` | Частичное | Совпадает паттерн списка людей с фильтрами/поиском/статусными бейджами. Семантика данных разная. |
| `WorkerDetailPage` | detail-pane в `EmployeeSearchScreen` + частично `DeviceListScreen`/`SummaryScreen` | Частичное | В Kotlin нет полноценного отдельного worker detail; можно перенести визуальные блоки карточек, но не копировать сущности 1:1. |
| `AlertsPage` | частично `JournalScreen` + сигнальные блоки в `ReturnScreen`/`SummaryScreen` | Частичное | Прямого экрана алертов в Kotlin нет; можно перенести карточный язык для ошибок, предупреждений, статусов и batch actions. |
| `MapsPage` | Нет аналога | Нет | Новый функциональный экран. Нельзя включать в обязательный редизайн без отдельного решения по продукту. |
| Общий `Layout` | `AppScreenScaffold`, `HomeScreen`, secondary top bars | Частичное | Есть смысл перенести визуальный shell: верхний статус-бар, max-width phone container, bottom nav характер, отступы, ритм. |
| `BottomNav` | bottom nav в `HomeScreen` | Прямое по паттерну | Хороший кандидат на почти полный визуальный перенос с адаптацией под текущие вкладки Kotlin. |

## 4. Что можно переносить почти 1:1

### 4.1 Дизайн-система

- базовую палитру:
  - светлый фон `#F9F9F9`;
  - тёмный primary `#122C1E`;
  - зелёные статусы `#00A36A`, `#2D9552`;
  - warning `#D2A232`;
  - danger `#C43232`;
  - мягкие серые surface/outline на основе `rgba(34,34,38,...)`.
- форму:
  - карточки `8dp`, `12dp`, `16dp`;
  - pill/badge скругления `16dp`, `24dp`, `48dp`;
  - плотные внутренние отступы `8dp`, `12dp`, `16dp`.
- типографическую иерархию:
  - `Geist` для UI/labels/body;
  - `Commissioner` для крупных чисел/ключевых метрик;
  - компактные размеры с высокой плотностью.
- визуальные паттерны:
  - карточки-метрики;
  - чипы фильтра;
  - статусные точки;
  - компактные toolbar-блоки;
  - карточки списков с 2-3 строками данных;
  - мягкие разделения вместо тяжёлых границ.

### 4.2 Layout-паттерны

- фиксированный верхний системный/контекстный статус-бар;
- короткие page-title блоки вместо тяжёлых заголовков;
- card-first layout;
- bottom navigation с визуально выраженным активным табом;
- scroll container с большим нижним inset под nav bar;
- detail-страницы с крупным hero-блоком и последующими секциями.

## 5. Что переносить нельзя без адаптации

- TS-роуты `Мониторинг / Работники / Карты / Алерты`.
- mock-данные про пульс, шаги, координаты, маршрут и карту.
- карточки, где UI напрямую завязан на несуществующие в Kotlin доменные поля.
- web-специфичные вещи:
  - fixed `max-width: 393px`;
  - CSS-only blur/scrollbar tricks;
  - hover/desktop cursor semantics;
  - импорт `figma:asset/*`.

## 6. Неподвижные ограничения для Kotlin-редизайна

Во всех дальнейших работах нельзя ломать:

1. текущую навигационную структуру `Login -> ContextSelection -> Home -> рабочие сценарии`;
2. трехшаговый flow выдачи в `IssueScreen`;
3. flow возврата в `ReturnScreen`, включая предупреждение о невыгруженных данных;
4. BLE upload flow и его состояния в `UploadScreen`;
5. offline/queue/sync семантику;
6. tablet list/detail layout для:
   - `DeviceListScreen`
   - `EmployeeSearchScreen`
   - `JournalScreen`
   - `ReturnScreen`
7. существующие ViewModel/Contract/Intent/Effect слои;
8. локализованные строки и бизнес-терминологию Kotlin-приложения.

## 7. Стратегия редизайна

Правильная стратегия: сначала привести Compose-приложение к новой визуальной системе, потом постепенно пересобрать каждый совпадающий или частично совпадающий экран.

Не делать:

- массовый экран-за-экраном rewrite без общей design foundation;
- перенос TS-структуры поверх Kotlin-логики;
- смешивание нового дизайна с текущим Material3 по месту без новых токенов и общих компонентов.

Делать:

1. сначала foundation;
2. затем shell/navigation;
3. затем high-traffic экраны;
4. затем вторичные экраны;
5. затем polishing и regression.

## 8. Подробный поэтапный план

## Этап 0. Базовая фиксация текущего состояния

### Цель

Подготовить безопасную точку входа перед масштабным UI-редизайном.

### Задачи

- Зафиксировать список экранов и их текущие сценарии.
- Зафиксировать текущие локальные незакоммиченные изменения и не перетирать их без необходимости.
- Собрать baseline screenshots:
  - phone portrait;
  - tablet portrait;
  - tablet landscape.
- Зафиксировать текущие acceptance сценарии:
  - login;
  - context selection;
  - issue;
  - return;
  - upload;
  - journal;
  - summary;
  - settings.

### Результат

- Понимание, что сравниваем "до/после".
- Возможность быстро ловить визуальные и поведенческие регрессии.

## Этап 1. Извлечение дизайн-системы из TypeScript-референса

### Цель

Создать Compose-слой токенов, который позволит собирать экраны в стиле TS-приложения системно, а не вручную.

### Задачи

- Создать карту цветов TS -> Compose:
  - background;
  - surface;
  - surface-muted;
  - primary dark;
  - success/warning/error;
  - text primary/secondary/tertiary.
- Обновить `ui/theme/Color.kt` и `Theme.kt` под новую палитру.
- Ввести дополнительный набор app-level tokens:
  - card elevation policy;
  - corner radii;
  - content paddings;
  - chip styles;
  - status colors.
- Подготовить типографику:
  - подключить `Geist` и `Commissioner`, если шрифты будут локально добавлены в Android-проект;
  - либо временно сделать максимально близкий fallback, но планом считать обязательным переход на реальные шрифты.
- Подготовить reusable компоненты:
  - `MTCard`;
  - `MTMetricCard`;
  - `MTStatusDot`;
  - `MTBadge`;
  - `MTFilterChipRow`;
  - `MTSearchField`;
  - `MTSectionHeader`;
  - `MTTopStatusBar`;
  - `MTBottomNavItem`.

### Результат

Новый визуальный foundation для всех следующих экранов.

## Этап 2. Пересборка общего shell приложения

### Цель

Сделать так, чтобы весь Compose-app визуально уже ощущался как TS-референс ещё до полного редизайна отдельных экранов.

### Задачи

- Переработать app scaffold:
  - фон экранов;
  - system/status bar color;
  - стандартные горизонтальные отступы;
  - нижний safe area under bottom nav.
- Переделать top app bar паттерн:
  - уменьшить визуальную тяжесть;
  - сделать компактные заголовки;
  - добавить контекстную вторичную строку там, где это уместно.
- Переделать bottom navigation в `HomeScreen`:
  - визуально приблизить к TS `BottomNav`;
  - сохранить текущие вкладки Kotlin (`Выдача`, `Возврат`, `Выгрузка`, `Журнал`, `Ещё`);
  - не менять маршрутную логику.
- Добавить единый верхний контекстный/status row:
  - online/offline/sync;
  - текущая площадка/смена/оператор.

### Результат

Даже без полного редизайна отдельных экранов приложение уже получит новую визуальную оболочку.

## Этап 3. Редизайн `HomeScreen` по мотивам `MonitoringPage`

### Цель

Сделать home не набором крупных M3-кнопок, а плотным dashboard-экраном в стиле TS.

### Что переносим из TS

- ритм секций;
- card-first layout;
- компактную метрику/статусную подачу;
- чёткие CTA;
- сочетание hero card + быстрые actionable секции.

### Что сохраняем из Kotlin

- вкладочную домашнюю навигацию;
- действия выдачи/возврата/выгрузки/журнала;
- счётчик pending packets;
- online/offline semantics;
- переходы в `Devices`, `Employees`, `Summary`, `Settings`.

### План по экрану

1. Убрать текущие "hero cards" в стиле Material demo.
2. Собрать новый home из:
   - верхнего контекстного блока;
   - блока ключевых счётчиков смены;
   - action cards для основных сценариев;
   - быстрого preview журнала/очереди/статусов.
3. Для вкладки `Ещё` перенести язык компактных list-card элементов.

### Результат

`HomeScreen` становится главным носителем нового дизайна и UX-настроения.

## Этап 4. Редизайн списков людей и устройств

### 4.1 `EmployeeSearchScreen` по мотивам `WorkersPage`

#### Что переносим

- search bar стиль;
- filter chips;
- карточный список;
- бейджи/статусы/вторичные строки;
- detail-pane в более визуально насыщенном виде.

#### Что сохраняем

- поиск по табельному номеру/ФИО;
- текущие поля сотрудника;
- tablet list/detail.

#### Дополнительно

- если прямого статуса сотрудника нет, не изобретать его в доменной модели;
- статусные цветовые акценты использовать только там, где есть реальные данные.

### 4.2 `DeviceListScreen` по мотивам карточек из `WorkersPage` и `WorkerDetailPage`

#### Что переносим

- плотные карточки;
- статусные точки;
- батарейные индикаторы;
- компактные secondary labels;
- detail-pane в стиле device profile card.

#### Что сохраняем

- device status filters;
- sync action;
- issued/available logic;
- tablet detail pane.

### Результат этапа

Списочные экраны начинают выглядеть консистентно с TS-референсом и друг с другом.

## Этап 5. Редизайн detail/операционных flow экранов

### 5.1 `IssueScreen`

#### Цель

Не менять логику шагов, но визуально сделать flow ближе к современному card-driven интерфейсу TS.

#### Задачи

- Переделать step indicator из utilitarian в более премиальный compact progress pattern.
- Пересобрать employee/device selection cards.
- В confirm screen добавить визуально сильный summary block.
- Выделить primary CTA и secondary actions по TS-стилистике.

### 5.2 `ReturnScreen`

#### Цель

Сделать возврат визуально более "операторским", с понятным статусом синхронизации и данных.

#### Задачи

- Перенести паттерн статусных карточек/бейджей.
- Упростить визуальное чтение binding card.
- Усилить предупреждение про невыгруженные данные.
- Сделать detail-pane информативнее и ближе к TS detail sections.

### 5.3 `UploadScreen`

#### Цель

Сохранить весь BLE flow, но визуально привести его к языку TS: статус, прогресс, success/error states.

#### Задачи

- Переработать idle/progress/error/done cards;
- унифицировать иконографику и цвета состояний;
- сделать step labels более визуально чистыми;
- усилить success state и packet summary.

### Результат этапа

Самые важные рабочие сценарии будут уже в целевом дизайне без изменения бизнес-поведения.

## Этап 6. Редизайн аналитических и служебных экранов

### 6.1 `JournalScreen` по мотивам `AlertsPage`

#### Что переносим

- карточный список событий;
- фильтры-чипы;
- визуальную семантику warning/error/pending;
- более выраженный action/status language.

#### Что сохраняем

- типы операций;
- фильтрацию;
- pull-to-refresh;
- tablet detail pane.

### 6.2 `SummaryScreen` по мотивам `MonitoringPage`

#### Что переносим

- metric cards;
- крупные цифры в стиле `Commissioner`;
- плотную дашбордную сетку;
- цветовую семантику.

#### Что сохраняем

- все текущие shift metrics;
- refresh pattern;
- offline/sync смыслы.

### 6.3 `SettingsScreen`

#### Что переносим

- компактные list cards;
- вторичные подзаголовки;
- более лёгкую визуальную структуру.

#### Что сохраняем

- logout;
- clear cache;
- change context;
- app version.

### 6.4 `LoginScreen` и `ContextSelectionScreen`

#### Цель

Эти экраны не имеют прямых TS-аналогов, но должны быть редизайнены в той же системе.

#### Подход

- использовать те же цвета, типографику, карточки и плотность;
- убрать "чисто Material" ощущение;
- привести их к языку продукта, который теперь задаёт TS-референс.

## Этап 7. Решение по "несовпадающим" TS-экранам

### Экраны без прямого Kotlin-аналога

- `MapsPage`
- полноценный `AlertsPage`
- полноценный `WorkerDetailPage` как отдельный route-level экран

### Решение

Их не включать в обязательный scope редизайна текущей задачи.

### Отдельно вынести в backlog

1. Нужно ли добавлять в Kotlin новый экран мониторинга?
2. Нужен ли отдельный экран алертов?
3. Нужен ли экран карты площадки?
4. Нужна ли отдельная карточка сотрудника вне list/detail?

Если ответ "да", это уже не редизайн, а расширение продукта.

## 9. Технический план реализации

## Шаг 1. Foundation

- обновить `Color.kt`, `Theme.kt`, `Type.kt`;
- добавить новые font resources;
- создать общий набор app-ui компонентов;
- обновить базовые scaffold helpers.

## Шаг 2. Shell

- переделать общий screen scaffold;
- переделать top bar patterns;
- переделать bottom navigation на `HomeScreen`.

## Шаг 3. High priority screens

- `HomeScreen`
- `EmployeeSearchScreen`
- `DeviceListScreen`
- `SummaryScreen`

## Шаг 4. Core workflows

- `IssueScreen`
- `ReturnScreen`
- `UploadScreen`

## Шаг 5. Secondary screens

- `JournalScreen`
- `SettingsScreen`
- `LoginScreen`
- `ContextSelectionScreen`

## Шаг 6. Polish

- анимации;
- empty/loading/error states;
- tablet adaptation;
- dark theme check;
- accessibility check.

## 10. Порядок внедрения по приоритету

Рекомендуемый фактический порядок работы:

1. Theme + tokens + fonts.
2. Shared UI components.
3. Home shell + bottom nav.
4. Summary.
5. EmployeeSearch.
6. DeviceList.
7. Issue.
8. Return.
9. Upload.
10. Journal.
11. Settings.
12. Login.
13. ContextSelection.
14. Полировка и регрессия.

## 11. Риски

### Риск 1. Попытка делать "100% как TS" по структуре экранов

Последствие:

- сломается текущий пользовательский flow Kotlin-продукта.

Митигируем:

- переносим 100% визуальное соответствие только там, где совпадает паттерн, а не сама бизнес-сущность.

### Риск 2. Потеря tablet-адаптации

Последствие:

- деградация уже существующего list/detail UX.

Митигируем:

- каждая переделка списочного экрана проверяется отдельно на tablet portrait/landscape.

### Риск 3. Слишком ранний экранный rewrite без общей design-system базы

Последствие:

- получим смесь старого Material3 и нового TS-стиля.

Митигируем:

- сначала foundation, потом screens.

### Риск 4. Визуальный перенос UI, завязанного на отсутствующие данные

Последствие:

- появятся фейковые поля и ложные ожидания у пользователей.

Митигируем:

- не создавать новые доменные поля без отдельного продуктового решения.

## 12. Критерии готовности каждого экрана

Экран считается завершённым, если:

- визуально соответствует новой дизайн-системе;
- не ломает существующий ViewModel/state flow;
- не ломает back-navigation;
- не ломает phone/tablet layout;
- все состояния `loading / empty / error / content` сохранены;
- нет hardcoded цветов вне design tokens;
- нет новых hardcoded строк;
- экран проходит ручной smoke-test.

## 13. Общий Definition of Done для всей задачи

- Все существующие Kotlin-экраны приведены к единому визуальному языку TS-референса.
- Совпадающие и частично совпадающие паттерны перенесены максимально близко к TS.
- Несовпадающие TS-экраны не встроены искусственно и не ломают текущий продукт.
- Не нарушены:
  - issue flow;
  - return flow;
  - upload flow;
  - journal;
  - summary;
  - settings;
  - login/context selection;
  - tablet list/detail.
- Приложение успешно собирается.
- Выполнена ручная проверка phone/tablet.

## 14. Практическое заключение перед стартом реализации

Работу нужно вести как "редизайн действующего Android-продукта по TS-референсу", а не как "порт web-прототипа в Android".

Самые сильные кандидаты на почти прямой перенос визуального языка:

- общий shell;
- bottom navigation;
- dashboard-подача;
- metric cards;
- search/filter/list cards;
- detail sections;
- status badges;
- warning/error blocks.

Самые слабые кандидаты на прямой перенос:

- карта;
- экран алертов как отдельный продуктовый раздел;
- worker telemetry detail как самостоятельная сущность.

Итоговый курс работ:

1. Сначала foundation и shell.
2. Потом редизайн совпадающих экранов и паттернов.
3. Потом адаптация операционных flow.
4. Потом полировка и регрессия.
