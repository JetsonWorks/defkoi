package com.jit.defkoi.jpa;

import org.springframework.data.rest.core.config.Projection;

import java.util.Set;

@Projection(name = "withRelations", types = { PipeConf.class })
public interface PipeConfWithRelations extends BaseRestEntityWithRelations {

  Boolean getEnabled();

  Boolean getNvEnabled();

  Device getDevice();

  Boolean getMotDetectEnabled();

  Boolean getObjDetectEnabled();

  default Set<Capability> getValidCaps() {
    return getDevice() == null ? null : getDevice().getCaps();
  }

  Capability getCap();

}

