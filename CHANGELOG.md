# Changelog

## [0.6.0] — M6: Настройки, безопасность, полировка

### Добавлено
- Экран настроек (SettingsScreen): информация об операторе, смена контекста, очистка кэша, выход
- Certificate Pinning (OkHttp CertificatePinner)
- HTTP 403 → принудительный logout (HttpResponseValidator)
- ProGuard/R8 правила для Kotlin Serialization, Ktor, OkHttp, Room, Koin
- Адаптивный layout для планшетов >600dp (AdaptiveLayout)
- LeakCanary для отладки утечек памяти
- GitHub Actions CI/CD (lint, тесты, APK)
- README.md с описанием проекта
- Unit-тесты SettingsViewModel (7 тестов)

### Изменено
- DAO: добавлены методы deleteAll() для Employee, Device, Site, DowntimeReason
- ReferenceRepository: добавлен clearAll() для очистки кэша
- HomeScreen: кнопка «Настройки» в табе «Ещё»
- NetworkClient: Certificate Pinning + обработка 403

---

## [0.5.0] — M5: Журнал, сводка, оффлайн-индикация

### Добавлено
- Журнал операций (JournalScreen): фильтры, поиск, pull-to-refresh
- Сводка по смене (SummaryScreen): карточки метрик
- Оффлайн-индикация: баннер + счётчик пакетов в очереди
- NetworkMonitor для наблюдения за состоянием сети
- Unit-тесты JournalViewModel, SummaryViewModel

### Изменено
- HomeScreen: табы Journal, More с навигацией
- AppNavGraph: маршруты Journal, Summary

---

## [0.4.0] — M4: Bluetooth и выгрузка данных

### Добавлено
- BLE Scanner, GATT Client, BLE Protocol
- UploadScreen с прогрессом считывания и отправки
- SyncPacketsWorker (WorkManager, экспоненциальный backoff)
- SyncBindingsWorker для синхронизации привязок
- GatewayApi: POST /api/v1/gateway/packets

---

## [0.3.0] — M3: Выдача и возврат часов

### Добавлено
- IssueScreen: идентификация сотрудника → назначение часов → подтверждение
- ReturnScreen: список выданных → возврат с опцией выгрузки
- BindingRepository: локальные привязки + синхронизация
- Валидации конфликтов привязок
- Журнал операций (запись выдачи/возврата)

---

## [0.2.0] — M2: Справочники и кэширование

### Добавлено
- API загрузка сотрудников, устройств, площадок, причин простоя
- Кэширование в Room (upsert)
- SyncReferenceDataWorker (WorkManager)
- DeviceListScreen: фильтрация по статусу
- EmployeeSearchScreen: поиск по ФИО и табельному номеру

---

## [0.1.0] — M1: Инфраструктура и авторизация

### Добавлено
- Kotlin + Compose + Gradle + Version Catalog
- DI-модули (Koin): appModule, networkModule, databaseModule
- Room БД: все Entity + DAO + миграции
- DataStore + EncryptedSharedPreferences
- HTTP-клиент (Ktor): AuthInterceptor, Token Refresh
- LoginScreen → ContextSelectionScreen → HomeScreen
- Compose Navigation (type-safe routes)
