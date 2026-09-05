package com.innowise.authservice.exception;

import java.io.Serial;

public class PhoneNumberAlreadyExistsException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 7602694045328871787L;

    public PhoneNumberAlreadyExistsException(String phoneNumber) {
        super("Phone number already exists in the database: " + phoneNumber);
    }
}