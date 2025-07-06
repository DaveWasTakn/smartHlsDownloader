package org.davesEnterprise.network;

public class OutOfRetriesException extends RuntimeException {

    public OutOfRetriesException(String message, Throwable cause) {
        super(message, cause);
    }

    public OutOfRetriesException(String message) {
        super(message);
    }
}
