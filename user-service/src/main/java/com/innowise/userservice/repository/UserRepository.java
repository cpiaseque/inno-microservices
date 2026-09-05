package com.innowise.userservice.repository;

import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.util.Constant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findUserById(Long id);

    List<User> findUsersByIdIn(List<Long> ids);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.cards WHERE u.email = :email")
    Optional<User> findUserByEmail(@Param(Constant.EMAIL) String email);

    boolean existsByEmail(String email);

    @Modifying
    @Query(value = """
            UPDATE users
            SET name = :name, surname = :surname, birth_date = :birthDate, email = :email
            WHERE id = :id
            """, nativeQuery = true)
    void updateUser(@Param(Constant.ID) Long id,
                    @Param(Constant.NAME) String name,
                    @Param(Constant.SURNAME) String surname,
                    @Param(Constant.BIRTH_DATE) LocalDate birthDate,
                    @Param(Constant.EMAIL) String email);

    @Modifying
    @Query("DELETE FROM User u WHERE u.id = :id")
    void deleteUserById(@Param(Constant.ID) Long id);
}