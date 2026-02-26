# Mobile Tracker — Контроль подрядчиков

Android-приложение для операторов площадок: выдача/возврат Samsung Galaxy Watch 8, считывание зашифрованных пакетов по BLE, отправка на сервер через GATEWAY API.

## Стек технологий

| Компонент | Технология |
|-----------|-----------|
| Язык | Kotlin 2.1 |
| UI | Jetpack Compose + Material 3 |
| Архитектура | Clean Architecture + MVI |
| DI | Koin 4.0 |
| HTTP | Ktor Client + OkHttp |
| БД | Room 2.7 |
| Навигация | Compose Navigation (type-safe) |
| Фоновые задачи | WorkManager |
| BLE | Android BLE GATT Client |
| Безопасность | EncryptedSharedPreferences, Certificate Pinning |
| Тесты | JUnit 4, MockK, Turbine, Coroutines Test |

## Архитектура

```
data/
  ├── local/         # Room DB, DataStore, SecureStorage
  ├── remote/        # Ktor API, DTO
  ├── repository/    # Репозитории
  ├── ble/           # BLE Scanner, GATT Client, Protocol
  └── worker/        # WorkManager workers
presentation/
  ├── login/         # Авторизация
  ├── context_selection/ # Выбор площадки и смены
  ├── home/          # Главный экран с табами
  ├── binding/       # Выдача / Возврат
  ├── upload/        # Выгрузка данных с часов
  ├── devices/       # Список часов
  ├── employees/     # Поиск сотрудников
  ├── journal/       # Журнал операций
  ├── summary/       # Сводка по смене
  ├── settings/      # Настройки
  ├── navigation/    # Routes + NavGraph
  └── common/        # Общие компоненты (AdaptiveLayout)
di/                  # Koin модули
util/                # NetworkMonitor, Extensions
```

## Сборка

```bash
# Debug APK
./gradlew assembleDebug

# Unit-тесты
./gradlew testDebugUnitTest

# Lint
./gradlew lintDebug
```

## Итерации

| Итерация | Содержание | Статус |
|----------|-----------|--------|
| M1 | Инфраструктура, авторизация | ✅ |
| M2 | Справочники, кэширование | ✅ |
| M3 | Выдача и возврат часов | ✅ |
| M4 | Bluetooth и выгрузка данных | ✅ |
| M5 | Журнал, сводка, оффлайн-индикация | ✅ |
| M6 | Настройки, безопасность, полировка | ✅ |

## Требования

- Android 10+ (API 29+)
- Bluetooth 5.0+ (BLE)
- RAM ≥ 2 ГБ
- Целевые устройства: Samsung Galaxy Tab A9+, Samsung Galaxy A-серия

## CI/CD

GitHub Actions: `.github/workflows/android-ci.yml`
- Lint → Unit Tests → Build Debug APK
- Артефакты: lint-report, test-report, app-debug.apk

## Безопасность

- HTTPS only (`usesCleartextTraffic="false"`)
- Токены в `EncryptedSharedPreferences` (AES-256)
- Certificate Pinning (OkHttp)
- HTTP 403 → принудительный logout
- ProGuard/R8 минификация в release
