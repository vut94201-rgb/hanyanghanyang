package com.personal.identity.api.dto;
public record AuthenticatedUser(
        Long userId,
        String sessionId,
        String tokenId
) {
}