package com.innowise.authservice.repository;

import com.innowise.authservice.model.entity.UserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCredentialsRepository extends JpaRepository<UserCredentials, String> {
    boolean existsByPhoneNumber(String phoneNumber);

    Optional<UserCredentials> findCredentialsByPhoneNumber(String phoneNumber);

    Optional<UserCredentials> findCredentialsByUserId(Long userId);
}