package com.example.logistics.security;

import java.util.UUID;

/**
 * The authenticated caller. userId is the Identity & Access Service user id
 * (a reference id only - no personal data is stored locally).
 */
public record CurrentUser(UUID userId, UserRole role) {
}
