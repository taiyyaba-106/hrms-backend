package com.hrms.repository;

import com.hrms.entity.UserActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserActivationTokenRepository extends JpaRepository<UserActivationToken, Long> {

    Optional<UserActivationToken> findByToken(String token);
}
