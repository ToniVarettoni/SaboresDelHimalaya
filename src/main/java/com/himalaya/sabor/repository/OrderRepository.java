package com.himalaya.sabor.repository;

import com.himalaya.sabor.model.Order;
import com.himalaya.sabor.model.OrderStatus;
import com.himalaya.sabor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
            OrderStatus status, LocalDateTime start, LocalDateTime end);
}
