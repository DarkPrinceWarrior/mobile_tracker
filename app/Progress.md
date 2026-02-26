# Mobile Tracker — Прогресс разработки

---

## Итерация M1 — Инфраструктура и авторизация ✅

**Статус:** Завершена  
**Критерий:** Оператор входит, выбирает площадку и смену.

### Задачи

- [x] Обновить `libs.versions.toml` — Version Catalog (Room, Koin, Ktor, WorkManager, DataStore, Security, Navigation, Serialization, Timber, Coil)
- [x] Обновить `build.gradle.kts` (root + app) — плагины KSP, serialization; все зависимости; minSdk=29, compileSdk/targetSdk=36
- [x] Обновить `AndroidManifest.xml` — разрешения BLE, NFC, Network, Foreground Service, Notifications; Application class, network security config
- [x] Создать `network_security_config.xml`
- [x] Создать доменные модели (`domain/model/`) — User, Employee, Device, DeviceBinding, Site, ShiftPacket, OperationLog, ShiftContext
- [x] Создать Room Entity (`data/local/db/entity/`) — EmployeeEntity, DeviceEntity, BindingEntity, PacketQueueEntity, OperationLogEntity, SiteEntity, ShiftContextEntity, DowntimeReasonEntity
- [x] Создать DAO (`data/local/db/dao/`) — EmployeeDao, DeviceDao, BindingDao, PacketQueueDao, OperationLogDao, SiteDao, ShiftContextDao
- [x] Создать `Converters.kt` — TypeConverter для Room (списки строк)
- [x] Создать `AppDatabase.kt` — объединение всех Entity и DAO
- [x] Создать `UserPreferencesManager` (DataStore) — хранение данных сессии
- [x] Создать `SecureStorage` (EncryptedSharedPreferences) — безопасное хранение токенов
- [x] Создать DTO авторизации (`data/remote/dto/AuthDto.kt`) — LoginRequest, LoginResponse, UserDto, RefreshTokenRequest, RefreshTokenResponse
- [x] Создать `NetworkConfig.kt` — константы сетевых настроек
- [x] Создать `NetworkClient.kt` — Ktor HTTP клиент с интерцепторами, таймаутами, логированием, обновлением токена
- [x] Создать `AuthApi.kt` — API авторизации (login, getMe, refresh)
- [x] Создать Koin DI-модули (`di/`) — appModule, networkModule, databaseModule
- [x] Создать `App.kt` — Application class с инициализацией Koin и Timber
- [x] Создать навигацию (`presentation/navigation/`) — Route (sealed interface), AppNavGraph
- [x] Создать `LoginScreen` + `LoginViewModel` + `LoginContract` — MVI-паттерн, авторизация
- [x] Создать `ContextSelectionScreen` + `ContextSelectionViewModel` + `ContextSelectionContract` — выбор площадки, даты, смены
- [x] Создать `HomeScreen` + `HomeViewModel` — главный экран с Bottom Navigation (5 табов)
- [x] Обновить `MainActivity` — интеграция с Compose Navigation
- [x] Обновить `strings.xml` — русская локализация (Login, ContextSelection, Home, общие)
- [x] Создать `NetworkMonitor` — отслеживание состояния сети (Flow<Boolean>)
- [x] Создать `Extensions.kt` — утилиты форматирования дат
- [x] Исправить версию `kotlinx-serialization-json` в toml (1.8.0)

### Файлы (ключевые)

| Файл | Назначение |
|------|-----------|
| `gradle/libs.versions.toml` | Version Catalog |
| `app/build.gradle.kts` | Зависимости и плагины модуля |
| `data/local/db/AppDatabase.kt` | Room база данных |
| `data/local/datastore/UserPreferencesManager.kt` | DataStore preferences |
| `data/local/secure/SecureStorage.kt` | EncryptedSharedPreferences |
| `data/remote/NetworkClient.kt` | Ktor HTTP клиент |
| `data/remote/api/AuthApi.kt` | API авторизации |
| `di/AppModule.kt`, `NetworkModule.kt`, `DatabaseModule.kt` | Koin DI |
| `presentation/login/LoginScreen.kt` | Экран авторизации |
| `presentation/context_selection/ContextSelectionScreen.kt` | Выбор контекста |
| `presentation/home/HomeScreen.kt` | Главный экран |
| `presentation/navigation/Route.kt`, `AppNavGraph.kt` | Навигация |

---

## Итерация M2 — Справочники и кэширование ✅

**Статус:** Завершена  
**Критерий:** Справочники загружаются, работают оффлайн, обновляются автоматически.

### Задачи

- [x] Создать DTO справочников (`ReferenceDto.kt`) — EmployeeDto, DeviceDto, SiteDto, DowntimeReasonDto, PaginatedResponse<T>
- [x] Создать мапперы (`Mappers.kt`) — DTO → Entity → Domain (Employee, Device, Site, DowntimeReason)
- [x] Создать `ReferenceApi.kt` — API загрузки справочников с пагинацией (employees, devices, sites, downtime-reasons)
- [x] Создать `ReferenceRepository.kt` — логика кэширования: upsert в Room, удаление устаревших записей (7 дней), полная синхронизация
- [x] Создать `SyncReferenceDataWorker.kt` — WorkManager: периодическая (каждые 4ч) и разовая синхронизация с retry-логикой
- [x] Создать `DowntimeReasonDao.kt` + обновить `AppDatabase` — DAO для причин простоя
- [x] Создать доменную модель `DowntimeReason`
- [x] Создать `DeviceListScreen` + `DeviceListViewModel` + `DeviceListContract` — экран часов на площадке с фильтрацией (все/свободны/выданы), поиском, синхронизацией
- [x] Создать `EmployeeSearchScreen` + `EmployeeSearchViewModel` + `EmployeeSearchContract` — поиск сотрудников по ФИО и табельному номеру с debounce (300мс)
- [x] Обновить DI-модули — ReferenceApi, ReferenceRepository, DeviceListViewModel, EmployeeSearchViewModel
- [x] Обновить навигацию — Route.DeviceList, Route.EmployeeSearch, composable-маршруты
- [x] Обновить `HomeScreen` — таб "Ещё" с кнопками навигации к DeviceList и EmployeeSearch
- [x] Обновить `ContextSelectionViewModel` — фоновая синхронизация справочников при старте работы
- [x] Обновить `App.kt` — запуск периодической синхронизации WorkManager
- [x] Обновить `strings.xml` — строки для устройств, сотрудников, синхронизации, раздела "Ещё"

### Файлы (ключевые)

| Файл | Назначение |
|------|-----------|
| `data/remote/dto/ReferenceDto.kt` | DTO справочников |
| `data/remote/dto/Mappers.kt` | Маппинг DTO ↔ Entity ↔ Domain |
| `data/remote/api/ReferenceApi.kt` | API справочников |
| `data/repository/ReferenceRepository.kt` | Кэширование и синхронизация |
| `data/worker/SyncReferenceDataWorker.kt` | WorkManager фоновая синхронизация |
| `data/local/db/dao/DowntimeReasonDao.kt` | DAO причин простоя |
| `domain/model/DowntimeReason.kt` | Доменная модель |
| `presentation/devices/DeviceListScreen.kt` | Экран часов на площадке |
| `presentation/devices/DeviceListViewModel.kt` | ViewModel устройств |
| `presentation/employees/EmployeeSearchScreen.kt` | Экран поиска сотрудников |
| `presentation/employees/EmployeeSearchViewModel.kt` | ViewModel поиска |

---

## Итерация M3 — Выдача и возврат часов ✅

**Статус:** Завершена  
**Критерий:** Оператор выдаёт и принимает часы, привязки синхронизируются с сервером.

### План задач

#### 3.1. Data Layer — DTO и API
- [x] Создать `BindingDto.kt` — DTO для привязок (CreateBindingRequest, BindingResponse, ReturnBindingRequest)
- [x] Создать `BindingApi.kt` — API привязок: POST create, PUT close, GET by site
- [x] Создать `BindingRepository.kt` — логика создания/возврата привязок, оффлайн-очередь, синхронизация

#### 3.2. Data Layer — WorkManager
- [x] Создать `SyncBindingsWorker.kt` — фоновая синхронизация привязок (отправка оффлайн-привязок на сервер)

#### 3.3. Domain Layer
- [x] Обновить `DeviceBinding` — поля уже есть (isSynced)
- [x] Создать Repository-методы: `issueDevice()`, `returnDevice()`, `observeActiveBindings()`
- [x] Добавить `BindingEntity.toDomain()` маппер в `Mappers.kt`

#### 3.4. Presentation — Экран выдачи (IssueScreen)
- [x] Создать `IssueContract.kt` — State/Intent/Effect для выдачи
- [x] Создать `IssueViewModel.kt` — логика: поиск сотрудника → выбор часов → валидация → подтверждение
- [x] Создать `IssueScreen.kt` — UI: шаги выдачи (идентификация сотрудника → назначение устройства → подтверждение)
- [x] Реализовать идентификацию сотрудника: по табельному номеру, по ФИО
- [x] Реализовать выбор устройства: список свободных часов на площадке, выбор из списка
- [x] Реализовать подтверждение: summary-карточка, кнопка "Выдать"
- [ ] Реализовать идентификацию по RFID-пропуску (требует BLE интеграции)

#### 3.5. Presentation — Экран возврата (ReturnScreen)
- [x] Создать `ReturnContract.kt` — State/Intent/Effect для возврата
- [x] Создать `ReturnViewModel.kt` — логика: список выданных → выбор → возврат + "Потеряны"
- [x] Создать `ReturnScreen.kt` — UI: список выданных часов, кнопка возврата, предупреждение о невыгруженных данных

#### 3.6. Валидации
- [x] Проверка: часы свободны (localStatus == "available") перед выдачей
- [x] Проверка: сотрудник не имеет активной привязки (один сотрудник — одни часы)
- [x] Проверка: часы активны (status == "active"), не неисправны / не разряжены
- [x] Отображение ошибок валидации в UI

#### 3.7. Журнал операций
- [x] Запись в журнал при каждой выдаче/возврате через `OperationLogDao` (тип, время, сотрудник, устройство)

#### 3.8. Оффлайн-привязки
- [x] Сохранение привязки локально (BindingEntity + DeviceEntity.localStatus) при отсутствии сети
- [x] Флаг `isSynced` в BindingEntity для неотправленных привязок
- [x] Автоматическая синхронизация (SyncBindingsWorker)
- [x] Регистрация SyncBindingsWorker в WorkManager при старте приложения
- [x] Индикация неотправленных привязок в UI (SyncStatusIcon на ReturnScreen)

#### 3.9. Навигация и DI
- [x] Добавить Route.Issue, Route.Return в навигацию
- [x] Обновить AppNavGraph — composable для IssueScreen, ReturnScreen
- [x] Обновить HomeScreen — табы "Выдача" и "Возврат" ведут на соответствующие экраны
- [x] Обновить DI: BindingApi, BindingRepository, IssueViewModel, ReturnViewModel

#### 3.10. Строковые ресурсы
- [x] Обновить `strings.xml` — строки для экранов выдачи и возврата

#### 3.11. Тесты
- [x] Unit-тесты: BindingRepository (создание, возврат, валидация, оффлайн)
- [x] Unit-тесты: IssueViewModel, ReturnViewModel

---

## Предстоящие итерации

### M4 — Bluetooth и выгрузка данных (2-3 недели)
BLE Scanner, GATT Client, протокол обмена с часами, считывание пакетов, экран выгрузки.

### M5 — Отправка на сервер и оффлайн (1-2 недели)
POST packets API, идемпотентность, SyncPacketsWorker, экспоненциальный backoff, индикация оффлайн.

### M6 — Журнал, полировка, тестирование (1-2 недели)
JournalScreen с фильтрами, тёмная тема, полевые тесты, performance, безопасность, CI/CD.
