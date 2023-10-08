package com.jit.defkoi.service;

import com.jit.defkoi.jpa.*;
import com.oblac.nomen.Nomen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

import static com.jit.defkoi.jpa.BaseRestEntity.equalsDefaultRetireTime;
import static com.jit.defkoi.jpa.BaseRestEntity.getDefaultRetireTime;

public abstract class BaseRestEntityService<T extends BaseRestEntity, R extends BaseRestEntityRepository<T>> {

  @Autowired
  protected UserRepository userRepo;
  @Autowired
  protected UserEventService userEventService;

  protected R repo;

  public BaseRestEntityService(R baseRestRepo) {
    this.repo = baseRestRepo;
  }

  public T findByName(String name) {
    T entity = repo.findByName(name);
    if(entity == null)
      throw new NoSuchElementException(name);
    return entity;
  }

  public Optional<T> findById(Integer id) {
    return repo.findById(id);
  }

  public T get(Integer id) {
    return repo.getReferenceById(id);
  }

  public List<T> findAllByOrderByName() {
    return repo.findAllByOrderByName();
  }

  public Set<T> findActive() {
    return repo.findByRetireTimeGreaterThan(new Date());
  }

  public List<T> findAllByRetireTimeGreaterThanOrderByName(Date date) {
    return repo.findByRetireTimeGreaterThanEqualOrderByName(date);
  }

  public T findByLastModTime() {
    return repo.findFirstByOrderByModTimeDesc();
  }

  public List<T> search(boolean showRetired, T form) {
    boolean hideRetired = Boolean.FALSE.equals(showRetired);
    Specification specs = null;

    if(hideRetired)
      specs =
        specs == null ? Specification.where(BaseRestEntitySpecs.isActive()) : specs.and(BaseRestEntitySpecs.isActive());
    if(StringUtils.hasLength(form.getFilterText()))
      specs = specs == null ? Specification.where(containsFilterText(form)) : specs.and(containsFilterText(form));
    Specification add = additionalSpecification(form);
    if(add != null)
      specs = specs == null ? add : specs.and(add);

    if(specs == null)
      return repo.findAll();
    else
      return repo.findAll(specs);
  }

  /** Override to build Specification for class-specific field value(s) containing filterText */
  protected Specification containsFilterText(T form) {
    return Specification.where(BaseRestEntitySpecs.nameContains(form.getName()));
  }

  /** Override to build class-specific Specification */
  protected Specification additionalSpecification(T form) {
    return null;
  }

  public List<T> find() {
    return repo.findByRetireTimeGreaterThanEqualOrderByName(new Date());
  }

  @Transactional
  @Modifying
  public T edit(Integer id, T form) throws NonUniqueObjectException, EmptyKeyException, UnsupportedChangeException {
    Optional<T> op = repo.findById(id);
    if(!op.isPresent())
      throw new NoSuchElementException("" + id);
    checkUniqueness(form, op.get());
    return save(op.get(), form);
  }

  @Transactional
  @Modifying
  public T create(T form) throws NonUniqueObjectException, EmptyKeyException, UnsupportedChangeException {
    form.setName(String.format("_%s_%s", form.getName(), Nomen.est().adjective().person().get()));
    T entity = doCreate(form);
    checkUniqueness(form, entity);
    return save(entity, form);
  }

  /**
   * Determines if a uniqueness check is warranted, and if so, tests for uniqueness.
   * @param form
   * @param entity null if not updating an existing entity
   * @throws NonUniqueObjectException
   */
  @Transactional
  @Modifying
  protected void checkUniqueness(T form, @Nullable T entity)
    throws NonUniqueObjectException, EmptyKeyException, UnsupportedChangeException {
    if(entity != null && entity.getId() != null && entity.getName().equals(form.getName()) || form.getName() == null)
      return;
    T test = repo.findByName(form.getName());
    if(test != null)
      throw new NonUniqueObjectException(test.toString());
    entity.setName(form.getName());
  }

  public abstract T doCreate(T form) throws NonUniqueObjectException;

  @Transactional
  @Modifying
  public T save(T entity) {
    return repo.save(entity);
  }

  /** Copy non-null values from form to entity and ensure name is not empty */
  @Transactional
  @Modifying
  protected T save(T entity, T form) throws NonUniqueObjectException, EmptyKeyException, UnsupportedChangeException {
    // if retireTime selection doesn't match the retireTime, set the retireTime
    if(form.getSetActive() != null)
      if(form.getSetActive()) {
        if(!equalsDefaultRetireTime(entity.getRetireTime()))
          entity.setRetireTime(getDefaultRetireTime());
      } else {
        if(equalsDefaultRetireTime(entity.getRetireTime()))
          entity.setRetireTime(new Date());
      }
    return doSave(entity, form);
  }

  @Transactional
  @Modifying
  protected abstract T doSave(T entity, T form) throws UnsupportedChangeException;

  public void saveAll(Set<T> entities) {
    repo.saveAll(entities);
  }

  @Transactional
  @Modifying
  public void delete(T entity) {
    entity.removeFromReferences();
    repo.delete(entity);
  }

  @Transactional
  @Modifying
  public void delete(Integer id) {
    Optional<T> op = repo.findById(id);
    if(!op.isPresent())
      throw new NoSuchElementException("" + id);
    delete(op.get());
  }

  @Transactional
  @Modifying
  public T retire(Integer id) {
    Optional<T> op = repo.findById(id);
    if(!op.isPresent())
      throw new NoSuchElementException("" + id);

    T entity = op.get();
    entity.setRetireTime(new Date());
    userEventService.save(UserEvent.retired(userRepo.loggedUser(true), op.get()));
    return save(entity);
  }

  public Long count() {
    return repo.count();
  }

  public T findLastMod() {
    return repo.findFirstByOrderByModTimeDesc();
  }

}

