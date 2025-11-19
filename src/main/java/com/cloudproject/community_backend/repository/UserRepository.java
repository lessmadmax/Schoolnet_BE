package com.cloudproject.community_backend.repository;
import java.util.Optional;

import com.cloudproject.community_backend.entity.User;
import com.cloudproject.community_backend.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    long countByRole(UserRole role);
    long countByIsSeniorVerified(boolean isSeniorVerified);
}
