package com.jit.defkoi.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jit.defkoi.audit.EntityEventListener;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Calendar;
import java.util.Date;

@Entity
@Table(name = "base_rest_entity")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@EntityListeners(EntityEventListener.class)
public class BaseRestEntity implements Comparable<BaseRestEntity> {

  protected static Date defaultRetireTime = new Date();

  static {
    Calendar term = Calendar.getInstance();
    term.set(2031, Calendar.JANUARY, 1, 0, 0, 0);
    defaultRetireTime.setTime(term.getTimeInMillis());
  }

  public static Date getDefaultRetireTime() {
    return defaultRetireTime;
  }

  public static boolean equalsDefaultRetireTime(Date date) {
    return Math.abs(date.getTime() - defaultRetireTime.getTime()) < 1000;
  }

  @ToString.Include
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  protected Integer id;

  @ToString.Include
  @EqualsAndHashCode.Include
  @Column(length = 256)
  protected String name;

  @NotNull
  @Column(name = "mod_time", columnDefinition = "timestamp with time zone not null", nullable = false)
  protected Date modTime = new Date();

  @NotNull
  @Column(name = "retire_time", columnDefinition = "timestamp with time zone not null default '2023-01-01'",
    nullable = false)
  protected Date retireTime = getDefaultRetireTime();

  @Transient
  protected String filterText;

  @Transient
  @JsonIgnore
  protected Boolean setActive;

  public static void touch(BaseRestEntity entity) {
    if(entity != null)
      entity.setModTime(new Date());
  }

  @Override
  public int compareTo(BaseRestEntity o) {
    return getName().compareTo(o.getName());
  }

  public boolean isActive() {
    return retireTime.after(new Date());
  }

  public void retire() {
    this.retireTime = new Date();
  }

  public void activate() {
    this.retireTime = defaultRetireTime;
  }

  public void removeFromReferences() {
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    BaseRestEntity cloned = (BaseRestEntity)super.clone();
    cloned.setId(null);
    cloned.setName("cloned " + cloned.getName());
    cloned.setModTime(new Date());
    return cloned;
  }

}
