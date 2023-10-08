package com.jit.defkoi.jpa;

import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(excerptProjection = CapabilityWithRelations.class)
public interface CapabilityRepository extends BaseRestEntityRepository<Capability> {

  Capability findByDeviceAndNameAndFormatAndWidthAndHeightAndAspectRatioAndFramerateMinAndFramerateMax(Device device,
    String name, String format, Integer width, Integer height, Double aspectRatio, Double framerateMin,
    Double framerateMax);

  List<Capability> findByWidthLessThanEqual(Integer width);

  List<Capability> findByHeightLessThanEqual(Integer height);

  List<Capability> findByFramerateMinGreaterThanEqual(Double framerateMin);

}
