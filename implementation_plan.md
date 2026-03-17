# Автоматическая регистрация часов через QR-код

## Описание задачи

Сейчас часы регистрируются через захардкоженный [registration_code](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/watch-backend/src/services/auth_device_service.py#84-93). Нужно автоматизировать процесс:

1. **Часы запускаются** → показывают QR-код со своим `device_id`
2. **Мобилка сканирует QR** → начинается регистрация часов на бэкенде
3. **Оператор выбирает сотрудника** → создаётся привязка (binding) часов к сотруднику

### Текущее состояние

| Компонент | Что есть | Что нужно |
|-----------|---------|-----------|
| Watch | Хардкод `registrationCode = "A5A15D74E0D11F33"`, кнопка «Зарегистрировать» | QR-код с `device_id`, поллинг статуса регистрации |
| Backend | `POST /auth/device/register` (требует [registration_code](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/watch-backend/src/services/auth_device_service.py#84-93)), `POST /bindings/` | Новый endpoint для регистрации через мобилку  |
| Mobile | [QrScanScreen](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/mobile_tracker/app/src/main/java/com/example/mobile_tracker/presentation/qr_scan/QrScanScreen.kt#63-238) (IssueDevice/Return/Upload), [IssueScreen](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/mobile_tracker/app/src/main/java/com/example/mobile_tracker/presentation/binding/issue/IssueScreen.kt#78-174) (3-step wizard) | Новый mode `RegisterWatch` → сканирование → регистрация + binding |

---

## User Review Required

> [!IMPORTANT]
> **Новый бэкенд endpoint:** Предлагается добавить `POST /api/v1/auth/device/register-via-mobile` — мобилка вызывает его с `device_id` часов (из QR-кода) и `employee_id` (выбранный оператором). Этот endpoint создаёт устройство, регистрационный код, и привязку за один вызов. Это упрощает flow и не требует от часов знания registration_code — мобилка всё берёт на себя.

> [!IMPORTANT]  
> **Поллинг vs BLE:** Часы после показа QR будут поллить бэкенд (`GET /api/v1/auth/device/{device_id}/status`) чтобы узнать, что регистрация завершена. Альтернатива — BLE-уведомление от мобилки, но поллинг проще и не зависит от BLE-стека. Подтвердите подход.

> [!WARNING]
> **Безопасность:** Новый endpoint `register-via-mobile` требует авторизацию оператора (Bearer token мобилки), но НЕ требует [registration_code](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/watch-backend/src/services/auth_device_service.py#84-93) от часов. Часы получат [device_secret](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/watch-backend/src/services/auth_device_service.py#42-45) через ответ поллинга. Это безопасно, потому что только авторизованный оператор может инициировать регистрацию.

---

## Proposed Changes

### Backend (watch-backend)

Добавить два новых endpoint'а и обновить модель Device.

---

#### [NEW] [register_via_mobile.py](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/watch-backend/src/api/v1/register_via_mobile.py)

Новый router с двумя endpoint'ами:

**`POST /api/v1/auth/device/register-via-mobile`** — вызывается мобилкой:
- Вход: `{ device_id, employee_id, site_id, model?, firmware? }`
- Требует Bearer token оператора
- Создаёт [Device](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/mobile_tracker/app/src/main/java/com/example/mobile_tracker/data/repository/BindingRepository.kt#52-174) (status=`ACTIVE`), генерирует [device_secret](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/watch-backend/src/services/auth_device_service.py#42-45)
- Создаёт `DeviceBinding` (device → employee)
- Сохраняет `device_secret_hash` в Device
- Возвращает: `{ device_id, status: "registered" }`

**`GET /api/v1/auth/device/{device_id}/registration-status`** — поллится часами:
- Вход: `device_id` (path param)
- Если устройство существует и `status == ACTIVE` → возвращает `{ registered: true, device_secret, server_public_key_pem }`
- Если устройство не найдено → `{ registered: false }`
- [device_secret](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/watch-backend/src/services/auth_device_service.py#42-45) возвращается только один раз (после первого запроса — обнуляется из plain-text или помечается как "выдан")

---

#### [NEW] [register_via_mobile_service.py](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/watch-backend/src/services/register_via_mobile_service.py)

Сервис с логикой:
- `register_device_via_mobile(device_id, employee_id, site_id, model?, firmware?)` → создаёт Device + Binding + Audit log
- `get_registration_status(device_id)` → возвращает статус + credentials (одноразово)

---

#### [NEW] [register_via_mobile.py](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/watch-backend/src/schemas/register_via_mobile.py)

Pydantic-схемы:
- `MobileRegisterRequest(device_id, employee_id, site_id, model?, firmware?, app_version?)`
- `MobileRegisterResponse(device_id, status, binding_id)`
- `RegistrationStatusResponse(registered, device_secret?, server_public_key_pem?, server_time?)`

---

#### [MODIFY] [main.py](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/watch-backend/src/main.py)

Подключить новый router `register_via_mobile.router`.

---

### Watch App (app/)

Заменить экран ручной регистрации на QR-код и поллинг.

---

#### [MODIFY] [RegistrationScreen.kt](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/app/src/main/java/com/example/activity_tracker/presentation/ui/RegistrationScreen.kt)

Полная переработка → **`QrRegistrationScreen`**:
- Генерирует уникальный `device_id` (или берёт Android ID)
- Формирует JSON: `{ "device_id": "...", "model": "...", "firmware": "..." }`
- Кодирует в QR-код и отображает на экране часов
- Показывает текст: «Отсканируйте QR-код мобильным приложением»
- Поллит `GET /auth/device/{device_id}/registration-status` каждые 3 сек
- Когда `registered == true` → сохраняет credentials → переходит к StatusScreen

---

#### [MODIFY] [StatusViewModel.kt](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/app/src/main/java/com/example/activity_tracker/presentation/viewmodel/StatusViewModel.kt)

- Удалить `registrationCode = "A5A15D74E0D11F33"`
- Добавить `qrPayload: StateFlow<String>` — JSON для QR-кода
- Добавить `startRegistrationPolling()` — запускает поллинг
- Добавить `stopRegistrationPolling()` — останавливает при переходе к StatusScreen
- Метод [registerDevice()](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/app/src/main/java/com/example/activity_tracker/presentation/viewmodel/StatusViewModel.kt#81-117) заменить на поллинг-логику

---

#### [MODIFY] [build.gradle.kts](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/app/build.gradle.kts)

Добавить зависимость для генерации QR-кодов:
```kotlin
implementation("com.google.zxing:core:3.5.3")
```

---

#### [MODIFY] [AuthManager.kt](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/app/src/main/java/com/example/activity_tracker/network/AuthManager.kt)

- Добавить метод `pollRegistrationStatus(deviceId)` — вызывает endpoint поллинга
- При получении `registered = true` — сохраняет [device_secret](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/watch-backend/src/services/auth_device_service.py#42-45) и `server_public_key_pem` через `credentialsStore`
- Затем вызывает [authenticate()](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/app/src/main/java/com/example/activity_tracker/network/AuthManager.kt#105-153) для получения токенов

---

#### [MODIFY] [WatchAuthService.kt](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/app/src/main/java/com/example/activity_tracker/network/WatchAuthService.kt)

Добавить:
```kotlin
@GET("auth/device/{device_id}/registration-status")
suspend fun getRegistrationStatus(
    @Path("device_id") deviceId: String
): Response<RegistrationStatusResponse>
```

---

### Mobile App (mobile_tracker/)

Добавить новый режим QR-сканирования и flow регистрации.

---

#### [MODIFY] [Route.kt](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/mobile_tracker/app/src/main/java/com/example/mobile_tracker/presentation/navigation/Route.kt)

Добавить `QrScanMode.RegisterWatch` в enum.

---

#### [MODIFY] [QrScanScreen.kt](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/mobile_tracker/app/src/main/java/com/example/mobile_tracker/presentation/qr_scan/QrScanScreen.kt)

Добавить обработку `QrScanMode.RegisterWatch`:
- subtitle/instruction/demoValue для нового режима

---

#### [NEW] [RegisterWatchScreen.kt](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/mobile_tracker/app/src/main/java/com/example/mobile_tracker/presentation/register_watch/RegisterWatchScreen.kt)

Новый экран для регистрации часов (после сканирования QR):
- Показывает отсканированный `device_id`
- Позволяет выбрать сотрудника (переиспользует `EmployeeSearchScreen` или inline-поиск)
- Кнопка «Зарегистрировать и привязать»
- Вызывает `POST /auth/device/register-via-mobile` с `device_id` + `employee_id`
- Показывает результат: ✅ Часы зарегистрированы и привязаны к [имя сотрудника]

---

#### [NEW] [RegisterWatchViewModel.kt](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/mobile_tracker/app/src/main/java/com/example/mobile_tracker/presentation/register_watch/RegisterWatchViewModel.kt)

ViewModel:
- `deviceId: String` (из QR)
- `employees: List<Employee>` — поиск сотрудников
- `selectedEmployee: Employee?`
- `registerAndBind()` — вызывает API

---

#### [NEW] [DeviceRegistrationApi.kt](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/mobile_tracker/app/src/main/java/com/example/mobile_tracker/data/remote/api/DeviceRegistrationApi.kt)

Ktor API client:
```kotlin
class DeviceRegistrationApi(private val client: HttpClient) {
    suspend fun registerWatchViaMobile(request: MobileRegisterRequest): HttpResponse =
        client.post("/api/v1/auth/device/register-via-mobile") { setBody(request) }
}
```

---

#### [NEW] [DeviceRegistrationDto.kt](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/mobile_tracker/app/src/main/java/com/example/mobile_tracker/data/remote/dto/DeviceRegistrationDto.kt)

DTO-классы:
```kotlin
@Serializable
data class MobileRegisterRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("employee_id") val employeeId: String,
    @SerialName("site_id") val siteId: String,
)

@Serializable
data class MobileRegisterResponse(
    @SerialName("device_id") val deviceId: String,
    val status: String,
    @SerialName("binding_id") val bindingId: Long? = null,
)
```

---

#### [MODIFY] [AppNavGraph.kt](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/mobile_tracker/app/src/main/java/com/example/mobile_tracker/presentation/navigation/AppNavGraph.kt)

Добавить:
- Route для `RegisterWatchScreen`
- Navigation при `QrScanMode.RegisterWatch` из [QrScanScreen](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/mobile_tracker/app/src/main/java/com/example/mobile_tracker/presentation/qr_scan/QrScanScreen.kt#63-238) → `RegisterWatchScreen`

---

#### [MODIFY] [AppModule.kt](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/mobile_tracker/app/src/main/java/com/example/mobile_tracker/di/AppModule.kt)

Зарегистрировать `DeviceRegistrationApi` и `RegisterWatchViewModel` в Koin.

---

#### [MODIFY] [HomeScreen.kt](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/mobile_tracker/app/src/main/java/com/example/mobile_tracker/presentation/home/HomeScreen.kt)

Добавить кнопку/карточку «Регистрация часов» → открывает QR-сканирование в режиме `RegisterWatch`.

---

## Verification Plan

### Manual Verification (основной путь)

Поскольку проект состоит из 3 Android-приложений + Python-бэкенд, полный тест требует реальные устройства. Предлагаю:

1. **Backend unit test** — проверить новый сервис `RegisterViaMobileService` изолированно
2. **Backend API test** — запустить сервер, вызвать новые endpoint'ы через `curl`/`httpie`
3. **Ручной E2E тест** — часы на эмуляторе + мобилка на эмуляторе/устройстве + бэкенд
4. **Compile check** — убедиться, что оба Android-проекта компилируются

### Automated Tests

#### Backend (pytest)

Файл: `tests/test_register_via_mobile.py`

```bash
cd watch-backend
uv run pytest tests/test_register_via_mobile.py -v
```

Тесты:
- Успешная регистрация через мобилку → проверка что Device создан + Binding создан
- Повторная регистрация того же `device_id` → ошибка 409
- Поллинг status → до регистрации `registered: false`, после регистрации `registered: true`
- [device_secret](file:///c:/Users/safae/AndroidStudioProjects/activity_tracker/watch-backend/src/services/auth_device_service.py#42-45) выдаётся только один раз

### Manual E2E Test Steps

> [!NOTE]
> Нужна ваша помощь с ручным тестированием — подскажите, какие эмуляторы/устройства доступны и как запускать бэкенд локально.

1. Запустить бэкенд (`make run` или `uv run uvicorn src.main:app`)
2. Запустить watch app на Wear OS эмуляторе
3. Убедиться, что на экране часов отображается QR-код
4. Запустить mobile app на Android эмуляторе/устройстве
5. На Home screen нажать «Регистрация часов»
6. Ввести device_id вручную (или отсканировать QR с экрана эмулятора)
7. Выбрать сотрудника из списка
8. Нажать «Зарегистрировать»
9. Проверить, что часы перешли на StatusScreen (перестали показывать QR)
10. Проверить в бэкенде, что Device и Binding созданы
