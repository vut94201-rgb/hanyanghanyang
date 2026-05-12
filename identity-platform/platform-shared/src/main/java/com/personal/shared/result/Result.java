package com.personal.shared.result;

import com.personal.shared.exception.BusinessException;
import com.personal.shared.exception.ErrorCode;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
/**
 * Sum type for operations whose failure is an expected, non-exceptional outcome
 * (e.g. validation, lookup miss, business rule rejection).
 *
 * <p>Use this when you want the failure case to be visible in the type signature
 * instead of relying on exceptions. For genuinely exceptional cases, throw
 * {@link BusinessException} instead.
 *
 * <p>Sealed so consumers can pattern-match exhaustively (Java 21):
 * <pre>{@code
 * return switch (result) {
 *     case Result.Success<User> s -> s.value();
 *     case Result.Failure<User> f -> throw new BusinessException(f.errorCode());
 * };
 * }</pre>
 *
 * @param <T> value type on success
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {
    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    static <T> Result<T> failure(ErrorCode errorCode) {
        return new Failure<>(errorCode, errorCode.getMessage());
    }

    static <T> Result<T> failure(ErrorCode errorCode, String detailMessage) {
        return new Failure<>(errorCode, detailMessage);
    }

    boolean isSuccess();

    default boolean isFailure() {
        return !isSuccess();
    }

    /**
     * Returns the success value, or throws {@link NoSuchElementException}
     * if this is a failure. Prefer {@link #toOptional()} or pattern-matching.
     */
    T getOrThrow();

    Optional<T> toOptional();

    <R> Result<R> map(Function<? super T, ? extends R> mapper);

    record Success<T>(T value) implements Result<T> {
        public Success {
            Objects.requireNonNull(value, "Success value must not be null");
        }

        @Override public boolean isSuccess() { return true; }
        @Override public T getOrThrow() { return value; }
        @Override public Optional<T> toOptional() { return Optional.of(value); }

        @Override
        public <R> Result<R> map(Function<? super T, ? extends R> mapper) {
            return Result.success(mapper.apply(value));
        }
    }

    record Failure<T>(ErrorCode errorCode, String detailMessage) implements Result<T> {
        public Failure {
            Objects.requireNonNull(errorCode, "ErrorCode must not be null");
        }

        @Override public boolean isSuccess() { return false; }

        @Override
        public T getOrThrow() {
            throw new NoSuchElementException(
                    "Result is Failure: [" + errorCode.getCode() + "] " + detailMessage);
        }

        @Override public Optional<T> toOptional() { return Optional.empty(); }

        @Override
        @SuppressWarnings("unchecked")
        public <R> Result<R> map(Function<? super T, ? extends R> mapper) {
            // Failure is unchanged; safe cast — no T held.
            return (Result<R>) this;
        }
    }
}
