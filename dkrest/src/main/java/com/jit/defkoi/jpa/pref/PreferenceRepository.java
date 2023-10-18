package com.jit.defkoi.jpa.pref;

import com.jit.defkoi.jpa.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RepositoryRestResource
public interface PreferenceRepository extends JpaRepository<Preference, Integer> {

  Set<Preference> findByUser(User user);

  Preference findByUserAndName(User user, String name);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @Override
  List<Preference> findAll();

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @Override
  List<Preference> findAll(Sort sort);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @Override
  Optional<Preference> findById(Integer id);

  @PreAuthorize("hasRole('DEFKOI_ADMIN')")
  @Override
  void deleteById(Integer id);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @Override
  Page<Preference> findAll(Pageable pageable);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @Override
  <S extends Preference> S save(S entity);

}

