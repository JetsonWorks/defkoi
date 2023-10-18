package com.jit.defkoi.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jit.defkoi.audit.ObjectMapperFactory;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Table(name = "user_event")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
@NoArgsConstructor
public class UserEvent {

  private static final ObjectMapper mapper = ObjectMapperFactory.objectMapper();

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;

  @NotNull
  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @NotNull
  @Column(name = "time_stamp", nullable = false)
  private Date timeStamp;

  @NotEmpty
  @Column(name = "entity_type", length = 45, nullable = false)
  private String entityType;

  @NotEmpty
  @Column(name = "event_type", length = 45, nullable = false)
  private String eventType;

  @Column(name = "entity_id")
  private Integer entityId;

  @NotEmpty
  @Column(name = "event_value", length = 51200, nullable = false)
  private String value;

  public UserEvent(User user, String eventType, Object entity) {
    this.user = user;
    this.eventType = eventType;
    this.entityType = entity.getClass().getSimpleName();
    this.timeStamp = new Date();

    entityId = 0;
    if(BaseRestEntity.class.isAssignableFrom(entity.getClass()))
      entityId = ((BaseRestEntity)entity).getId();

    String objStr = entity instanceof BaseRestEntity && ((BaseRestEntity)entity).getName() != null ?
      ((BaseRestEntity)entity).getName() :
      entity.toString();
    this.value = String.format("%s %s %s", eventType, entityType, objStr);
  }

  public static UserEvent edited(User user, Object entity) {
    return new UserEvent(user, "edited", entity);
  }

  public static UserEvent saved(User user, Object entity) {
    return new UserEvent(user, "saved", entity);
  }

  public static UserEvent deleted(User user, Object entity) {
    return new UserEvent(user, "deleted", entity);
  }

  public static UserEvent retired(User user, Object entity) {
    return new UserEvent(user, "retired", entity);
  }

  public static UserEvent migrated(User user, Object entity) {
    return new UserEvent(user, "migrated", entity);
  }

  public static UserEvent purged(User user, Object entity) {
    return new UserEvent(user, "purged", entity);
  }

}
