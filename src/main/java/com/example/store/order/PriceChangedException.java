package com.example.store.order;

import com.example.store.common.ApiException;
import com.example.store.common.Money;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;

public class PriceChangedException extends ApiException {

    public PriceChangedException(String title, BigDecimal expectedPrice, BigDecimal currentPrice) {
        super(HttpStatus.CONFLICT, "Price of '%s' changed from %s to %s, review the cart before checkout"
                .formatted(title, Money.format(expectedPrice), Money.format(currentPrice)));
    }
}
