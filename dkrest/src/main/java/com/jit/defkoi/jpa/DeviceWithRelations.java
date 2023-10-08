package com.jit.defkoi.jpa;

import org.springframework.data.rest.core.config.Projection;

import java.util.Set;

@Projection(name = "withRelations", types = { Device.class })
public interface DeviceWithRelations extends BaseRestEntityWithRelations {

  Boolean getEnabled();

  String getDisplayName();

  String getDeviceClass();

  String getDevicePath();

  String getDeviceApi();

  String getDeviceDriver();

  Integer getDeviceVersion();

  String getDeviceCard();

  String getDeviceBusInfo();

  Set<Capability> getCaps();

  PipeConf getPipeConf();

}

