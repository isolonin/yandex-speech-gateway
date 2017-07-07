package ru.speech.gateway.exceptions;

/**
 *
 * @author ivan
 */
public class KeyNotFound extends Exception{
    private String message;

    public KeyNotFound(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
    
}
