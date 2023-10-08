package com.jit.defkoi.rest;

import com.jit.defkoi.DefKoiException;
import com.jit.defkoi.Sentry;
import com.jit.defkoi.Utils;
import com.jit.defkoi.jpa.Stats;
import com.jit.defkoi.service.PipeConfService;
import com.jit.defkoi.service.pref.Config;
import com.jit.defkoi.service.pref.PreferenceService;
import com.jit.defkoi.service.pref.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class MainController {

  private static final Logger logger = LoggerFactory.getLogger(MainController.class);

  @Autowired
  private Sentry sentry;

  @Autowired
  @Qualifier("preferenceService")
  private PreferenceService prefService;
  @Autowired
  private PipeConfService pipeConfService;

  @Autowired
  private DeviceController deviceController;
  @Autowired
  private CapabilityController capabilityController;
  @Autowired
  private PipeConfController pipeConfController;

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @RequestMapping(value = "/prefs", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public Preferences getPrefs() {
    return prefService.getPreferences();
  }

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @RequestMapping(value = "/prefs", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public Preferences patchPrefs(@RequestBody Preferences update) {
    Preferences prefs = prefService.getPreferences();
    prefs.patch(update);
    return prefService.savePreferences(prefs);
  }

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @RequestMapping(value = "/metadata", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Map<String, Object>> metadata(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date lastSync) {
    Map<String, Map<String, Object>> map = new LinkedHashMap<>();
    map.put("device", deviceController.metadata(lastSync));
    map.put("capability", capabilityController.metadata(lastSync));
    map.put("pipeConf", pipeConfController.metadata(lastSync));
    return map;
  }

  /** Rechecks devices and recreates pipelines. */
  @PreAuthorize("hasRole('DEFKOI_OPERATOR')")
  @RequestMapping(value = "/reinit")
  public ResponseEntity reinit() {
    if(prefService.getConfig().isDebug())
      Utils.dumpDevices();
    try {
      sentry.boot().up();
    } catch(Exception e) {
      logger.error(e.toString(), e);
      return new ResponseEntity(e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return new ResponseEntity(HttpStatus.OK);
  }

  /** Recreates pipelines. */
  @PreAuthorize("hasRole('DEFKOI_OPERATOR')")
  @RequestMapping(value = "/activate")
  public ResponseEntity activate() throws DefKoiException {
    sentry.up();
    return new ResponseEntity(HttpStatus.OK);
  }

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @RequestMapping(value = "/config", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public Config getConfig() {
    return prefService.getConfig();
  }

  @PreAuthorize("hasRole('DEFKOI_OPERATOR')")
  @RequestMapping(value = "/config", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public Config patchConfig(@RequestBody Config update) throws DefKoiException {
    Config config = prefService.getConfig();
    prefService.saveConfig(config.patch(update));
    sentry.up();
    return config;
  }

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @RequestMapping(value = "/stats", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, List<Stats>> stats() {
    return sentry.getStats();
  }

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @RequestMapping(value = "/availableResolutions", method = RequestMethod.GET,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public List<String> availableResolutions() {
    return new ArrayList<>(pipeConfService.getAvailableResolutions());
  }

  @PreAuthorize("hasRole('DEFKOI_OPERATOR')")
  @RequestMapping(value = "/debugToDot")
  public ResponseEntity debugToDot() {
    sentry.debugToDot();
    return new ResponseEntity(HttpStatus.OK);
  }

}

