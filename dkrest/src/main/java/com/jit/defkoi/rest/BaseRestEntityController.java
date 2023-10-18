package com.jit.defkoi.rest;

import com.jit.defkoi.jpa.BaseRestEntity;
import com.jit.defkoi.jpa.BaseRestEntityRepository;
import com.jit.defkoi.jpa.UserEvent;
import com.jit.defkoi.jpa.UserRepository;
import com.jit.defkoi.jpa.pref.IPrefKey;
import com.jit.defkoi.service.*;
import com.jit.defkoi.service.pref.PreferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @param <T> JPA class
 * @param <R> BaseRestEntityRepository for T
 * @param <S> BaseRestEntityService for T
 */
public abstract class BaseRestEntityController<T extends BaseRestEntity, R extends BaseRestEntityRepository<T>, S extends BaseRestEntityService<T, R>> {

  protected static final Date uptime = new Date();

  @Autowired
  protected MessageSource messageSource;
  @Autowired
  protected UserRepository userRepo;
  @Autowired
  private PreferenceService prefService;
  @Autowired
  private UserEventService userEventService;

  protected IPrefKey showRetiredPrefsKey;
  protected String emptyKeyKey;
  protected String nonUniqueKey;

  protected S service;
  protected String entityClassName = null;

  protected BaseRestEntityController(S service, String entityClassName, IPrefKey showRetiredPrefsKey,
    String emptyKeyKey, String nonUniqueKey) {
    this.service = service;
    this.entityClassName = entityClassName;
    this.showRetiredPrefsKey = showRetiredPrefsKey;
    this.emptyKeyKey = emptyKeyKey;
    this.nonUniqueKey = nonUniqueKey;
  }

  public List<T> search(T form) {
    boolean show = prefService.getPreferences().getPreference(showRetiredPrefsKey).getBooleanData();
    List<T> results = service.search(show, form);
    return results;
  }

  public ResponseEntity add(@Valid T form, BindingResult bindingResult, Locale locale) throws RestException {
    if(bindingResult.hasErrors())
      throw RestException.build(bindingResult, messageSource, locale);
    try {
      service.create(form);
      return new ResponseEntity(HttpStatus.OK);
    } catch(NonUniqueObjectException e) {
      bindingResult.rejectValue(nonUniqueKey.replaceAll(".*\\.", ""), nonUniqueKey);
      throw RestException.build(bindingResult, messageSource, locale);
    } catch(EmptyKeyException e) {
      bindingResult.rejectValue(emptyKeyKey.replaceAll(".*\\.", ""), emptyKeyKey);
      throw RestException.build(bindingResult, messageSource, locale);
    } catch(UnsupportedChangeException e) {
      bindingResult.rejectValue(emptyKeyKey.replaceAll(".*\\.", ""), emptyKeyKey);
      throw RestException.build(bindingResult, messageSource, locale);
    }
  }

  public ResponseEntity edit(@PathVariable("id") Integer id, @Valid T form, BindingResult bindingResult, Locale locale)
    throws RestException {
    if(bindingResult.hasErrors())
      throw RestException.build(bindingResult, messageSource, locale);
    try {
      service.edit(id, form);
      return new ResponseEntity(HttpStatus.OK);
    } catch(NonUniqueObjectException e) {
      bindingResult.rejectValue(nonUniqueKey.replaceAll(".*\\.", ""), nonUniqueKey);
      throw RestException.build(bindingResult, messageSource, locale);
    } catch(EmptyKeyException e) {
      bindingResult.rejectValue(emptyKeyKey.replaceAll(".*\\.", ""), emptyKeyKey);
      throw RestException.build(bindingResult, messageSource, locale);
    } catch(UnsupportedChangeException e) {
      bindingResult.rejectValue(emptyKeyKey.replaceAll(".*\\.", ""), emptyKeyKey);
      throw RestException.build(bindingResult, messageSource, locale);
    }
  }

  public ResponseEntity retire(@PathVariable("id") Integer id) {
    service.retire(id);
    return new ResponseEntity(HttpStatus.OK);
  }

  public ResponseEntity delete(@PathVariable("id") Integer id) throws ObjectInUseException {
    service.delete(id);
    return new ResponseEntity(HttpStatus.OK);
  }

  public Long count() {
    return service.count();
  }

  public Date modTime() {
    return service.findLastMod().getModTime();
  }

  public Map<String, Object> metadata(Date lastSync) {
    Map<String, Object> map = new HashMap<>();
    T lastMod = service.findLastMod();
    Date modTime = lastMod == null ? uptime : lastMod.getModTime();

    List<UserEvent> deleted = userEventService.findDeletedSince(entityClassName, lastSync);
    if(!deleted.isEmpty()) {
      if(deleted.get(0).getTimeStamp().after(modTime))
        modTime = deleted.get(0).getTimeStamp();
    }

    map.put("modTime", modTime);
    map.put("count", service.count());
    map.put("deleted", deleted.stream().map(UserEvent::getEntityId).collect(Collectors.toList()));
    return map;
  }

}

