package com.jit.defkoi;

import com.jit.defkoi.config.DefaultConfig;
import com.jit.defkoi.jpa.Device;
import com.jit.defkoi.jpa.PipeConf;
import com.jit.defkoi.jpa.Stats;
import com.jit.defkoi.jpa.StatsRepository;
import com.jit.defkoi.pipeline.CameraPipeline;
import com.jit.defkoi.pipeline.PipelineContext;
import com.jit.defkoi.service.CapabilityService;
import com.jit.defkoi.service.DeviceApi;
import com.jit.defkoi.service.DeviceService;
import com.jit.defkoi.service.PipeConfService;
import com.jit.defkoi.service.pref.Config;
import com.jit.defkoi.service.pref.PreferenceService;
import com.sun.security.auth.UnixPrincipal;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.jit.defkoi.SentryRuntime.Status.booted;
import static com.jit.defkoi.pipeline.PipelineContext.rootName;

@Service
public class Sentry {

  private static final Logger logger = LoggerFactory.getLogger(Sentry.class);
  protected static PreAuthenticatedAuthenticationToken serviceToken;

  @Getter
  private SentryRuntime runtime = new SentryRuntime(logger);
  @Getter
  private static Detector detector;
  private static Pattern numPat = Pattern.compile("/dev/video([0-9]+)");

  private Config config;
  private DefaultConfig defaultConfig;
  private PreferenceService prefService;
  private DeviceService deviceService;
  private PipeConfService pipeConfService;
  private CapabilityService capService;
  private StatsRepository statsRepo;

  @Getter
  private static Hashtable<String, PipelineContext> contexts = new Hashtable<>();
  private Integer statsPeriod;
  private Timer statsTimer;

  public Sentry(@Autowired DefaultConfig defaultConfig, @Autowired PreferenceService prefService,
    @Autowired DeviceService deviceService, @Autowired PipeConfService pipeConfService,
    @Autowired CapabilityService capService, @Autowired StatsRepository statsRepository) {
    Collection<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_DEFKOI_ADMIN"));
    authorities.add(new SimpleGrantedAuthority("ROLE_DEFKOI_OPERATOR"));
    authorities.add(new SimpleGrantedAuthority("ROLE_DEFKOI_USER"));
    serviceToken =
      new PreAuthenticatedAuthenticationToken(new UnixPrincipal(System.getProperty("user.name")), null, authorities);
    SecurityContextHolder.getContext().setAuthentication(serviceToken);
    this.defaultConfig = defaultConfig;
    this.prefService = prefService;
    this.deviceService = deviceService;
    this.pipeConfService = pipeConfService;
    this.capService = capService;
    this.statsRepo = statsRepository;
    runtime.start();
  }

  public Sentry boot() throws DefKoiException {
    runtime.booting();
    try {
      config = prefService.saveConfig(prefService.getConfig(defaultConfig));
    } catch(InvocationTargetException | IllegalAccessException e) {
      throw new DefKoiException("Exception loading config: " + e.getMessage(), e);
    }
    try {
      if(config.isObjDetectEnabled() && detector == null)
        detector = new Detector(prefService).init();
    } catch(DetectionInitException e) {
      down();
      throw e;
    }

    config.setNvCapable(Utils.detectNvidiaGst());
    config.setDockerized(Utils.detectDockerContainer());
    prefService.saveConfig(config);
    logger.info("Probing V4L2 devices");
    updateDevices(Utils.detectV4l2Devices(), DeviceApi.v4l2);
    logger.info("Probing CSI devices");
    updateDevices(probeCsiDevices(), DeviceApi.csi);
    logger.info("Updating pipeline configurations");
    Set<PipeConf> pipeConfs = updatePipeConfs();
    runtime.booted();
    return this;
  }

  public void up() throws DefKoiException {
    if(runtime.equals(booted))
      runtime.starting();
    else
      runtime.reinitialize();
    config = prefService.getConfig();
    configStatsCollection();
    // in case detection config has changed
    try {
      if(config.isObjDetectEnabled()) {
        if(detector == null)
          detector = new Detector(prefService).init();
        else
          detector.reinit();
      }
    } catch(DetectionInitException e) {
      down();
      throw e;
    }

    // kill all CameraPipelines that are not still active and enabled
    Collection<PipelineContext> killList = new ArrayList<>(contexts.values());
    killList.forEach(x -> {
      PipeConf pipeConf = pipeConfService.get(x.getPipeConf().getId());
      if(!pipeConf.isActive() || !Boolean.TRUE.equals(pipeConf.getEnabled()))
        x.killCameraPipeline();
    });

    Set<PipeConf> active = pipeConfService.findActive();
    if(active.isEmpty())
      return;
    logger.info("Creating/updating camera pipeline(s)");
    DefKoiException je = null;
    for(PipeConf pipeConf : active) {
      if(!Boolean.TRUE.equals(pipeConf.getEnabled()))
        continue;

      // if exists then reinit(), else create
      PipelineContext context = contexts.get(rootName(pipeConf));
      if(context != null) {
        context.reinit(pipeConf);
        continue;
      }
      context = new PipelineContext(pipeConf, serviceToken);
      contexts.put(rootName(pipeConf), context);
      CameraPipeline cp = new CameraPipeline(prefService, pipeConfService, context);
      try {
        new Thread(new ThreadGroup(cp.getName()), cp).start();
        logger.info("Created pipeline " + cp.getName());
      } catch(Exception e) {
        je = new DefKoiException(String.format("Exception in pipeline %s: %s", cp.getName(), e.getMessage()), e);
        context.stop();
      }
    }
    runtime.run();
    runtime.running();
    if(je != null)
      throw je;
  }

  public void down() {
    runtime.stop();
    runtime.stopping();
    logger.info("Stopping " + contexts.size() + " camera pipeline(s)");
    contexts.values().forEach(PipelineContext::stop);
    runtime.stopped();
  }

  protected List<Device> updateDevices(List<Device> probedDevs, DeviceApi api) {
    List<Device> active = new ArrayList<>();
    Set<Device> retire = deviceService.findActiveByApi(api);
    for(Device probed : probedDevs) {
      Device existing = api.equals(DeviceApi.v4l2) ?
        deviceService.findByCardAndBusInfo(probed.getDeviceCard(), probed.getDeviceBusInfo()) :
        deviceService.findByApiAndDevicePath(api, probed.getDevicePath());
      if(existing == null) {
        probed.setEnabled(true);
        existing = deviceService.save(probed);
      } else {
        existing.setName(probed.getName());
        existing.setDevicePath(probed.getDevicePath());
        existing = deviceService.mergeProbedIntoExisting(existing, probed);
      }
      existing.activate();
      active.add(deviceService.save(existing));
    }

    // retire those devices that were not detected
    retire.removeAll(active);
    retire.forEach(Device::retire);
    deviceService.saveAll(retire);
    return active;
  }

  /**
   * Ensures a PipeConf has been created for each Device. When creating a new PipeConf, set the resolution no higher
   * than specified in the config.
   */
  protected Set<PipeConf> updatePipeConfs() throws DefKoiException {
    Set<Device> devices = deviceService.findActive();
    Set<PipeConf> pipeConfs = pipeConfService.findActive();
    Set<PipeConf> active = new HashSet<>();
    if(pipeConfs.isEmpty() && devices.isEmpty())
      throw new DefKoiException("No camera sources defined and no devices detected");
    for(Device dev : devices) {
      PipeConf pipeConf = dev.getPipeConf();
      if(pipeConf == null) {
        pipeConf = pipeConfService.findByDevice(dev);
        if(pipeConf == null)
          pipeConf = pipeConfService.save(new PipeConf(dev));
        else {
          dev.setPipeConf(pipeConf);
          deviceService.save(dev);
        }
        // TODO: revise pipeline and/or code to work with V4L2 and NV GST pipeline elements, and then change default
        pipeConf.setNvEnabled(config.isNvCapable() && DeviceApi.csi.name().equals(pipeConf.getDevice().getDeviceApi()));
        pipeConf.setMotDetectEnabled(config.isObjDetectEnabled());
        pipeConf.setObjDetectEnabled(config.isObjDetectEnabled());
        pipeConf = pipeConfService.save(pipeConf);
      } else {
        pipeConf.setDevice(dev);
        if(!pipeConf.getName().equals(dev.getDevicePath()))
          pipeConf.setName(dev.getDevicePath());
      }
      if(dev.getCaps().isEmpty()) {
        dev.setEnabled(false);
        pipeConf.setEnabled(false);
        continue;
      }
      pipeConf.optimizeResolution(config);
      pipeConf.getCap().setPipeConf(pipeConf);
      capService.save(pipeConf.getCap());
      pipeConf.setEnabled(dev.getEnabled());
      active.add(pipeConf);
    }

    // retire pipeConfs not associated with a device
    pipeConfs.removeAll(active);
    pipeConfs.forEach(PipeConf::retire);
    pipeConfService.saveAll(pipeConfs);

    for(PipeConf pc : active) {
      pc.setRetireTime(pc.getDevice().getRetireTime());
      pc.setEnabled(pc.getDevice().getEnabled());
      pipeConfService.save(pc);
      deviceService.save(pc.getDevice());
    }
    return active;
  }

  /**
   * Looks for devices that may be CSI and attempts to probe their caps (since the NVidia CSI plugins do not come with a
   * DeviceProviderFactory). V4L2 devices (assuming USB) will be assigned device paths after any CSI devices. Once we
   * know the V4L2 device paths, antecedent paths are potentially assigned to CSI devices. Using nvarguscamerasrc will
   * print caps to stdout, so we attempt to run a simple pipeline via CLI gst-launch-1.0 and parse the output.
   */
  protected List<Device> probeCsiDevices() {
    TreeSet<String> devicePaths =
      deviceService.findActiveByApi(DeviceApi.v4l2).stream().map(Device::getDevicePath).filter(Objects::nonNull)
        .collect(Collectors.toCollection(TreeSet::new));
    List<Device> probed = new ArrayList<>();
    try {
      Matcher m = numPat.matcher(devicePaths.first());
      if(m.matches()) {
        int num = Integer.parseInt(m.group(1));
        for(int i = 0; i < num; i++) {
          try {
            Device d = new Device("/dev/video" + i, Utils.captureNvargusCaps(i));
            d.setName("csidevice" + i);
            d.setDisplayName("NV Argus " + i);
            d.setDeviceClass("Video/Source");
            d.setDeviceApi(DeviceApi.csi.name());
            d.setDeviceDriver("CSI");
            d.setDeviceCard("CSI");
            d.setDeviceBusInfo("" + i);
            probed.add(d);
          } catch(Exception e) {
            logger.debug("Exception while probing CSI devices: " + e, e);
          }
        }
      }
    } catch(NoSuchElementException e) {
    }
    return probed;
  }

  protected void configStatsCollection() {
    if(config.getStatsUpdatePeriod().equals(statsPeriod))
      return;
    if(statsTimer != null) {
      statsTimer.cancel();
      statsTimer = null;
    }
    statsPeriod = config.getStatsUpdatePeriod();
    if(statsPeriod > 0) {
      statsTimer = new Timer();
      statsTimer.scheduleAtFixedRate(new StatsTaker(), statsPeriod * 1000, statsPeriod * 1000);
    }
  }

  public Map<String, List<Stats>> getStats() {
    Map<String, List<Stats>> allStats = new HashMap<>();
    for(PipelineContext context : contexts.values())
      allStats.put(context.getRootName(), context.getStatsHistory().stream().collect(Collectors.toList()));
    return allStats;
  }

  protected void logStats() {
    if(!config.isLogStatsEnabled())
      return;
    for(PipelineContext context : contexts.values()) {
      statsRepo.save(new Stats(context.getPipelineStats()));
    }
  }

  private class StatsTaker extends TimerTask {
    public void run() {
      SecurityContextHolder.getContext().setAuthentication(Sentry.serviceToken);
      logStats();
    }
  }

  public void debugToDot() {
    for(PipelineContext context : contexts.values())
      context.debugToDotFile("ad-hoc." + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
  }

}
