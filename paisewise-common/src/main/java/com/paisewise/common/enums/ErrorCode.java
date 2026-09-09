package com.paisewise.common.enums;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    RESOURCE_NOT_FOUND("ERR_404_01", "Requested resource not found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED_ACCESS("ERR_401_01", "Unauthorized access or invalid token", HttpStatus.UNAUTHORIZED),
    BAD_REQUEST("ERR_400_01", "Invalid request parameters or payload", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("ERR_500_01", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public HttpStatus getHttpStatus() { return httpStatus; }
}