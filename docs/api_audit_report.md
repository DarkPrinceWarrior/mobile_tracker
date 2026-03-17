# 🔍 Аудит: Мобильное приложение vs Backend API

## Обнаруженная проблема: Все экраны работают ТОЛЬКО с локальной БД

### Текущая архитектура (как есть):

```
ViewModels → DAO (локальная Room БД) → данные из ReferenceRepository sync
```

**Ни один ViewModel не вызывает API-классы напрямую!** Все экраны читают данные из:
- `EmployeeDao` — сотрудники
- `DeviceDao` — устройства
- `BindingDao` — привязки
- `OperationLogDao` — журнал операций
- `PacketQueueDao` — очередь пакетов
- `ShiftContextDao` — контекст смены

### Откуда данные попадают в локальную БД:

| Источник | Как | Что |
|---|---|---|
| `SyncReferenceDataWorker` | Периодически (WorkManager) | Employees, Devices, Sites, DowntimeReasons |
| `SyncBindingsWorker` | Периодически | Bindings |
| `SyncPacketsWorker` | Периодически | Packets upload |
| `DemoDataSeeder` | При старте (demo mode) | Моковые Bindings |
| `FakeHttpEngine` | Demo mode | Моковые HTTP-ответы |

### Новые API — НЕ подключены к экранам:

| API | Класс | Используется? | Где нужен |
|---|---|:---:|---|
| **ShiftsApi** | ✅ Создан | ❌ Нет | MonitoringVM, WorkersVM, WorkerDetailVM |
| **ZonesApi** | ✅ Создан | ❌ Нет | MapsVM, WorkerDetailVM |
| **AnomaliesApi** | ✅ Создан | ❌ Нет | AlertsVM |
| **HeartbeatApi** | ✅ Создан | ❌ Нет | (часы шлют сами) |

### Конфликт моковых данных:

**В `FakeHttpEngine`:**
1. ✅ Auth — mock responses есть → нет конфликта
2. ✅ Employees — mock responses есть → нет конфликта
3. ✅ Devices — mock responses есть → нет конфликта
4. ✅ Sites — mock responses есть → нет конфликта
5. ✅ Bindings — mock responses есть → нет конфликта
6. ✅ Gateway — mock responses есть → нет конфликта
7. 🔴 **Shifts** — MOCK НЕТ → в demo-режиме вызов упадёт (вернёт `{"status":"ok"}`)
8. 🔴 **Zones** — MOCK НЕТ → аналогично
9. 🔴 **Anomalies** — MOCK НЕТ → аналогично
10. 🔴 **Heartbeat** — MOCK НЕТ → аналогично

### Вывод:

Пока нет конфликта — потому что **новые API никем не вызываются**.

Но если начнём подключать их к ViewModels, то в demo-режиме нужны mock-ответы в FakeHttpEngine.

## Рекомендуемые следующие шаги:

1. Добавить mock-ответы для Shifts/Zones/Anomalies в FakeHttpEngine (для demo-режима)
2. Подключить ShiftsApi к MonitoringViewModel/WorkersViewModel для реальных данных о сменах
3. Подключить AnomaliesApi к AlertsViewModel для реальных алертов с бэкенда
4. Подключить ZonesApi к MapsViewModel
