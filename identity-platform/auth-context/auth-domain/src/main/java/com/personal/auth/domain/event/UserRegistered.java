package com.personal.auth.domain.event;

import com.personal.shared.event.DomainEvent;

import java.time.Instant;

public record UserRegistered(Long userId, String email, Instant occurredAt) implements DomainEvent {
    public UserRegistered(Long userId, String email) {
        this(userId, email, Instant.now());
    }
}
