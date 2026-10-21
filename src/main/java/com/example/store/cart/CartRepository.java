package com.example.store.cart;

import com.example.store.session.UserSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findBySession(UserSession session);
}
