package com.github.dgsc_fav.wheelytest.exception;

/**
 * Created by DG on 21.10.2016.
 */
public class BadCredientalsException extends RuntimeException {
    public BadCredientalsException() {
        super("Bad credientals");
    }

    public BadCredientalsException(String message) {
        super(message);
    }
}
