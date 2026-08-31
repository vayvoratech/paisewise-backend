package in.sapphirus.rupee.web;

/**
 * Standard error body returned to clients: { code, message }.
 * Matches the mobile client's ApiError shape so error handling is uniform.
 */
public record ApiError(String code, String message) {}
