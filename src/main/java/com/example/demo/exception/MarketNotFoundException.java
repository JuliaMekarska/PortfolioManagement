package com.example.demo.exception;

public class MarketNotFoundException extends RuntimeException {

    public MarketNotFoundException(String marketName) {
        super("Market type not found: " + marketName);
    }
}
