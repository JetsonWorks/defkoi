package com.jit.defkoi.rest;

import com.jit.defkoi.jpa.Capability;
import com.jit.defkoi.jpa.CapabilityRepository;
import com.jit.defkoi.service.CapabilityService;
import com.jit.defkoi.service.ObjectInUseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.jit.defkoi.service.pref.PreferenceKey.showRetiredCapabilities;

@RestController
public class CapabilityController
  extends BaseRestEntityController<Capability, CapabilityRepository, CapabilityService> {

  public CapabilityController(@Autowired CapabilityService service) {
    super(service, "capability", showRetiredCapabilities, "NotEmpty.Capability.name",
      "com.jit.defkoi.validation.NonUniqueCapability.name");
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_USER')")
  @RequestMapping(value = "/capability/search")
  public List<Capability> search(Capability form) {
    return super.search(form);
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_OPERATOR')")
  @RequestMapping(value = "/capability/add", method = RequestMethod.POST)
  public ResponseEntity add(@Valid Capability form, BindingResult bindingResult, Locale locale) throws RestException {
    return super.add(form, bindingResult, locale);
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_OPERATOR')")
  @RequestMapping(value = "/capability/edit/{id}", method = RequestMethod.POST)
  public ResponseEntity edit(@PathVariable("id") Integer id, @Valid Capability form, BindingResult bindingResult,
    Locale locale) throws RestException {
    return super.edit(id, form, bindingResult, locale);
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_OPERATOR')")
  @RequestMapping(value = "/capability/retire/{id}")
  public ResponseEntity retire(@PathVariable("id") Integer id) {
    return super.retire(id);
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_OPERATOR')")
  @RequestMapping(value = "/capability/delete/{id}")
  public ResponseEntity delete(@PathVariable("id") Integer id) throws ObjectInUseException {
    return super.delete(id);
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_USER')")
  @RequestMapping(value = "/capability/count")
  public Long count() {
    return super.count();
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_USER')")
  @RequestMapping(value = "/capability/modTime")
  public Date modTime() {
    return super.modTime();
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_USER')")
  @RequestMapping(value = "/capability/metadata")
  public Map<String, Object> metadata(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date lastSync) {
    return super.metadata(lastSync);
  }

}

