package com.telecom.authentication.domain.repository;

import com.telecom.authentication.domain.model.User;
import com.telecom.authentication.domain.model.Role;
import com.telecom.authentication.domain.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByRole(Role role);

    List<User> findByBoutiqueId(Long boutiqueId);

    List<User> findByRoleAndBoutiqueId(Role role, Long boutiqueId);

    List<User> findByStatus(UserStatus status);

    Optional<User> findBySetPasswordToken(String setPasswordToken);
}
