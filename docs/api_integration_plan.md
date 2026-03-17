# План интеграции Backend API с мобильным приложением

## Анализ: что уже реализовано ✅ vs что нужно подключить 🔲

### Текущее состояние API-слоя

| # | Функционал Backend | Уже реализовано? | Комментарий |
|---|-------------------|:---:|---|
| 1 | **Auth — Login** (`POST /auth/login`) | ✅ | `AuthApi.login()` |
| 2 | **Auth — Me** (`GET /auth/me`) | ✅ | `AuthApi.getMe()` |
| 3 | **Device Registration via Mobile** (`POST /auth/device/register-via-mobile`) | ✅ | [DeviceRegistrationApi](file:///c:/Users/safae/AndroidStudioProjects/mobile_tracker/app/src/main/java/com/example/mobile_tracker/data/remote/api/DeviceRegistrationApi.kt#9-18) |
| 4 | **Device Registration Status** (`GET /auth/device/{id}/registration-status`) | 🔲 | Нет API-класса — нужно для Watch QR flow |
| 5 | **Devices — List** (`GET /devices`) | ✅ | `ReferenceApi.getDevices()` |
| 6 | **Devices — Detail** (`GET /devices/{id}`) | 🔲 | Нет — нужно для WorkerDetail экрана |
| 7 | **Devices — Full** (`GET /devices/{id}/full`) | 🔲 | Опционально для MVP |
| 8 | **Devices — Bind** (`POST /devices/{id}/bind`) | 🔲 | Есть [BindingApi](file:///c:/Users/safae/AndroidStudioProjects/mobile_tracker/app/src/main/java/com/example/mobile_tracker/data/remote/api/BindingApi.kt#16-42), но bind через другой endpoint |
| 9 | **Employees — List** (`GET /employees/`) | ✅ | `ReferenceApi.getEmployees()` |
| 10 | **Employees — Get** (`GET /employees/{uuid}`) | 🔲 | Нет — нужно для WorkerDetail |
| 11 | **Shifts — List** (`GET /shifts`) | 🔲 | **Нужно для Monitoring/Workers** |
| 12 | **Shifts — Detail** (`GET /shifts/{id}`) | 🔲 | Нужно для WorkerDetail |
| 13 | **Shifts — Activity** (`GET /shifts/{id}/activity`) | 🔲 | Нужно для WorkerDetail (активность) |
| 14 | **Shifts — Metrics** (`GET /shifts/{id}/metrics`) | 🔲 | Нужно для Monitoring (продуктивность) |
| 15 | **Shifts — Zones** (`GET /shifts/{id}/zones`) | 🔲 | Нужно для WorkerDetail (маршрут на объекте) |
| 16 | **Shifts — Route** (`GET /shifts/{id}/route`) | 🔲 | Нужно для Maps/WorkerDetail |
| 17 | **Shifts — Downtimes** (`GET /shifts/{id}/downtimes`) | 🔲 | Нужно для WorkerDetail |
| 18 | **Shifts — Reaction Times** (`GET /shifts/{id}/reaction-times`) | 🔲 | Опционально для MVP |
| 19 | **Bindings — Create** (`POST /bindings/`) | ✅ | `BindingApi.createBinding()` |
| 20 | **Bindings — Close** (`PUT /bindings/{id}/close`) | ✅ | `BindingApi.closeBinding()` |
| 21 | **Bindings — List** (`GET /bindings/`) | ✅ | `BindingApi.getBindings()` |
| 22 | **Gateway — Upload Packet** (`POST /gateway/packets`) | ✅ | `GatewayApi.uploadPacket()` |
| 23 | **Heartbeat** (`POST /watch/heartbeat`) | 🔲 | **Нужно** — мобилка как gateway |
| 24 | **Sites — List** (`GET /sites/`) | ✅ | `ReferenceApi.getSites()` |
| 25 | **Downtime Reasons** (`GET /downtime-reasons/`) | ✅ | `ReferenceApi.getDowntimeReasons()` |
| 26 | **Zones** (`GET /sites/{id}/zones`) | 🔲 | Нужно для Maps |
| 27 | **Analytics — Productivity** (`GET /analytics/productivity`) | 🔲 | Нужно для Monitoring |
| 28 | **Analytics — Employee Summary** (`GET /analytics/employee/{id}/summary`) | 🔲 | Нужно для WorkerDetail |
| 29 | **Analytics — Device Summary** (`GET /analytics/device/{id}/summary`) | 🔲 | Опционально |
| 30 | **Anomalies — List** (`GET /anomalies`) | 🔲 | Нужно для Alerts |
| 31 | **Anomalies — Shift** (`GET /shifts/{id}/anomalies`) | 🔲 | Нужно для WorkerDetail |
| 32 | **Anomalies — Update** (`PATCH /anomalies/{id}`) | 🔲 | Нужно для Alerts (пометка) |

---

## План реализации по пунктам (MVP scope)

### Пункт 1: Shifts API — Список смен и метрики
> Это ключевой функционал для экранов Monitoring, Workers, WorkerDetail
- `GET /shifts` — список смен
- `GET /shifts/{id}` — детали смены
- `GET /shifts/{id}/metrics` — метрики смены
- `GET /shifts/{id}/activity` — классификация активности

### Пункт 2: Zones API — Зоны объекта
> Для Maps экрана и WorkerDetail (маршрут)
- `GET /sites/{site_id}/zones` — список зон объекта
- `GET /shifts/{id}/zones` — зоны за смену
- `GET /shifts/{id}/route` — маршрут

### Пункт 3: Analytics API — Продуктивность
> Для Monitoring и Summary экранов
- `GET /analytics/productivity` — отчёт по продуктивности
- `GET /analytics/employee/{id}/summary` — сводка по сотруднику

### Пункт 4: Anomalies API — Аномалии
> Для Alerts экрана
- `GET /anomalies` — список аномалий
- `PATCH /anomalies/{id}` — обновить статус

### Пункт 5: Heartbeat API
> Мобилка как gateway
- `POST /watch/heartbeat`

### Пункт 6: Employee Detail & Device Detail
> Для WorkerDetail
- `GET /employees/{uuid}` — детали сотрудника
- `GET /devices/{device_id}` — детали устройства

### Пункт 7: Shifts — Downtimes  
> Для WorkerDetail
- `GET /shifts/{id}/downtimes` — простои

### Пункт 8: Device Registration Status
> Для QR-flow часов
- `GET /auth/device/{device_id}/registration-status`
