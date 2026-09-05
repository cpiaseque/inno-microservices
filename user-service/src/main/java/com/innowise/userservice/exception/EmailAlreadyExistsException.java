package com.innowise.userservice.exception;

import java.io.Serial;

public class EmailAlreadyExistsException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 533424169484196759L;

    public EmailAlreadyExistsException(String email) {
        super("Email already exists in the database: " + email);
    }
}