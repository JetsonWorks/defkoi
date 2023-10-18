package com.jit.defkoi.jpa;

import com.jit.defkoi.pipeline.PipelineStats;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "stats")
@Data
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class Stats {

  private static final Logger logger = LoggerFactory.getLogger(Stats.class);

  @ToString.Include
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  protected Integer id;

  @ToString.Include
  @EqualsAndHashCode.Include
  @Column(length = 256)
  protected String observed;

  @NotNull
  @Column(name = "time_stamp", nullable = false)
  private Date timeStamp;

  @Column
  private double seconds;

  @Column
  private double motAcceptRate;

  @Column
  private double motConvRate;

  @Column
  private double motConvCost;

  @Column
  private double motProcRate;

  @Column
  private double motProcCost;

  @Column
  private double motFramesDetected;

  @Column
  private double objAcceptRate;

  @Column
  private double objConvRate;

  @Column
  private double objConvCost;

  @Column
  private double objProcRate;

  @Column
  private double objProcCost;

  @Column
  private double objFramesDetected;

  @Column
  private double objPubAcceptRate;

  @Column
  private double objPubProcRate;

  @Column
  private double objPubProcCost;

  public Stats(PipelineStats pipelineStats) {
    timeStamp = new Date();
    seconds = pipelineStats.getDefaultExpirySeconds();
    observed = pipelineStats.getObserved();

    try {
      motAcceptRate = pipelineStats.getMotAccept().getRate();
      motConvRate = pipelineStats.getMotConversion().getRate();
      motConvCost = pipelineStats.getMotConversion().getAverageTime();
      motProcRate = pipelineStats.getMotProcess().getRate();
      motProcCost = pipelineStats.getMotProcess().getAverageTime();
      motFramesDetected = pipelineStats.getMotFramesDetected().getRate();

      objAcceptRate = pipelineStats.getObjAccept().getRate();
      objConvRate = pipelineStats.getObjConversion().getRate();
      objConvCost = pipelineStats.getObjConversion().getAverageTime();
      objProcRate = pipelineStats.getObjProcess().getRate();
      objProcCost = pipelineStats.getObjProcess().getAverageTime();
      objFramesDetected = pipelineStats.getObjFramesDetected().getRate();

      objPubAcceptRate = pipelineStats.getObjPubAccept().getRate();
      objPubProcRate = pipelineStats.getObjPubProcess().getRate();
      objPubProcCost = pipelineStats.getObjPubProcess().getAverageTime();

    } catch(Exception e) {
      logger.error(e.toString());
    }
  }

  @Override
  public boolean equals(Object o) {
    if(this == o)
      return true;
    if(!(o instanceof Stats))
      return false;
    if(!super.equals(o))
      return false;
    Stats stats = (Stats)o;
    return getId() == stats.getId();
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getId());
  }

}
