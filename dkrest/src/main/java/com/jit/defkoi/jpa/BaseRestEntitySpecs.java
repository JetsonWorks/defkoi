package com.jit.defkoi.jpa;

import org.springframework.data.jpa.domain.Specification;

import java.util.Date;

public class BaseRestEntitySpecs {

  public static Specification<BaseRestEntity> isActive() {
    return Specification.where(notRetired());
  }

  public static Specification<BaseRestEntity> notRetired() {
    return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("retireTime"), new Date());
  }

  public static Specification<BaseRestEntity> nameContains(String text) {
    return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + text.toLowerCase() + "%");
  }

}
