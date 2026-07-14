package com.exalt.it.belair.application.config;

import com.exalt.it.belair.application.dto.ErrorResponse;
import com.exalt.it.belair.domain.order.exceptions.EmptyOrderException;
import com.exalt.it.belair.domain.order.exceptions.FestivalGoerNotFoundException;
import com.exalt.it.belair.domain.order.exceptions.InsufficientTokensException;
import com.exalt.it.belair.domain.order.exceptions.InvalidItemTypeException;
import com.exalt.it.belair.domain.order.exceptions.InvalidQuantityException;
import com.exalt.it.belair.domain.order.exceptions.InvalidSubtypeException;
import com.exalt.it.belair.domain.order.exceptions.OrderCannotBeAcknowledgedException;
import com.exalt.it.belair.domain.order.exceptions.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(EmptyOrderException.class)
    public ResponseEntity<Void> handleEmptyOrder(EmptyOrderException e) {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(InvalidItemTypeException.class)
    public ResponseEntity<Void> handleInvalidItemType(InvalidItemTypeException e) {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(InvalidSubtypeException.class)
    public ResponseEntity<Void> handleInvalidSubtype(InvalidSubtypeException e) {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(InvalidQuantityException.class)
    public ResponseEntity<Void> handleInvalidQuantity(InvalidQuantityException e) {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(FestivalGoerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFestivalGoerNotFound(FestivalGoerNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("FESTIVAL_GOER_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(InsufficientTokensException.class)
    public ResponseEntity<Void> handleInsufficientTokens(InsufficientTokensException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("ORDER_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(OrderCannotBeAcknowledgedException.class)
    public ResponseEntity<ErrorResponse> handleOrderCannotBeAcknowledged(OrderCannotBeAcknowledgedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("ORDER_CANNOT_BE_ACKNOWLEDGED", e.getMessage()));
    }
}
