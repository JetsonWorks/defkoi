package com.jit.defkoi.service;

import com.jit.defkoi.jpa.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CapabilityService extends BaseRestEntityService<Capability, CapabilityRepository> {

  @Autowired
  private DeviceService deviceService;

  public CapabilityService(@Autowired CapabilityRepository repo) {
    super(repo);
  }

  @Transactional(readOnly = true)
  public Set<Capability> findActiveByWidthLe(@NotNull Integer maxWidth) {
    return repo.findByWidthLessThanEqual(maxWidth).stream().filter(BaseRestEntity::isActive)
      .collect(Collectors.toSet());
  }

  @Transactional(readOnly = true)
  public Set<Capability> findActiveByHeightLe(@NotNull Integer maxHeight) {
    return repo.findByHeightLessThanEqual(maxHeight).stream().filter(BaseRestEntity::isActive)
      .collect(Collectors.toSet());
  }

  @Transactional(readOnly = true)
  public Set<Capability> findActiveByFramerateGe(@NotNull Double maxFramerate) {
    return repo.findByFramerateMinGreaterThanEqual(maxFramerate).stream().filter(BaseRestEntity::isActive)
      .collect(Collectors.toSet());
  }

  @Transactional
  @Modifying
  protected void checkUniqueness(Capability form, @Nullable Capability entity)
    throws NonUniqueObjectException, EmptyKeyException {
    boolean uniqueCheck = false;

    if(entity == null)
      entity = new Capability();

    Device device = entity.getDevice();
    if(form.getDeviceId() == null) {
      if(device == null)
        throw new EmptyKeyException("A Capability must have a Device");
    } else
      device = deviceService.get(Integer.parseInt(form.getDeviceId()));
    uniqueCheck |= !device.equals(entity.getDevice());

    String name = entity.getName();
    if(form.getName() != null && !form.getName().equals(entity.getName())) {
      if(!StringUtils.hasLength(form.getName()))
        throw new EmptyKeyException("A Capability must have a name");
      name = form.getName();
      uniqueCheck = true;
    }

    String format = entity.getFormat();
    if(form.getFormat() != null && !form.getFormat().equals(entity.getFormat())) {
      if(!StringUtils.hasLength(form.getFormat()))
        throw new EmptyKeyException("A Capability must have a format");
      format = form.getFormat();
      uniqueCheck = true;
    }

    Integer width = entity.getWidth();
    if(form.getWidth() != null && !form.getWidth().equals(entity.getWidth())) {
      width = form.getWidth();
      uniqueCheck = true;
    }

    Integer height = entity.getHeight();
    if(form.getHeight() != null && !form.getHeight().equals(entity.getHeight())) {
      height = form.getHeight();
      uniqueCheck = true;
    }

    Double aspectRatio = entity.getAspectRatio();
    if(form.getAspectRatio() != null && !form.getAspectRatio().equals(entity.getAspectRatio())) {
      aspectRatio = form.getAspectRatio();
      uniqueCheck = true;
    }

    Double framerateMin = entity.getFramerateMin();
    if(form.getFramerateMin() != null && !form.getFramerateMin().equals(entity.getFramerateMin())) {
      framerateMin = form.getFramerateMin();
      uniqueCheck = true;
    }

    Double framerateMax = entity.getFramerateMax();
    if(form.getFramerateMax() != null && !form.getFramerateMax().equals(entity.getFramerateMax())) {
      framerateMax = form.getFramerateMax();
      uniqueCheck = true;
    }

    // TODO: test one or more values being null on new Capability
    if(uniqueCheck) {
      Capability test =
        repo.findByDeviceAndNameAndFormatAndWidthAndHeightAndAspectRatioAndFramerateMinAndFramerateMax(device,
          form.getName(), form.getFormat(), form.getWidth(), form.getHeight(), form.getAspectRatio(),
          form.getFramerateMin(), form.getFramerateMax());
      if(test != null && !test.getId().equals(entity.getId()))
        throw new NonUniqueObjectException(entity.toString());
    }
    // only set the changed values
    if(form.getDevice() != null)
      entity.setDevice(device);
    if(form.getName() != null)
      entity.setName(form.getName());
    if(form.getFormat() != null)
      entity.setFormat(form.getFormat());
    if(form.getWidth() != null)
      entity.setWidth(form.getWidth());
    if(form.getHeight() != null)
      entity.setHeight(form.getHeight());
    if(form.getAspectRatio() != null)
      entity.setAspectRatio(form.getAspectRatio());
    if(form.getFramerateMin() != null)
      entity.setFramerateMin(form.getFramerateMin());
    if(form.getFramerateMax() != null)
      entity.setFramerateMax(form.getFramerateMax());
  }

  @Override
  public Capability doCreate(Capability form) {
    Device device = deviceService.findByLastModTime();
    Capability cap = new Capability();
    if(form.getDeviceId() == null)
      cap.setDevice(device);
    else
      cap.setDevice(deviceService.get(Integer.parseInt(form.getDeviceId())));
    cap.setFormat("");
    cap.setWidth(0);
    cap.setHeight(0);
    cap.setAspectRatio(1.0);
    cap.setFramerateMin(0.0);
    cap.setFramerateMax(0.0);
    return cap;
  }

  @Override
  protected Capability doSave(Capability entity, Capability form) {
    return save(entity);
  }

  @Override
  protected Specification containsFilterText(Capability form) {
    return Specification.where(CapabilitySpecs.extendedContains(form.getFilterText()));
  }

}
