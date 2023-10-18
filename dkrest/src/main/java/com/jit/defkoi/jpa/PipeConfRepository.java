package com.jit.defkoi.jpa;

import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(excerptProjection = PipeConfWithRelations.class)
public interface PipeConfRepository extends BaseRestEntityRepository<PipeConf> {

  PipeConf findByDevice(Device device);

}
