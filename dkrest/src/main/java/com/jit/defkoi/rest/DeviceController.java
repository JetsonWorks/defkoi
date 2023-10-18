package com.jit.defkoi.rest;

import com.jit.defkoi.jpa.Device;
import com.jit.defkoi.jpa.DeviceRepository;
import com.jit.defkoi.service.DeviceApi;
import com.jit.defkoi.service.DeviceService;
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
import java.util.*;
import java.util.stream.Collectors;

import static com.jit.defkoi.service.pref.PreferenceKey.showRetiredDevices;

@RestController
public class DeviceController extends BaseRestEntityController<Device, DeviceRepository, DeviceService> {

  public DeviceController(@Autowired DeviceService service) {
    super(service, "device", showRetiredDevices, "NotEmpty.Device.name",
      "com.jit.defkoi.validation.NonUniqueDevice.name");
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_USER')")
  @RequestMapping(value = "/device/search")
  public List<Device> search(Device form) {
    return super.search(form);
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_OPERATOR')")
  @RequestMapping(value = "/device/add", method = RequestMethod.POST)
  public ResponseEntity add(@Valid Device form, BindingResult bindingResult, Locale locale) throws RestException {
    return super.add(form, bindingResult, locale);
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_OPERATOR')")
  @RequestMapping(value = "/device/edit/{id}", method = RequestMethod.POST)
  public ResponseEntity edit(@PathVariable("id") Integer id, @Valid Device form, BindingResult bindingResult,
    Locale locale) throws RestException {
    return super.edit(id, form, bindingResult, locale);
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_OPERATOR')")
  @RequestMapping(value = "/device/retire/{id}")
  public ResponseEntity retire(@PathVariable("id") Integer id) {
    return super.retire(id);
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_OPERATOR')")
  @RequestMapping(value = "/device/delete/{id}")
  public ResponseEntity delete(@PathVariable("id") Integer id) throws ObjectInUseException {
    return super.delete(id);
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_USER')")
  @RequestMapping(value = "/device/count")
  public Long count() {
    return super.count();
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_USER')")
  @RequestMapping(value = "/device/modTime")
  public Date modTime() {
    return super.modTime();
  }

  @Override
  @PreAuthorize("hasRole('DEFKOI_USER')")
  @RequestMapping(value = "/device/metadata")
  public Map<String, Object> metadata(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date lastSync) {
    return super.metadata(lastSync);
  }

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @RequestMapping(value = "/device/apis")
  public Set<String> getDeviceApis() {
    return Arrays.stream(DeviceApi.values()).map(DeviceApi::name).collect(Collectors.toSet());
  }

}

