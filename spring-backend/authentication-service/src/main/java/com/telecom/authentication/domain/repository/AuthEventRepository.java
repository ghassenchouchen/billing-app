package com.telecom.authentication.domain.repository;

import com.telecom.authentication.domain.model.AuthEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthEventRepository extends JpaRepository<AuthEvent, Long> {

    List<AuthEvent> findByUsernameOrderByCreatedAtDesc(String username);

    List<AuthEvent> findTop50ByOrderByCreatedAtDesc();
}
