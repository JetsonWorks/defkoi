package com.jit.defkoi.jpa;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table(name = "watch")
@PrimaryKeyJoinColumn(name = "id")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class Watch extends BaseRestEntity {

  @EqualsAndHashCode.Include
  @ToString.Include
  @Column(name = "confidence")
  private Double confidence;

  @EqualsAndHashCode.Include
  @ToString.Include
  @Column(name = "frame_area")
  private Double frameArea;

}
