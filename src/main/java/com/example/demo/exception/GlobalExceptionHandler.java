package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MarketNotFoundException.class)
    public ResponseEntity<String> handleMarketTypeNotFound(MarketNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage()); // message will be shown in browser or API response
    }

    @ExceptionHandler(SymbolNotFoundException.class)
    public ResponseEntity<String> handleSymbolNotFound(SymbolNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }
}
