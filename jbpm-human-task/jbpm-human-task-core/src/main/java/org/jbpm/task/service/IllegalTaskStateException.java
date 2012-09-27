package org.jbpm.task.service;

public class IllegalTaskStateException extends TaskException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public IllegalTaskStateException(String message) {
        super(message);
    }

    public IllegalTaskStateException(String message, Throwable cause) {
        super(message, cause);
    }

}
