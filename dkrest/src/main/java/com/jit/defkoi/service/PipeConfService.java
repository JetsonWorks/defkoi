package com.jit.defkoi.service;

import com.jit.defkoi.jpa.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class PipeConfService extends BaseRestEntityService<PipeConf, PipeConfRepository> {

  @Autowired
  private DeviceService deviceService;
  @Autowired
  private CapabilityService capService;

  public PipeConfService(@Autowired PipeConfRepository repo) {
    super(repo);
  }

  public PipeConf findByDevice(Device device) {
    return repo.findByDevice(device);
  }

  @Transactional
  @Modifying
  protected void checkUniqueness(PipeConf form, @Nullable PipeConf entity) throws NonUniqueObjectException {
    PipeConf test = repo.findByName(form.getName());
    if(test != null)
      throw new NonUniqueObjectException(test.toString());
    if(form.getName() != null)
      entity.setName(form.getName());
  }

  @Override
  public PipeConf doCreate(PipeConf form) {
    return new PipeConf();
  }

  @Override
  protected PipeConf doSave(PipeConf entity, PipeConf form) {
    if(form.getEnabled() != null) {
      boolean enabled = form.getEnabled();
      entity.setEnabled(enabled);
      if(enabled)
        entity.getDevice().setEnabled(enabled);
    }
    if(form.getNvEnabled() != null)
      entity.setNvEnabled(form.getNvEnabled().booleanValue());
    if(form.getName() != null)
      entity.setName(form.getName());
    if(form.getDeviceId() != null) {
      entity.setDevice(deviceService.get(form.getDeviceId()));
      entity.getDevice().setPipeConf(entity);
      deviceService.save(entity.getDevice());
    }
    if(form.getCapId() != null) {
      entity.setCap(capService.get(form.getCapId()));
      entity.getCap().setPipeConf(entity);
      capService.save(entity.getCap());
    }
    if(form.getMotDetectEnabled() != null)
      entity.setMotDetectEnabled(form.getMotDetectEnabled());
    if(form.getObjDetectEnabled() != null)
      entity.setObjDetectEnabled(form.getObjDetectEnabled());
    return save(entity);
  }

  @Override
  protected Specification containsFilterText(PipeConf form) {
    return Specification.where(PipeConfSpecs.extendedContains(form.getFilterText()));
  }

  public List<String> getAvailableResolutions() {
    List<Capability> allCaps = capService.find();
    TreeSet<Capability> caps = new TreeSet<>(Comparator.comparingInt(o -> o.getWidth() * o.getHeight()));
    caps.addAll(allCaps.stream().filter(o -> "video/x-raw".equals(o.getName())).collect(Collectors.toSet()));
    List<String> sorted = new ArrayList<>();
    for(Capability cap : caps)
      sorted.add(String.format("%dx%d", cap.getWidth(), cap.getHeight()));
    return sorted;
  }

}
