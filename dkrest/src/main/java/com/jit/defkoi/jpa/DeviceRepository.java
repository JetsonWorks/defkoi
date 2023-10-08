package com.jit.defkoi.jpa;

import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Date;
import java.util.Set;

@RepositoryRestResource(excerptProjection = DeviceWithRelations.class)
public interface DeviceRepository extends BaseRestEntityRepository<Device> {

  Device findByDeviceCardAndDeviceBusInfo(String deviceCard, String deviceBusInfo);

  Device findByDeviceApiAndDevicePath(String deviceApi, String devicePath);

  Set<Device> findByDeviceApiAndRetireTimeGreaterThan(String api, Date date);

}
