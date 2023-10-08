package com.jit.defkoi.jpa;

import org.springframework.data.rest.core.config.Projection;

import java.util.Date;

@Projection(name = "withRelations", types = { BaseRestEntity.class })
public interface BaseRestEntityWithRelations {

  Integer getId();

  String getName();

  Date getModTime();

  Date getRetireTime();

  boolean isActive();

}
