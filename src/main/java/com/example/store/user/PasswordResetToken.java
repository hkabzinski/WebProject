package com.example.store.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used;

    protected PasswordResetToken() {
    }

    public PasswordResetToken(String id, User user, Instant expiresAt) {
        this.id = id;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    public boolean isUsableAt(Instant moment) {
        return !used && expiresAt.isAfter(moment);
    }

    public void markUsed() {
        used = true;
    }

    public String getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return used;
    }
}
