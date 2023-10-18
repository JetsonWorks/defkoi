package com.jit.defkoi.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@RepositoryRestResource(excerptProjection = UserEventExcerpt.class)
public interface UserEventRepository extends JpaRepository<UserEvent, Integer>, JpaSpecificationExecutor<UserEvent> {

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @Override
  List<UserEvent> findAll();

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @Override
  List<UserEvent> findAll(Sort sort);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @Override
  Optional<UserEvent> findById(Integer id);

  @PreAuthorize("hasRole('DEFKOI_ADMIN')")
  @Override
  void deleteById(Integer id);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @Override
  Page<UserEvent> findAll(Pageable pageable);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @Override
  <S extends UserEvent> S save(S entity);

  List<UserEvent> findByTimeStampAfter(Date cutoff);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  List<UserEvent> findByEntityTypeAndEventTypeAndTimeStampAfterOrderByTimeStampDesc(String entityType, String eventType,
    Date cutoff);

}
