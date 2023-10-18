package com.jit.defkoi.jpa;

import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(excerptProjection = Watch.class)
public interface WatchRepository extends BaseRestEntityRepository<Watch> {

}
