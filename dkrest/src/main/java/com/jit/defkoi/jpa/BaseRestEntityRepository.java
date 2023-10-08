package com.jit.defkoi.jpa;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;

@RepositoryRestResource
public interface BaseRestEntityRepository<T extends BaseRestEntity>
  extends JpaRepository<T, Integer>, JpaSpecificationExecutor<T> {

  @PreAuthorize("hasRole('DEFKOI_USER')")
  T findByName(String name);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  List<T> findAllByOrderByName();

  @PreAuthorize("hasRole('DEFKOI_USER')")
  Set<T> findByRetireTimeGreaterThan(Date date);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  List<T> findByRetireTimeGreaterThanEqualOrderByName(Date date);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @Override
  List<T> findAll();

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @Override
  List<T> findAll(Sort sort);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  Optional<T> findById(Integer id);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  List<T> findByIdIn(Collection<Integer> ids);

  @PreAuthorize("hasRole('DEFKOI_ADMIN')")
  void deleteById(Integer id);

  @PreAuthorize("hasRole('DEFKOI_OPERATOR')")
  @Override
  <S extends T> S save(S entity);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  T findFirstByOrderByModTimeDesc();

  @PreAuthorize("hasRole('DEFKOI_USER')")
  List<T> findAllByOrderByModTime();

  @PreAuthorize("hasRole('DEFKOI_USER')")
  List<T> findFirst10ByOrderByModTimeDesc();

  @PreAuthorize("hasRole('DEFKOI_USER')")
  List<T> findFirst500ByOrderByModTime();

  @PreAuthorize("hasRole('DEFKOI_USER')")
  List<T> findFirst1000ByOrderByModTime();

  @PreAuthorize("hasRole('DEFKOI_USER')")
  List<T> findFirst2000ByOrderByModTime();

  @PreAuthorize("hasRole('DEFKOI_USER')")
  List<T> findByModTimeGreaterThanEqualOrderByModTime(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date date);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  List<T> findFirst500ByModTimeGreaterThanEqualOrderByModTime(
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date date);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  List<T> findFirst1000ByModTimeGreaterThanEqualOrderByModTime(
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date date);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  List<T> findFirst2000ByModTimeGreaterThanEqualOrderByModTime(
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date date);

}

