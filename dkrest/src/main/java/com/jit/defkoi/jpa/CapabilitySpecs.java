package com.jit.defkoi.jpa;

import org.springframework.data.jpa.domain.Specification;

public class CapabilitySpecs extends BaseRestEntitySpecs {

  public static Specification<Capability> contains(String text) {
    Specification specs = BaseRestEntitySpecs.nameContains(text);
    return specs.or(formatContains(text)).or(deviceDisplayNameContains(text));
  }

  public static Specification<Capability> extendedContains(String text) {
    return contains(text);
  }

  public static Specification<Capability> formatContains(String text) {
    return (root, query, cb) -> cb.like(cb.lower(root.get("format")), "%" + text.toLowerCase() + "%");
  }

  public static Specification<Capability> deviceDisplayNameContains(String text) {
    return (root, query, cb) -> cb.like(cb.lower(root.get("device").get("displayName")),
      "%" + text.toLowerCase() + "%");
  }

  public static Specification<Capability> widthLe(Integer max) {
    return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("width"), max);
  }

  public static Specification<Capability> heightLe(Integer max) {
    return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("height"), max);
  }

  public static Specification<Capability> framerateGe(Double min) {
    return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("framerateMin"), min);
  }

}
