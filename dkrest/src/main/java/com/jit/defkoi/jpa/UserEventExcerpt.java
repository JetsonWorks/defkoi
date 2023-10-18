package com.jit.defkoi.jpa;

import org.springframework.data.rest.core.config.Projection;

import java.util.Date;

@Projection(name = "excerpt", types = { UserEvent.class })
public interface UserEventExcerpt {

  Integer getId();

  User getUser();

  default String getUsername() {
    return getUser().getName();
  }

  Date getTimeStamp();

  String getEntityType();

  String getEventType();

  Integer getEntityId();

  String getValue();

}
