package com.example.mobile_tracker.data.remote.dto

import com.example.mobile_tracker.data.local.db.entity.DeviceEntity
import com.example.mobile_tracker.data.local.db.entity.DowntimeReasonEntity
import com.example.mobile_tracker.data.local.db.entity.EmployeeEntity
import com.example.mobile_tracker.data.local.db.entity.SiteEntity
import com.example.mobile_tracker.domain.model.Device
import com.example.mobile_tracker.domain.model.Employee
import com.example.mobile_tracker.domain.model.Site

fun EmployeeDto.toEntity(syncedAt: Long): EmployeeEntity =
    EmployeeEntity(
        id = id,
        fullName = fullName,
        companyId = companyId,
        companyName = companyName,
        position = position,
        passNumber = passNumber,
        personnelNumber = personnelNumber,
        brigadeId = brigadeId,
        brigadeName = brigadeName,
        siteId = siteId,
        status = status,
        syncedAt = syncedAt,
    )

fun EmployeeEntity.toDomain(): Employee =
    Employee(
        id = id,
        fullName = fullName,
        companyName = companyName,
        position = position,
        passNumber = passNumber,
        personnelNumber = personnelNumber,
        brigadeName = brigadeName,
        siteId = siteId,
    )

fun DeviceDto.toEntity(syncedAt: Long): DeviceEntity =
    DeviceEntity(
        deviceId = deviceId,
        serialNumber = serialNumber,
        model = model,
        status = status,
        chargeStatus = chargeStatus,
        employeeId = employeeId,
        employeeName = employeeName,
        siteId = siteId,
        lastSyncAt = lastSyncAt,
        localStatus = if (employeeId != null) "issued"
            else "available",
        syncedAt = syncedAt,
    )

fun DeviceEntity.toDomain(): Device =
    Device(
        deviceId = deviceId,
        serialNumber = serialNumber,
        model = model,
        status = status,
        chargeStatus = chargeStatus,
        employeeId = employeeId,
        employeeName = employeeName,
        siteId = siteId,
        lastSyncAt = lastSyncAt,
        localStatus = localStatus,
    )

fun SiteDto.toEntity(syncedAt: Long): SiteEntity =
    SiteEntity(
        id = id,
        name = name,
        address = address,
        timezone = timezone,
        status = status,
        syncedAt = syncedAt,
    )

fun SiteEntity.toDomain(): Site =
    Site(
        id = id,
        name = name,
        address = address,
        timezone = timezone,
    )

fun DowntimeReasonDto.toEntity(
    syncedAt: Long,
): DowntimeReasonEntity =
    DowntimeReasonEntity(
        id = id,
        name = name,
        syncedAt = syncedAt,
    )
