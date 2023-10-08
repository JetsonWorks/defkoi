package com.jit.defkoi.jpa;

import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RepositoryRestResource(excerptProjection = UserWithRelations.class)
public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

  @PreAuthorize("hasRole('DEFKOI_USER')")
  User findByName(String name);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  List<User> findAllByOrderByName();

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @Override
  List<User> findAll();

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @Override
  List<User> findAll(Sort sort);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  Optional<User> findById(String id);

  @PreAuthorize("hasRole('DEFKOI_ADMIN')")
  void deleteById(String id);

  @PreAuthorize("hasRole('DEFKOI_USER')")
  @Override
  Page<User> findAll(Pageable pageable);

  @PreAuthorize("hasRole('DEFKOI_OPERATOR')")
  @Override
  <S extends User> S save(S entity);

  default User loggedUser() {
    return loggedUser(false);
  }

  default User loggedUser(boolean create) {
    String name = getPrincipalUsername();
    if(name == null)
      return null;
    Optional<User> logged = findById(name);
    if(!logged.isPresent())
      if(create)
        return save(new User(name));
      else
        return null;
    return logged.get();
  }

  default String getPrincipalUsername() {
    AccessToken token = getAccessToken();
    if(token != null)
      return token.getPreferredUsername();
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if(auth == null)
      return null;
    if(auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User)
      return ((org.springframework.security.core.userdetails.User)auth.getPrincipal()).getUsername();
    if(auth.getPrincipal() instanceof Principal)
      return ((Principal)auth.getPrincipal()).getName();
    return auth.getPrincipal().toString();
  }

  default AccessToken getAccessToken() {
    if(SecurityContextHolder.getContext().getAuthentication() instanceof KeycloakAuthenticationToken) {
      KeycloakAuthenticationToken auth =
        (KeycloakAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
      return auth.getAccount().getKeycloakSecurityContext().getToken();
    } else
      return null;
  }

  default String getPrincipalEmail() {
    AccessToken token = getAccessToken();
    if(token != null)
      return token.getEmail();
    return getPrincipalUsername();
  }

  default Collection<? extends GrantedAuthority> getPrincipalAuthorities() {
    AccessToken token = getAccessToken();
    if(token == null)
      return SecurityContextHolder.getContext().getAuthentication().getAuthorities();
    return AuthorityUtils.commaSeparatedStringToAuthorityList(
      token.getRealmAccess().getRoles().stream().collect(Collectors.joining(",")));
  }

}
