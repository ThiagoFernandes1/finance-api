package com.thiago.financeapi.api.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource) {
        super(resource + " nao encontrado(a)");
    }
}
