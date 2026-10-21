package com.example.store.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "store_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private int failedLoginAttempts;

    private Instant lockedUntil;

    protected User() {
    }

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        clearFailedLoginAttempts();
    }

    public void registerFailedLoginAttempt() {
        failedLoginAttempts++;
    }

    public void clearFailedLoginAttempts() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    public void lockUntil(Instant moment) {
        lockedUntil = moment;
    }

    public boolean isLockedAt(Instant moment) {
        return lockedUntil != null && lockedUntil.isAfter(moment);
    }

    public boolean hasLockRecord() {
        return lockedUntil != null;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }
}
