package com.exalt.it.belair.domain.order.ports.out;

import com.exalt.it.belair.domain.order.model.FestivalGoerBalance;

/**
 * Outbound port for retrieving and persisting festival goer balance information.
 * Implemented by the Infrastructure layer to fetch and save token balance data.
 */
public interface IFestivalGoerRepository {
    /**
     * Retrieves the token balance for a festival goer.
     *
     * @param festivalGoerId the unique identifier of the festival goer
     * @return the FestivalGoerBalance containing drink and snack tokens
     * @throws com.exalt.it.belair.domain.order.exceptions.FestivalGoerNotFoundException if the festival goer is not found
     */
    FestivalGoerBalance getBalance(String festivalGoerId);

    /**
     * Saves the token balance for a festival goer.
     * Used to persist token state changes (e.g., after unreserving tokens during order cancellation).
     *
     * @param balance the FestivalGoerBalance to save
     * @return the saved FestivalGoerBalance
     */
    FestivalGoerBalance saveBalance(FestivalGoerBalance balance);
}
