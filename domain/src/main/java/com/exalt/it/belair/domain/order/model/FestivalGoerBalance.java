package com.exalt.it.belair.domain.order.model;

import com.exalt.it.belair.domain.order.exceptions.InsufficientTokensException;

/**
 * Represents the token balance of a festival goer.
 * Manages both total tokens and reserved tokens for drinks and snacks.
 */
public class FestivalGoerBalance {
    private int totalDrinkTokens;
    private int totalSnackTokens;
    private int reservedDrinkTokens = 0;
    private int reservedSnackTokens = 0;

    /**
     * Constructs a FestivalGoerBalance with the specified token amounts.
     * 
     * @param drinkTokens the total number of drink tokens
     * @param snackTokens the total number of snack tokens
     */
    public FestivalGoerBalance(int drinkTokens, int snackTokens) {
        this.totalDrinkTokens = drinkTokens;
        this.totalSnackTokens = snackTokens;
    }

    /**
     * Gets the total drink tokens.
     * @return the total drink tokens
     */
    public int getDrinkTokens() {
        return totalDrinkTokens;
    }

    /**
     * Gets the total snack tokens.
     * @return the total snack tokens
     */
    public int getSnackTokens() {
        return totalSnackTokens;
    }

    /**
     * Gets the number of reserved drink tokens.
     * @return the reserved drink tokens
     */
    public int getReservedDrinkTokens() {
        return reservedDrinkTokens;
    }

    /**
     * Gets the number of available (unreserved) drink tokens.
     * @return the available drink tokens (total - reserved)
     */
    public int getAvailableDrinkTokens() {
        return totalDrinkTokens - reservedDrinkTokens;
    }

    /**
     * Gets the number of reserved snack tokens.
     * @return the reserved snack tokens
     */
    public int getReservedSnackTokens() {
        return reservedSnackTokens;
    }

    /**
     * Gets the number of available (unreserved) snack tokens.
     * @return the available snack tokens (total - reserved)
     */
    public int getAvailableSnackTokens() {
        return totalSnackTokens - reservedSnackTokens;
    }

    /**
     * Reserves the specified amount of drink tokens.
     * 
     * @param amount the amount to reserve
     * @throws InsufficientTokensException if the available tokens are less than the amount to reserve
     */
    public void reserveDrinkTokens(int amount) {
        if (amount > getAvailableDrinkTokens()) {
            throw new InsufficientTokensException("Insufficient drink tokens");
        }
        reservedDrinkTokens += amount;
    }

    /**
     * Reserves the specified amount of snack tokens.
     * 
     * @param amount the amount to reserve
     * @throws InsufficientTokensException if the available tokens are less than the amount to reserve
     */
    public void reserveSnackTokens(int amount) {
        if (amount > getAvailableSnackTokens()) {
            throw new InsufficientTokensException("Insufficient snack tokens");
        }
        reservedSnackTokens += amount;
    }

    /**
     * Unreserves the specified amount of drink and snack tokens.
     * Used during order cancellation to release tokens back to the festival goer's available balance.
     * 
     * @param drinkTokens the number of drink tokens to unreserve
     * @param snackTokens the number of snack tokens to unreserve
     */
    public void unreserveTokens(int drinkTokens, int snackTokens) {
        reservedDrinkTokens -= drinkTokens;
        reservedSnackTokens -= snackTokens;
    }

    /**
     * Consumes reserved tokens when an order is acknowledged.
     * This permanently deducts consumed tokens from total balances and releases
     * matching reserved amounts.
     *
     * @param drinkTokens number of reserved drink tokens to consume
     * @param snackTokens number of reserved snack tokens to consume
     */
    public void consumeTokens(int drinkTokens, int snackTokens) {
        int newTotalDrinkTokens = totalDrinkTokens - drinkTokens;
        int newReservedDrinkTokens = reservedDrinkTokens - drinkTokens;
        int newTotalSnackTokens = totalSnackTokens - snackTokens;
        int newReservedSnackTokens = reservedSnackTokens - snackTokens;

        if (newTotalDrinkTokens < 0 || newReservedDrinkTokens < 0) {
            throw new InsufficientTokensException("Insufficient drink tokens");
        }
        if (newTotalSnackTokens < 0 || newReservedSnackTokens < 0) {
            throw new InsufficientTokensException("Insufficient snack tokens");
        }

        totalDrinkTokens = newTotalDrinkTokens;
        reservedDrinkTokens = newReservedDrinkTokens;
        totalSnackTokens = newTotalSnackTokens;
        reservedSnackTokens = newReservedSnackTokens;
    }
}
