package com.jit.defkoi.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jit.defkoi.jpa.pref.Preference;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class User implements Serializable {

  @Id
  @EqualsAndHashCode.Include
  @ToString.Include
  @Column(length = 256)
  protected String name;

  @EqualsAndHashCode.Include
  @ToString.Include
  @Column(name = "first_name", length = 50)
  private String firstName;

  @EqualsAndHashCode.Include
  @ToString.Include
  @Column(name = "last_name", length = 50)
  private String lastName;

  @EqualsAndHashCode.Include
  @ToString.Include
  @Column(name = "email_address", length = 255)
  private String emailAddress;

  @EqualsAndHashCode.Include
  @ToString.Include
  @Column(length = 4000)
  private String notes;

  @JsonIgnore
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
  @Fetch(FetchMode.JOIN)
  private Set<Preference> preferences = new HashSet<>();

  @JsonIgnore
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
  private Set<UserEvent> userEvents = new HashSet<>();

  public User(String name) {
    this.name = name;
  }

  public int compareTo(User o) {
    return getName().compareTo(o.getName());
  }

  public void removeFromReferences() {
  }

}
