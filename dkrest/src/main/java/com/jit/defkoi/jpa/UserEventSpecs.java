package com.jit.defkoi.jpa;

import org.springframework.data.jpa.domain.Specification;

import java.util.Date;

public class UserEventSpecs {

  public static Specification<UserEvent> since(Date since) {
    return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<Date>get("timeStamp"), since);
  }

  public static Specification<UserEvent> valueContains(String text) {
    return (root, query, cb) -> cb.like(cb.lower(root.get("value")), "%" + text.toLowerCase() + "%");
  }

}
