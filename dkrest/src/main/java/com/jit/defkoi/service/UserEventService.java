package com.jit.defkoi.service;

import com.jit.defkoi.jpa.UserEvent;
import com.jit.defkoi.jpa.UserEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class UserEventService {

  @Autowired
  private UserEventRepository userEventRepo;

  @Transactional
  @Modifying
  public void save(UserEvent userEvent) {
    userEvent.setTimeStamp(new Date());
    userEventRepo.save(userEvent);
  }

  @PreAuthorize("hasRole('DEFKOI_USER')")
  public List<UserEvent> findDeletedSince(String itemName, Date since) {
    String first = itemName.substring(0, 1).toUpperCase();
    String entityType = itemName.replaceAll("^.", first);
    List<UserEvent> deleted =
      userEventRepo.findByEntityTypeAndEventTypeAndTimeStampAfterOrderByTimeStampDesc(entityType, "deleted", since);
    return deleted;
  }

}
