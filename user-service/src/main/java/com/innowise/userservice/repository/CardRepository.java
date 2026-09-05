package com.innowise.userservice.repository;

import com.innowise.userservice.model.entity.Card;
import com.innowise.userservice.util.Constant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    Optional<Card> findCardById(Long id);

    @Query(value = "SELECT * FROM card_info WHERE id IN :ids", nativeQuery = true)
    List<Card> findCardsByIdIn(@Param("ids") List<Long> ids);

    boolean existsByNumber(String number);

    @Modifying
    @Query(value = """
            UPDATE card_info
            SET holder = :holder
            WHERE user_id = :userId
            """, nativeQuery = true)
    void updateHolderByUserId(@Param(Constant.USER_ID) Long userId, @Param(Constant.HOLDER) String holder);

    @Modifying
    @Query("DELETE FROM Card c WHERE c.id = :id")
    void deleteCardById(@Param(Constant.ID) Long id);
}