package com.jit.defkoi;

import ai.djl.Application;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.repository.Artifact;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import com.jit.defkoi.service.pref.Config;
import com.jit.defkoi.service.pref.PreferenceService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

/* Shared inference service for DJL.
 * Ref: href="https://github.com/deepjavalibrary/djl/blob/master/examples/docs/instance_segmentation.md">doc</a>
 * Note: DJL seems to be touchy about the thread used to create the Criteria and Model.
 * It seems like you need to do this from the main thread.
 * If you don't use the right one, you'll get nice descriptive errors like:

2022-07-23 07:28:10,588 ERROR [GstBus] com.jit.defkoi.Detection - com.jit.defkoi.DefKoiException: Error initializing model or predictor: ai.djl.repository.zoo.ModelNotFoundException: No matching model with specified Input/Output type found.
com.jit.defkoi.DefKoiException: Error initializing model or predictor: ai.djl.repository.zoo.ModelNotFoundException: No matching model with specified Input/Output type found.
	at com.jit.defkoi.Detector.init(Detector.java:95)
	at com.jit.defkoi.Detector.predict(Detector.java:101)
	at com.jit.defkoi.Detection.newSample(Detection.java:51)
	at org.freedesktop.gstreamer.elements.AppSink$2.callback(AppSink.java:232)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:566)
	at com.sun.jna.CallbackReference$DefaultCallbackProxy.invokeCallback(CallbackReference.java:585)
	at com.sun.jna.CallbackReference$DefaultCallbackProxy.callback(CallbackReference.java:616)
Caused by: ai.djl.repository.zoo.ModelNotFoundException: No matching model with specified Input/Output type found.
	at ai.djl.repository.zoo.Criteria.loadModel(Criteria.java:178)
	at com.jit.defkoi.Detector.init(Detector.java:92)
	... 9 common frames omitted
Caused by: ai.djl.repository.zoo.ModelNotFoundException: No matching default translator found. The valid input and output classes are:
	(ai.djl.ndarray.NDList, ai.djl.ndarray.NDList)

	at ai.djl.repository.zoo.BaseModelLoader.loadModel(BaseModelLoader.java:90)
	at ai.djl.repository.zoo.Criteria.loadModel(Criteria.java:166)
	... 10 common frames omitted

 */
public class Detector {

  private static final Logger logger = LoggerFactory.getLogger(Detector.class);

  private PreferenceService prefService;
  private Config config;

  private ZooModel<Image, DetectedObjects> model;
  private Predictor<Image, DetectedObjects> predictor;

  public Detector(PreferenceService prefService) {
    this.prefService = prefService;
  }

  public Detector init() throws DetectionInitException {
    config = prefService.getConfig();
    try {
      // @formatter:off
      Criteria.Builder<Image, DetectedObjects> builder =
        Criteria.builder()
          .optApplication(Application.CV.OBJECT_DETECTION)
          .setTypes(Image.class, DetectedObjects.class)
        ;
      // @formatter:on
      if(config.getEngine() != null) {
        builder.optEngine(config.getEngine());
        logger.debug("Using specified detectEngine: " + config.getEngine());
      }
      if(config.getArtifactId() != null) {
        builder.optArtifactId(config.getArtifactId());
        logger.debug("Using specified artifactId: " + config.getArtifactId());
      }
      if(config.getBackbone() != null) {
        builder.optFilter("backbone", config.getBackbone());
        logger.debug("Using specified backbone: " + config.getBackbone());
      }
      if(config.getFlavor() != null) {
        builder.optFilter("flavor", config.getFlavor());
        logger.debug("Using specified flavor: " + config.getFlavor());
      }
      if(config.getDataset() != null) {
        builder.optFilter("dataset", config.getDataset());
        logger.debug("Using specified dataset: " + config.getDataset());
      }
      if(config.getArgThreshold() != null) {
        builder.optArgument("threshold", config.getArgThreshold());
        logger.debug("Using specified threshold: " + config.getArgThreshold());
      }
      Criteria<Image, DetectedObjects> criteria = builder.build();
      logger.debug("models:");
      Map<Application, List<Artifact>> models = ModelZoo.listModels();
      for(Application app : models.keySet())
        logger.debug(models.get(app).toString());
      logger.debug("models meeting criteria:");
      models = ModelZoo.listModels(criteria);
      for(Application app : models.keySet())
        logger.debug(models.get(app).toString());

      model = criteria.loadModel();
      predictor = model.newPredictor();
    } catch(Exception e) {
      throw new DetectionInitException("Error initializing model or predictor: " + e, e);
    }
    return this;
  }

  public void reinit() throws DetectionInitException {
    Config config = prefService.getConfig();
    if(!this.config.detectEquals(config))
      init();
    else // otherwise, we just need to refresh our config, as init() would do
      this.config = config;
  }

  public CvImageDetection predict(BufferedImage image) throws DetectionInitException, PredictionException {
    if(predictor == null)
      init();
    Image img = ImageFactory.getInstance().fromImage(image);
    try {
      DetectedObjects detection = predictor.predict(img);
      img.drawBoundingBoxes(detection);
      return new CvImageDetection(img, detection);
    } catch(TranslateException e) {
      throw new PredictionException("Prediction error: " + e, e);
    }
  }

  @Data
  public class CvImageDetection {
    private final Image image;
    private final DetectedObjects objects;
  }

}
