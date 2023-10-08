package com.jit.defkoi.jpa;

import org.springframework.data.jpa.domain.Specification;

public class UserSpecs {

  public static Specification<User> contains(String text) {
    Specification specs = nameContains(text);
    return specs.or(firstNameContains(text)).or(lastNameContains(text)).or(emailAddressContains(text))
      .or(notesContains(text));
  }

  public static Specification<User> extendedContains(String text) {
    return contains(text);
  }

  public static Specification<User> nameContains(String text) {
    return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + text.toLowerCase() + "%");
  }

  public static Specification<User> firstNameContains(String text) {
    return (root, query, cb) -> cb.like(cb.lower(root.get("firstName")), "%" + text.toLowerCase() + "%");
  }

  public static Specification<User> lastNameContains(String text) {
    return (root, query, cb) -> cb.like(cb.lower(root.get("lastName")), "%" + text.toLowerCase() + "%");
  }

  public static Specification<User> emailAddressContains(String text) {
    return (root, query, cb) -> cb.like(cb.lower(root.get("emailAddress")), "%" + text.toLowerCase() + "%");
  }

  public static Specification<User> notesContains(String text) {
    return (root, query, cb) -> cb.like(cb.lower(root.get("notes")), "%" + text.toLowerCase() + "%");
  }

}
