package com.jit.defkoi.jpa;

import org.springframework.data.rest.core.config.Projection;

@Projection(name = "withRelations", types = { Capability.class })
public interface CapabilityWithRelations extends BaseRestEntityWithRelations {

  String getFormat();

  Integer getWidth();

  Integer getHeight();

  Double getAspectRatio();

  Double getFramerateMin();

  Double getFramerateMax();

  Device getDevice();

  String getFormatted();

}

