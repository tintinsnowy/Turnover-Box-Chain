package com.template;

import net.corda.core.CordaRuntimeException;

public class NotFoundException extends CordaRuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}