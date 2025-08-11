package com.beauty_store.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beauty_store.backend.model.Cart;
import com.beauty_store.backend.model.User;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Cart findByUser(User user);

    @Query("SELECT c FROM Cart c JOIN FETCH c.user WHERE c.user.id = :userId")
    Optional<Cart> findByUserId(@Param("userId") Long userId);
}