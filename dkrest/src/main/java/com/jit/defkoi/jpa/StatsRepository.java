package com.jit.defkoi.jpa;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface StatsRepository extends JpaRepository<Stats, Integer>, JpaSpecificationExecutor<Stats> {

  @PreAuthorize("hasRole('DEFKOI_USER')")
  List<Stats> findAll();

  @PreAuthorize("hasRole('DEFKOI_USER')")
  List<Stats> findAll(Sort sort);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  Optional<Stats> findById(Integer id);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  List<Stats> findByIdIn(Collection<Integer> ids);

  @PreAuthorize("hasRole('DEFKOI_ADMIN')")
  void deleteById(Integer id);

  @PreAuthorize("hasRole('DEFKOI_OPERATOR')")
  Stats save(Stats entity);

}
