package com.jit.defkoi.jpa;

import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

public class DeviceSpecs extends BaseRestEntitySpecs {

  public static Specification<Device> contains(String text) {
    Specification specs = BaseRestEntitySpecs.nameContains(text);
    return specs.or(displayNameContains(text)).or(deviceClassContains(text)).or(devicePathContains(text))
      .or(deviceApiContains(text)).or(deviceDriverContains(text)).or(deviceCardContains(text));
  }

  public static Specification<Device> extendedContains(String text) {
    return contains(text).or(anyCapNameContains(text));
  }

  public static Specification<Device> displayNameContains(String text) {
    return (root, query, cb) -> cb.like(cb.lower(root.get("displayName")), "%" + text.toLowerCase() + "%");
  }

  public static Specification<Device> deviceClassContains(String text) {
    return (root, query, cb) -> cb.like(cb.lower(root.get("deviceClass")), "%" + text.toLowerCase() + "%");
  }

  public static Specification<Device> devicePathContains(String text) {
    return (root, query, cb) -> cb.like(cb.lower(root.get("devicePath")), "%" + text.toLowerCase() + "%");
  }

  public static Specification<Device> deviceApiContains(String text) {
    return (root, query, cb) -> cb.like(cb.lower(root.get("deviceApi")), "%" + text.toLowerCase() + "%");
  }

  public static Specification<Device> deviceDriverContains(String text) {
    return (root, query, cb) -> cb.like(cb.lower(root.get("deviceDriver")), "%" + text.toLowerCase() + "%");
  }

  public static Specification<Device> deviceCardContains(String text) {
    return (root, query, cb) -> cb.like(cb.lower(root.get("deviceCard")), "%" + text.toLowerCase() + "%");
  }

  public static Specification<Device> deviceVersionContains(String text) {
    return (root, query, cb) -> cb.like(cb.lower(root.get("deviceVersion")), "%" + text.toLowerCase() + "%");
  }

  public static Specification<Device> deviceBusInfoContains(String text) {
    return (root, query, cb) -> cb.like(cb.lower(root.get("deviceBusInfo")), "%" + text.toLowerCase() + "%");
  }

  public static Specification<Device> anyCapNameContains(String text) {
    return (root, query, cb) -> {
      Subquery<Device> sub = query.subquery(Device.class);
      Root<Device> subRoot = sub.from(Device.class);
      sub.select(subRoot).where(cb.like(cb.lower(subRoot.join("caps").get("name")), "%" + text.toLowerCase() + "%"));
      return cb.in(root).value(sub);
    };
  }

}
