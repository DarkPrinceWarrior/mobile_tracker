package com.example.mobile_tracker.presentation.register_watch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.db.dao.EmployeeDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.remote.api.DeviceRegistrationApi
import com.example.mobile_tracker.data.remote.dto.MobileRegisterRequest
import com.example.mobile_tracker.data.remote.dto.MobileRegisterResponse
import com.example.mobile_tracker.data.remote.dto.toDomain
import com.example.mobile_tracker.data.repository.BindingRepository
import com.example.mobile_tracker.data.repository.ReferenceRepository
import com.example.mobile_tracker.domain.model.Employee
import io.ktor.client.call.body
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

data class RegisterWatchState(
    val deviceId: String = "",
    val model: String? = null,
    val firmware: String? = null,
    val appVersion: String? = null,
    val searchQuery: String = "",
    val allEmployees: List<Employee> = emptyList(),
    val filteredEmployees: List<Employee> = emptyList(),
    val isLoadingEmployees: Boolean = false,
    val selectedEmployee: Employee? = null,
    val isRegistering: Boolean = false,
    val isRegistered: Boolean = false,
    val registeredEmployeeName: String = "",
    val error: String? = null,
)

sealed interface RegisterWatchEffect {
    data class ShowSuccess(val employeeName: String, val deviceId: String) : RegisterWatchEffect
    data class ShowError(val message: String) : RegisterWatchEffect
}

class RegisterWatchViewModel(
    private val employeeDao: EmployeeDao,
    private val shiftContextDao: ShiftContextDao,
    private val deviceRegistrationApi: DeviceRegistrationApi,
    private val bindingRepository: BindingRepository,
    private val referenceRepository: ReferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterWatchState())
    val state: StateFlow<RegisterWatchState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<RegisterWatchEffect>()
    val effect: SharedFlow<RegisterWatchEffect> = _effect.asSharedFlow()

    private var siteId: String = ""
    private var shiftDate: String = ""

    init {
        loadContext()
    }

    private fun loadContext() {
        viewModelScope.launch {
            val ctx = shiftContextDao.get()
            if (ctx != null) {
                siteId = ctx.siteId
                shiftDate = ctx.shiftDate
                loadEmployees()
            } else {
                _state.update { it.copy(error = "Контекст смены не выбран") }
            }
        }
    }

    private fun loadEmployees() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingEmployees = true) }
            employeeDao.observeBySite(siteId).collect { entities ->
                val employees = entities.map { it.toDomain() }
                _state.update {
                    it.copy(
                        allEmployees = employees,
                        filteredEmployees = filterByQuery(employees, it.searchQuery),
                        isLoadingEmployees = false,
                    )
                }
            }
        }
    }

    fun applyScannedData(rawValue: String) {
        val parsed = parseQrPayload(rawValue)
        _state.update {
            it.copy(
                deviceId = parsed.deviceId,
                model = parsed.model,
                firmware = parsed.firmware,
                appVersion = parsed.appVersion,
                error = null,
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _state.update {
            it.copy(
                searchQuery = query,
                filteredEmployees = filterByQuery(it.allEmployees, query),
                error = null,
            )
        }
    }

    fun selectEmployee(employee: Employee) {
        _state.update {
            it.copy(
                selectedEmployee = employee,
                error = null,
            )
        }
    }

    fun clearSelectedEmployee() {
        _state.update {
            it.copy(
                selectedEmployee = null,
                error = null,
            )
        }
    }

    fun registerWithEmployee(employee: Employee) {
        val deviceId = _state.value.deviceId
        if (deviceId.isBlank()) return
        _state.update { it.copy(selectedEmployee = employee, error = null) }
        registerAndBind()
    }

    fun registerAndBind() {
        val employee = _state.value.selectedEmployee ?: return
        val deviceId = _state.value.deviceId
        if (deviceId.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isRegistering = true, error = null) }
            try {
                val request = MobileRegisterRequest(
                    deviceId = deviceId,
                    employeeId = employee.id,
                    siteId = siteId,
                    model = _state.value.model,
                    firmware = _state.value.firmware,
                    appVersion = _state.value.appVersion,
                )
                val response = deviceRegistrationApi.registerWatchViaMobile(request)
                val code = response.status.value
                when {
                    code in 200..201 -> {
                        val body = response.body<MobileRegisterResponse>()
                        Timber.d("Watch registered: device_id=${body.deviceId}, status=${body.status}")
                        bindingRepository.refreshBindings(
                            siteId = siteId,
                            shiftDate = shiftDate,
                        ).onFailure { error ->
                            Timber.w(error, "Bindings refresh failed after watch registration")
                        }
                        referenceRepository.syncDevices(siteId)
                            .onFailure { error ->
                                Timber.w(error, "Device sync failed after watch registration")
                            }
                        _state.update {
                            it.copy(
                                isRegistering = false,
                                isRegistered = true,
                                registeredEmployeeName = employee.fullName,
                            )
                        }
                        _effect.emit(
                            RegisterWatchEffect.ShowSuccess(
                                employeeName = employee.fullName,
                                deviceId = deviceId,
                            ),
                        )
                    }
                    code == 409 -> {
                        _state.update {
                            it.copy(
                                isRegistering = false,
                                error = "Часы $deviceId уже зарегистрированы",
                            )
                        }
                        _effect.emit(
                            RegisterWatchEffect.ShowError("Часы $deviceId уже зарегистрированы"),
                        )
                    }
                    else -> {
                        val msg = "Ошибка регистрации: HTTP $code"
                        _state.update { it.copy(isRegistering = false, error = msg) }
                        _effect.emit(RegisterWatchEffect.ShowError(msg))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Register watch failed")
                val msg = e.message ?: "Ошибка регистрации"
                _state.update { it.copy(isRegistering = false, error = msg) }
                _effect.emit(RegisterWatchEffect.ShowError(msg))
            }
        }
    }

    fun reset() {
        _state.update { RegisterWatchState() }
    }

    private fun filterByQuery(employees: List<Employee>, query: String): List<Employee> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return employees
        return employees.filter { emp ->
            emp.fullName.lowercase().contains(q) ||
                emp.position?.lowercase()?.contains(q) == true
        }
    }

    private fun parseQrPayload(raw: String): QrData {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val obj = json.parseToJsonElement(raw).jsonObject
            QrData(
                deviceId = obj["device_id"]?.jsonPrimitive?.content ?: raw.trim(),
                model = obj["model"]?.jsonPrimitive?.content,
                firmware = obj["firmware"]?.jsonPrimitive?.content,
                appVersion = obj["app_version"]?.jsonPrimitive?.content,
            )
        } catch (_: Exception) {
            QrData(deviceId = raw.trim())
        }
    }

    private data class QrData(
        val deviceId: String,
        val model: String? = null,
        val firmware: String? = null,
        val appVersion: String? = null,
    )
}
