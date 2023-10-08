package com.jit.defkoi.audit;

import com.jit.defkoi.jpa.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import java.util.Date;

@Service
@Component
public class EntityEventListener implements InitializingBean, ApplicationContextAware {

  private static ApplicationContext appContext;
  private static UserRepository userRepo;
  private static UserEventRepository userEventRepo;

  @PrePersist
  @PreUpdate
  // The @PreUpdate callback is only called if the data is actually changed
  private void preSave(Object entity) {
    if(entity instanceof UserEvent)
      return;

    User user = userRepo.loggedUser();
    if(user == null)
      return;
    // ignore the creation of the current user
    if(entity instanceof User) {
      if(user == null)
        return;
    }

    if(BaseRestEntity.class.isAssignableFrom(entity.getClass()))
      ((BaseRestEntity)entity).setModTime(new Date());

    userEventRepo.save(UserEvent.saved(user, entity));
  }

  @PreRemove
  private void preDelete(Object entity) {
    User user = userRepo.loggedUser();
    if(user == null)
      return;
    userEventRepo.save(UserEvent.deleted(user, entity));
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    this.userRepo = appContext.getBean(UserRepository.class);
    this.userEventRepo = appContext.getBean(UserEventRepository.class);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.appContext = applicationContext;
  }

}
