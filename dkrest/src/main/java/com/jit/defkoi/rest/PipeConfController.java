package com.jit.defkoi.rest;

import com.jit.defkoi.jpa.PipeConf;
import com.jit.defkoi.jpa.PipeConfRepository;
import com.jit.defkoi.service.ObjectInUseException;
import com.jit.defkoi.service.PipeConfService;
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

import static com.jit.defkoi.service.pref.PreferenceKey.showRetiredPipeConfs;

@RestController
public class PipeConfController extends BaseRestEntityController<PipeConf, PipeConfRepository, PipeConfService> {

  public PipeConfController(@Autowired PipeConfService service) {
    super(service, "pipeConf", showRetiredPipeConfs, "NotEmpty.pipeConf.name",
      "com.jit.defkoi.validation.NonUniquePipeConf.name");
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_USER')")
  @RequestMapping(value = "/pipeConf/search")
  public List<PipeConf> search(PipeConf form) {
    return super.search(form);
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_OPERATOR')")
  @RequestMapping(value = "/pipeConf/add", method = RequestMethod.POST)
  public ResponseEntity add(@Valid PipeConf form, BindingResult bindingResult, Locale locale) throws RestException {
    return super.add(form, bindingResult, locale);
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_OPERATOR')")
  @RequestMapping(value = "/pipeConf/edit/{id}", method = RequestMethod.POST)
  public ResponseEntity edit(@PathVariable("id") Integer id, @Valid PipeConf form, BindingResult bindingResult,
    Locale locale) throws RestException {
    return super.edit(id, form, bindingResult, locale);
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_OPERATOR')")
  @RequestMapping(value = "/pipeConf/retire/{id}")
  public ResponseEntity retire(@PathVariable("id") Integer id) {
    return super.retire(id);
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_OPERATOR')")
  @RequestMapping(value = "/pipeConf/delete/{id}")
  public ResponseEntity delete(@PathVariable("id") Integer id) throws ObjectInUseException {
    return super.delete(id);
  }

  @Override
  @RequestMapping(value = "/pipeConf/count")
  @PreAuthorize("hasRole('DEFKOI_USER')")
  public Long count() {
    return super.count();
  }

  @Override
  @RequestMapping(value = "/pipeConf/modTime")
  @PreAuthorize("hasRole('DEFKOI_USER')")
  public Date modTime() {
    return super.modTime();
  }

  @Override
  @RequestMapping(value = "/pipeConf/metadata")
  @PreAuthorize("hasRole('DEFKOI_USER')")
  public Map<String, Object> metadata(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date lastSync) {
    return super.metadata(lastSync);
  }

}

