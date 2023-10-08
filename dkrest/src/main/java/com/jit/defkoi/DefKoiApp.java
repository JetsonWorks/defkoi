package com.jit.defkoi;

import com.jit.defkoi.config.DefaultConfig;
import com.jit.defkoi.service.pref.Config;
import com.jit.defkoi.service.pref.PreferenceService;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Version;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DefKoiApp {

  public static void main(String[] args) {
    SpringApplication.run(DefKoiApp.class, args);
  }

  @Bean
  public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
    return args -> {
      Utils.configurePaths();
      Gst.init(Version.BASELINE, "DefKoiDetection", new String[0]);

      PreferenceService prefService = ctx.getBean(PreferenceService.class);
      DefaultConfig defaultConfig = ctx.getBean(DefaultConfig.class);
      Config config = prefService.saveConfig(prefService.getConfig(defaultConfig));
      if(config.isDebug())
        Utils.dumpDevices();
      ctx.getBean(Sentry.class).boot().up();
    };
  }

}
