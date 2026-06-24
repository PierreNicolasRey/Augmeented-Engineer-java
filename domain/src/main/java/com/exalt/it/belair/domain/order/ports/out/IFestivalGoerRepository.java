package com.exalt.it.belair.domain.order.ports.out;

import com.exalt.it.belair.domain.order.model.FestivalGoerBalance;

/**
 * Outbound port for retrieving festival goer balance information.
 * Implemented by the Infrastructure layer to fetch token balance data.
 */
public interface IFestivalGoerRepository {
    /**
     * Retrieves the token balance for a festival goer.
     *
     * @param festivalGoerId the unique identifier of the festival goer
     * @return the FestivalGoerBalance containing drink and snack tokens
     * @throws FestivalGoerNotFoundException if the festival goer is not found
     */
    FestivalGoerBalance getBalance(String festivalGoerId);
}
