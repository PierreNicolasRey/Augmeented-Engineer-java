package com.exalt.it.belair.application.dto;

/**
 * Generic error response DTO returned for HTTP error responses.
 * Contains a machine-readable error code and a human-readable message.
 */
public record ErrorResponse(String error, String message) {

    /**
     * Factory method for creating an ErrorResponse.
     *
     * @param error   the machine-readable error code (e.g., "ORDER_NOT_FOUND")
     * @param message the human-readable error message
     * @return a new ErrorResponse instance
     */
    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message);
    }
}
