package com.thiago.financeapi.api.exception;

/** Violacao de regra de negocio; resulta em HTTP 409. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
