package com.jit.defkoi.jpa;

import org.springframework.data.jpa.domain.Specification;

public class WatchSpecs extends BaseRestEntitySpecs {

  public static Specification<Watch> contains(String text) {
    Specification specs = BaseRestEntitySpecs.nameContains(text);
    return specs;
  }

  public static Specification<Watch> extendedContains(String text) {
    return contains(text);
  }

}
