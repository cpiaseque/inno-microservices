package com.innowise.orderservice.repository;

import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.enums.OrderStatus;
import com.innowise.orderservice.util.Constant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(value = "Order.withItems")
    Optional<Order> findOrderById(Long id);

    boolean existsOrderByIdAndUserId(Long orderId, Long userId);

    @EntityGraph(value = "Order.withItems")
    List<Order> findOrdersByIdIn(List<Long> ids);

    @EntityGraph(value = "Order.withItems")
    List<Order> findOrdersByStatusIn(List<OrderStatus> statuses);

    @Modifying
    @Query("UPDATE Order o SET o.status = 'CANCELLED' WHERE o.id = :id")
    void cancelOrderAsUser(@Param(Constant.ID) Long id);
}