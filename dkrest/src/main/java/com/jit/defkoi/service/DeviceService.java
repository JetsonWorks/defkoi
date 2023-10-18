package com.jit.defkoi.service;

import com.jit.defkoi.jpa.CapabilityRepository;
import com.jit.defkoi.jpa.Device;
import com.jit.defkoi.jpa.DeviceRepository;
import com.jit.defkoi.jpa.DeviceSpecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class DeviceService extends BaseRestEntityService<Device, DeviceRepository> {

  private static final Hashtable<DeviceApi, Hashtable<Boolean, String>> efNameByApiAccel = new Hashtable<>();

  static {
    Hashtable<Boolean, String> efNames = new Hashtable<>();
    efNames.put(false, "v4l2src");
    efNames.put(true, "nvv4l2camerasrc");
    efNameByApiAccel.put(DeviceApi.v4l2, efNames);
    efNames = new Hashtable<>();
    efNames.put(true, "nvarguscamerasrc");
    efNameByApiAccel.put(DeviceApi.csi, efNames);
  }

  @Autowired
  protected CapabilityRepository capRepo;

  /**
   * Returns the name of the appropriate ElementFactory (if any) for the specified device API with or without
   * acceleration. For pipeline creation convenience, this will return null if the specified API is invalid or there is
   * no matching ElementFactory.
   * @param api         such as "v4l2" or "argus"
   * @param accelerated
   * @return the ElementFactory name
   */
  public static String getEfNameByApiAccel(DeviceApi api, boolean accelerated) {
    try {
      if(!efNameByApiAccel.keySet().contains(api))
        throw new IllegalArgumentException("API " + api + " is invalid");
      String efName = efNameByApiAccel.get(api).get(accelerated);
      if(efName == null)
        throw new NoSuchElementException(
          String.format("There is no known ElementFactory for API %s %s acceleration", api,
            (accelerated ? "with" : "without")));
      return efName;
    } catch(NullPointerException npe) {
      return null;
    }
  }

  public DeviceService(@Autowired DeviceRepository repo) {
    super(repo);
  }

  public Device findByCardAndBusInfo(String deviceCard, String deviceBusInfo) {
    return repo.findByDeviceCardAndDeviceBusInfo(deviceCard, deviceBusInfo);
  }

  public Set<Device> findActiveByApi(DeviceApi api) {
    return repo.findByDeviceApiAndRetireTimeGreaterThan(api.name(), new Date());
  }

  public Device findByApiAndDevicePath(DeviceApi api, String devicePath) {
    return repo.findByDeviceApiAndDevicePath(api.name(), devicePath);
  }

  @Transactional
  @Modifying
  protected void checkUniqueness(Device form, @Nullable Device entity)
    throws NonUniqueObjectException, EmptyKeyException {
    boolean uniqueCheck = false;

    if(entity == null)
      entity = new Device();

    String deviceCard = entity.getDeviceCard();
    if(form.getDeviceCard() != null && !form.getDeviceCard().equals(entity.getDeviceCard())) {
      if(!StringUtils.hasLength(form.getDeviceCard()))
        throw new EmptyKeyException("A Device must have a deviceCard");
      deviceCard = form.getDeviceCard();
      uniqueCheck = true;
    }

    String deviceBusInfo = entity.getDeviceBusInfo();
    if(form.getDeviceBusInfo() != null && !form.getDeviceBusInfo().equals(entity.getDeviceBusInfo())) {
      if(!StringUtils.hasLength(form.getDeviceBusInfo()))
        throw new EmptyKeyException("A Device must have a deviceBusInfo");
      deviceBusInfo = form.getDeviceBusInfo();
      uniqueCheck = true;
    }

    if(uniqueCheck) {
      Device test = repo.findByDeviceCardAndDeviceBusInfo(deviceCard, deviceBusInfo);
      if(test != null && !test.getId().equals(entity.getId()))
        throw new NonUniqueObjectException(entity.toString());
    }
    // only set the changed values
    if(form.getDeviceCard() != null)
      entity.setDeviceCard(deviceCard);
    if(form.getDeviceBusInfo() != null)
      entity.setDeviceBusInfo(deviceCard);
  }

  @Override
  public Device doCreate(Device form) {
    return new Device();
  }

  @Override
  protected Device doSave(Device entity, Device form) {
    if(form.getName() != null)
      entity.setName(form.getName());
    if(form.getEnabled() != null) {
      boolean enabled = form.getEnabled();
      entity.setEnabled(enabled);
      if(!enabled)
        entity.getPipeConf().setEnabled(enabled);
    }
    if(form.getDisplayName() != null)
      entity.setDisplayName(form.getDisplayName());
    if(form.getDeviceClass() != null)
      entity.setDeviceClass(form.getDeviceClass());
    if(form.getDevicePath() != null)
      entity.setDevicePath(form.getDevicePath());

    if(form.getDeviceApi() != null)
      entity.setDeviceApi(form.getDeviceApi());
    if(form.getDeviceDriver() != null)
      entity.setDeviceDriver(form.getDeviceDriver());
    if(form.getDeviceVersion() != null)
      entity.setDeviceVersion(form.getDeviceVersion());
    return save(entity);
  }

  public Device mergeProbedIntoExisting(Device existing, Device probed) {
    probed.getCaps().removeAll(existing.getCaps());
    probed.getCaps().forEach(cap -> {
      cap.activate();
      cap.setDevice(existing);
      existing.getCaps().add(capRepo.save(cap));
    });
    existing.activate();
    return save(existing);
  }

  @Override
  protected Specification containsFilterText(Device form) {
    return Specification.where(DeviceSpecs.extendedContains(form.getFilterText()));
  }

}
