package com.personal.shared.event;

import java.time.Instant;

/**
 * Marker interface for all domain events.
 *
 * <p>A domain event represents something meaningful that happened in the
 * business domain (e.g. {@code UserRegistered}, {@code TokenRevoked},
 * {@code SessionExpired}). Domain events are produced by domain logic and
 * later dispatched by the application layer to interested handlers
 * (other contexts, async workers, audit log, etc.).
 *
 * <p>Conventions:
 * <ul>
 *   <li>Past tense naming: {@code UserRegistered}, not {@code RegisterUser}.</li>
 *   <li>Immutable: implement as a {@code record} or a class with final fields.</li>
 *   <li>Pure domain data: no Spring, JPA, or transport-specific types.</li>
 *   <li>Each event captures {@link #occurredAt()} so handlers know when the
 *       fact happened (not when it was processed).</li>
 * </ul>
 *
 * <p>Typical implementation:
 * <pre>{@code
 * public record UserRegistered(
 *     Long userId,
 *     String email,
 *     Instant occurredAt
 * ) implements DomainEvent {
 *     public UserRegistered(Long userId, String email) {
 *         this(userId, email, Instant.now());
 *     }
 * }
 * }</pre>
 */
public interface DomainEvent {
    /**
     * The moment in time when this event occurred in the domain.
     * Must be set when the event is created, not when it is dispatched.
     */
    Instant occurredAt();}
