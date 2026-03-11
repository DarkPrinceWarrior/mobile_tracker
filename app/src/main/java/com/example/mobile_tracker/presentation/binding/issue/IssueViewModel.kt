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
            is IssueIntent.UpdatePersonnelQuery ->
                _state.update {
                    it.copy(
                        personnelQuery = intent.query,
                        error = null,
                    )
                }
            is IssueIntent.UpdateNameQuery ->
                _state.update {
                    it.copy(
                        nameQuery = intent.query,
                        error = null,
                    )
                }
            IssueIntent.SearchByPersonnel ->
                searchByPersonnel()
            IssueIntent.SearchByName ->
                searchByName()
            is IssueIntent.ApplyScannedPass ->
                applyScannedPass(intent.value)
            is IssueIntent.SelectEmployee ->
                selectEmployee(intent.employee)
            is IssueIntent.SetAssignmentMode ->
                setAssignmentMode(intent.mode)
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
            } else {
                _state.update {
                    it.copy(
                        error = "Контекст смены не выбран",
                    )
                }
            }
        }
    }

    private fun searchByPersonnel() {
        val query = _state.value.personnelQuery.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _state.update {
                it.copy(isSearching = true, error = null)
            }
            val entity =
                employeeDao.findByPersonnelNumber(query)
            if (entity != null) {
                _state.update {
                    it.copy(
                        searchResults =
                            listOf(entity.toDomain()),
                        isSearching = false,
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        searchResults = emptyList(),
                        isSearching = false,
                        error = "Сотрудник с табельным номером $query не найден",
                    )
                }
            }
        }
    }

    private fun searchByName() {
        val query = _state.value.nameQuery.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _state.update {
                it.copy(isSearching = true, error = null)
            }
            val results =
                employeeDao.search(query, siteId)
            _state.update {
                it.copy(
                    searchResults =
                        results.map { e -> e.toDomain() },
                    isSearching = false,
                    error = if (results.isEmpty()) {
                        "По запросу ничего не найдено"
                    } else {
                        null
                    },
                )
            }
        }
    }

    private fun applyScannedPass(value: String) {
        val normalized = value.trim()
        if (normalized.isBlank()) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSearching = true,
                    error = null,
                    personnelQuery = normalized,
                )
            }
            val entity = employeeDao.findByPassNumber(normalized)
            if (entity != null) {
                selectEmployee(entity.toDomain())
                _state.update {
                    it.copy(
                        isSearching = false,
                        nameQuery = "",
                        searchResults = listOf(entity.toDomain()),
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isSearching = false,
                        error = "Сотрудник по номеру пропуска не найден",
                    )
                }
            }
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
                val recommendation = recommendedDevice(
                    devices = devices,
                    mode = it.assignmentMode,
                    employee = it.selectedEmployee,
                )
                it.copy(
                    availableDevices = devices,
                    isLoading = false,
                    selectedDevice = recommendation,
                    error = if (devices.isEmpty()) {
                        "Нет свободных часов"
                    } else {
                        null
                    },
                )
            }
        }
    }

    private fun setAssignmentMode(mode: AssignmentMode) {
        _state.update { current ->
            val recommendation = recommendedDevice(
                devices = current.availableDevices,
                mode = mode,
                employee = current.selectedEmployee,
            )
            current.copy(
                assignmentMode = mode,
                selectedDevice = recommendation,
                validationError = null,
            )
        }
    }

    private fun selectDevice(device: Device) {
        _state.update {
            it.copy(
                selectedDevice = device,
                assignmentMode = AssignmentMode.Manual,
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
                    assignmentMode = AssignmentMode.Manual,
                    selectedDevice = matchedDevice,
                    step = IssueStep.CONFIRM,
                    validationError = null,
                )
            }
        } else {
            _state.update {
                it.copy(
                    validationError = "Сканированные часы не найдены среди доступных устройств",
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

    private fun recommendedDevice(
        devices: List<Device>,
        mode: AssignmentMode,
        employee: Employee?,
    ): Device? {
        if (devices.isEmpty()) return null
        return when (mode) {
            AssignmentMode.Queue -> devices.first()
            AssignmentMode.Random -> {
                val seed = "${employee?.id.orEmpty()}|$shiftDate|$siteId".hashCode().toLong()
                val index = kotlin.math.abs(seed % devices.size).toInt()
                devices[index]
            }
            AssignmentMode.Manual -> devices.firstOrNull()
        }
    }
}
