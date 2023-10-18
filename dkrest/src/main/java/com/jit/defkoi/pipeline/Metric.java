package com.jit.defkoi.pipeline;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

@Data
@NoArgsConstructor
public class Metric {
  @Setter
  private static int defaultExpiry = 5000;
  private static int defaultScale = 2;

  private ConcurrentLinkedQueue<Datum> samples = new ConcurrentLinkedQueue<>();
  protected int expiry = defaultExpiry;
  protected int scale = defaultScale;
  protected Double threshold;
  protected Date sampleStart;

  public Metric(int scale, int expiry) {
    this.scale = scale;
    this.expiry = expiry;
  }

  public Metric(double threshold) {
    this.threshold = threshold;
  }

  public void reset() {
    samples.clear();
  }

  public void increment() {
    samples.add(new Datum(0));
  }

  public void update(Double value) {
    samples.add(new Datum(value));
  }

  public int getCount() {
    removeStale();
    return samples.size();
  }

  public double getAverageTime() {
    removeStale();
    if(samples.size() == 0)
      return 0;
    double total = 0;
    double count = 0;
    for(Object s : samples) {
      total += ((Datum)s).getValue();
      count++;
    }
    return BigDecimal.valueOf(total / count).setScale(scale, RoundingMode.HALF_UP).doubleValue();
  }

  public double getRate() {
    removeStale();
    if(samples.size() == 0)
      return 0;
    return BigDecimal.valueOf((double)samples.size() / (expiry / 1000)).setScale(scale, RoundingMode.HALF_UP)
      .doubleValue();
  }

  public void startClock() {
    sampleStart = new Date();
  }

  public void stopClock() {
    if(sampleStart == null)
      return;
    double value = new Date().getTime() - sampleStart.getTime();
    sampleStart = null;
    update(value);
  }

  protected void removeStale() {
    Date now = new Date();
    for(Iterator<Datum> it = samples.iterator(); it.hasNext(); ) {
      Datum d = it.next();
      if(now.getTime() - d.getTimeStamp().getTime() > expiry)
        it.remove();
      else
        break;
    }
  }

}
