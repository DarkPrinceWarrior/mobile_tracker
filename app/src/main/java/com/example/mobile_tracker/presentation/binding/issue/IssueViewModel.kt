package com.example.mobile_tracker.presentation.binding.issue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_tracker.data.local.db.dao.DeviceDao
import com.example.mobile_tracker.data.local.db.dao.EmployeeDao
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.data.remote.dto.toDomain
import com.example.mobile_tracker.data.repository.BindingRepository
import com.example.mobile_tracker.domain.model.Device
import com.example.mobile_tracker.domain.model.Employee
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class IssueViewModel(
    private val employeeDao: EmployeeDao,
    private val deviceDao: DeviceDao,
    private val shiftContextDao: ShiftContextDao,
    private val bindingRepository: BindingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(IssueState())
    val state: StateFlow<IssueState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<IssueEffect>()
    val effect: SharedFlow<IssueEffect> =
        _effect.asSharedFlow()

    private var siteId: String = ""
    private var shiftDate: String = ""
    private var shiftType: String = "day"
    private var operatorId: String = ""

    init {
        loadContext()
    }

    fun onIntent(intent: IssueIntent) {
        when (intent) {
            is IssueIntent.UpdateSearchQuery ->
                updateSearchQuery(intent.query)
            is IssueIntent.UpdateDeviceSearchQuery ->
                updateDeviceSearchQuery(intent.query)
            is IssueIntent.SelectEmployee ->
                selectEmployee(intent.employee)
            is IssueIntent.SelectDevice ->
                selectDevice(intent.device)
            is IssueIntent.ApplyScannedDevice ->
                applyScannedDevice(intent.value)
            IssueIntent.ContinueWithSelectedDevice ->
                continueWithSelectedDevice()
            IssueIntent.ConfirmIssue ->
                confirmIssue()
            IssueIntent.GoBack ->
                goBack()
            IssueIntent.Reset ->
                reset()
            IssueIntent.DismissError ->
                _state.update {
                    it.copy(
                        error = null,
                        validationError = null,
                    )
                }
        }
    }

    private fun loadContext() {
        viewModelScope.launch {
            val ctx = shiftContextDao.get()
            if (ctx != null) {
                siteId = ctx.siteId
                shiftDate = ctx.shiftDate
                shiftType = ctx.shiftType
                operatorId = ctx.operatorId
                loadEmployees()
            } else {
                _state.update {
                    it.copy(
                        error = "Контекст смены не выбран",
                    )
                }
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
                        filteredEmployees = filterEmployees(employees, it.searchQuery),
                        isLoadingEmployees = false,
                    )
                }
            }
        }
    }

    private fun updateSearchQuery(query: String) {
        _state.update {
            it.copy(
                searchQuery = query,
                filteredEmployees = filterEmployees(it.allEmployees, query),
                error = null,
            )
        }
    }

    private fun filterEmployees(employees: List<Employee>, query: String): List<Employee> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return employees
        return employees.filter { emp ->
            emp.fullName.lowercase().contains(q) ||
                emp.personnelNumber?.lowercase()?.contains(q) == true ||
                emp.position?.lowercase()?.contains(q) == true
        }
    }

    private fun updateDeviceSearchQuery(query: String) {
        _state.update {
            it.copy(
                deviceSearchQuery = query,
                filteredDevices = filterDevices(it.availableDevices, query),
            )
        }
    }

    private fun filterDevices(devices: List<Device>, query: String): List<Device> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return devices
        return devices.filter { device ->
            device.deviceId.lowercase().contains(q) ||
                device.serialNumber?.lowercase()?.contains(q) == true ||
                device.model?.lowercase()?.contains(q) == true
        }
    }

    private fun selectEmployee(employee: Employee) {
        _state.update {
            it.copy(
                selectedEmployee = employee,
                step = IssueStep.SELECT_DEVICE,
                error = null,
                validationError = null,
            )
        }
        loadAvailableDevices()
    }

    private fun loadAvailableDevices() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val entities = deviceDao.getAvailable(siteId)
            val devices = entities.map { it.toDomain() }
            _state.update {
                it.copy(
                    availableDevices = devices,
                    filteredDevices = filterDevices(devices, it.deviceSearchQuery),
                    isLoading = false,
                    selectedDevice = null,
                )
            }
        }
    }

    private fun selectDevice(device: Device) {
        _state.update {
            it.copy(
                selectedDevice = device,
                validationError = null,
            )
        }
    }

    private fun continueWithSelectedDevice() {
        val selected = _state.value.selectedDevice
        if (selected != null) {
            _state.update {
                it.copy(
                    step = IssueStep.CONFIRM,
                    validationError = null,
                )
            }
        } else {
            _state.update {
                it.copy(
                    validationError = "Выберите часы для продолжения",
                )
            }
        }
    }

    private fun applyScannedDevice(value: String) {
        val normalized = value.trim()
        if (normalized.isBlank()) return
        val matchedDevice = _state.value.availableDevices.firstOrNull { device ->
            device.deviceId.equals(normalized, ignoreCase = true) ||
                device.serialNumber.equals(normalized, ignoreCase = true)
        }
        if (matchedDevice != null) {
            _state.update {
                it.copy(
                    selectedDevice = matchedDevice,
                    step = IssueStep.CONFIRM,
                    validationError = null,
                )
            }
        } else {
            _state.update {
                it.copy(
                    validationError = "Устройство не найдено среди доступных",
                )
            }
        }
    }

    private fun confirmIssue() {
        val employee = _state.value.selectedEmployee ?: return
        val device = _state.value.selectedDevice ?: return

        viewModelScope.launch {
            _state.update {
                it.copy(isIssuing = true, error = null)
            }
            bindingRepository.issueDevice(
                deviceId = device.deviceId,
                employeeId = employee.id,
                employeeName = employee.fullName,
                siteId = siteId,
                shiftDate = shiftDate,
                shiftType = shiftType,
                operatorId = operatorId,
            ).fold(
                onSuccess = {
                    _state.update {
                        it.copy(isIssuing = false)
                    }
                    _effect.emit(
                        IssueEffect.ShowSuccess(
                            employeeName = employee.fullName,
                            deviceId = device.deviceId,
                        ),
                    )
                    reset()
                },
                onFailure = { e ->
                    Timber.e(e, "Issue device failed")
                    _state.update {
                        it.copy(
                            isIssuing = false,
                            validationError =
                                e.message
                                    ?: "Ошибка выдачи",
                        )
                    }
                    _effect.emit(
                        IssueEffect.ShowError(
                            e.message ?: "Ошибка выдачи",
                        ),
                    )
                },
            )
        }
    }

    private fun goBack() {
        when (_state.value.step) {
            IssueStep.SELECT_DEVICE -> _state.update {
                it.copy(
                    step = IssueStep.IDENTIFY_EMPLOYEE,
                    selectedDevice = null,
                    deviceSearchQuery = "",
                    validationError = null,
                )
            }
            IssueStep.CONFIRM -> _state.update {
                it.copy(
                    step = IssueStep.SELECT_DEVICE,
                    validationError = null,
                )
            }
            IssueStep.IDENTIFY_EMPLOYEE -> { /* no-op */ }
        }
    }

    private fun reset() {
        _state.update { IssueState() }
    }
}
