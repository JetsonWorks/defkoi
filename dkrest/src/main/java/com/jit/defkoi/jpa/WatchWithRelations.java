package com.jit.defkoi.jpa;

import org.springframework.data.rest.core.config.Projection;

@Projection(name = "withRelations", types = { Watch.class })
public interface WatchWithRelations extends BaseRestEntityWithRelations {

  Double getConfidence();

  Double getFrameArea();

}

