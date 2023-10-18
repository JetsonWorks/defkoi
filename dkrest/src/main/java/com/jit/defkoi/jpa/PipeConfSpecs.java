package com.jit.defkoi.jpa;

import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

public class PipeConfSpecs extends BaseRestEntitySpecs {

  public static Specification<PipeConf> contains(String text) {
    Specification specs = BaseRestEntitySpecs.nameContains(text);
    return specs;
  }

  public static Specification<PipeConf> extendedContains(String text) {
    return contains(text).or(displayNameContains(text)).or(deviceCardContains(text));
  }

  public static Specification<PipeConf> displayNameContains(String text) {
    return (root, query, cb) -> {
      Subquery<Device> sub = query.subquery(Device.class);
      Root<Device> subRoot = sub.from(Device.class);
      sub.select(subRoot).where(DeviceSpecs.displayNameContains(text).toPredicate(subRoot, query, cb));
      return cb.in(root.get("device")).value(sub);
    };
  }

  public static Specification<PipeConf> deviceCardContains(String text) {
    return (root, query, cb) -> {
      Subquery<Device> sub = query.subquery(Device.class);
      Root<Device> subRoot = sub.from(Device.class);
      sub.select(subRoot).where(DeviceSpecs.deviceCardContains(text).toPredicate(subRoot, query, cb));
      return cb.in(root.get("device")).value(sub);
    };
  }

}
