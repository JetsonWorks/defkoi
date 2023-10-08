package com.jit.defkoi.jpa.pref;

import com.jit.defkoi.audit.EntityEventListener;
import com.jit.defkoi.jpa.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "preference",
  uniqueConstraints = @UniqueConstraint(name = "unique_preference", columnNames = { "name", "user_id" }))
@Data
@NoArgsConstructor
@Cacheable
@EntityListeners(EntityEventListener.class)
@DiscriminatorColumn(name = "data_type", discriminatorType = DiscriminatorType.STRING)
public class Preference<T extends Serializable> {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  protected Integer id;

  @Transient
  protected IPrefKey prefKey;

  @EqualsAndHashCode.Include
  protected String name;

  @Column(name = "string_data")
  protected String stringData;

  @Column(name = "numeric_data")
  protected Number numericData;

  @Column(name = "boolean_data")
  protected Boolean booleanData;

  @EqualsAndHashCode.Include
  @ManyToOne
  @JoinColumn(name = "user_id")
  protected User user;

  public Preference(IPrefKey prefKey) {
    this.prefKey = prefKey;
    this.name = prefKey.getName();
  }

  public Preference<T> user(User user) {
    setUser(user);
    return this;
  }

  public T value() {
    if(prefKey.getDataType() == PrefDataType.STRING)
      return (T)stringData;
    else if(prefKey.getDataType() == PrefDataType.NUMERIC)
      return (T)numericData;
    else if(prefKey.getDataType() == PrefDataType.BOOLEAN)
      return (T)booleanData;
    else
      throw new IllegalArgumentException("PrefDataType " + prefKey.getDataType().name() + " is not supported");
  }

  public Preference<T> value(T value) {
    if(prefKey.getDataType() == PrefDataType.STRING)
      setStringData((String)value);
    else if(prefKey.getDataType() == PrefDataType.NUMERIC) {
      setNumericData(value instanceof String ? (T)Double.valueOf((String)value) : (T)value);
    } else if(prefKey.getDataType() == PrefDataType.BOOLEAN)
      setBooleanData(value instanceof String ? (T)Boolean.valueOf((String)value) : (T)value);
    else
      throw new IllegalArgumentException("PrefDataType " + prefKey.getDataType().name() + " is not supported");
    return this;
  }

  public void setNumericData(T value) {
    numericData = value instanceof String ? Double.parseDouble((String)value) : (Number)value;
  }

  public void setBooleanData(T value) {
    booleanData = value instanceof String ? Boolean.parseBoolean((String)value) : (Boolean)value;
  }

  @Override
  public String toString() {
    return String.format("Preference{name='%s', value='%s'}", prefKey.getName(), value());
  }

}
