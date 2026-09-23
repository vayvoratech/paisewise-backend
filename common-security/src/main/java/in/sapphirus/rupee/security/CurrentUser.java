package in.sapphirus.rupee.security;

import org.springframework.security.core.context.SecurityContextHolder;

/** Convenience accessor for the authenticated user's id (the JWT subject). */
public final class CurrentUser {

    private CurrentUser() {}

    /** @return the current user id, or null if unauthenticated. */
    public static String id() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : (String) auth.getPrincipal();
    }

    public static String requireId() {
        String id = id();
        if (id == null) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        return id;
    }
}
