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
}
