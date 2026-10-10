package com.placetrack.repository;

import com.placetrack.entity.ApplicationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationEventRepository extends JpaRepository<ApplicationEvent, Long> {
}
