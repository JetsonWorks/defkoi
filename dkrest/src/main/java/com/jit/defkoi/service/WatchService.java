package com.jit.defkoi.service;

import com.jit.defkoi.jpa.Watch;
import com.jit.defkoi.jpa.WatchRepository;
import com.jit.defkoi.jpa.WatchSpecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WatchService extends BaseRestEntityService<Watch, WatchRepository> {

  @Autowired
  private DeviceService deviceService;

  public WatchService(@Autowired WatchRepository repo) {
    super(repo);
  }

  @Transactional
  @Modifying
  protected void checkUniqueness(Watch form, @Nullable Watch entity) throws NonUniqueObjectException {
    Watch test = repo.findByName(form.getName());
    if(test != null)
      throw new NonUniqueObjectException(test.toString());
    if(form.getName() != null)
      entity.setName(form.getName());
  }

  @Override
  public Watch doCreate(Watch form) {
    return new Watch();
  }

  @Override
  protected Watch doSave(Watch entity, Watch form) {
    if(form.getName() != null)
      entity.setName(form.getName());
    if(form.getConfidence() != null)
      entity.setConfidence(form.getConfidence());
    if(form.getFrameArea() != null)
      entity.setFrameArea(form.getFrameArea());
    return save(entity);
  }

  @Override
  protected Specification containsFilterText(Watch form) {
    return Specification.where(WatchSpecs.extendedContains(form.getFilterText()));
  }

}
