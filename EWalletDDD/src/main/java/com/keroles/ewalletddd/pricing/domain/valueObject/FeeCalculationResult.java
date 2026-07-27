package com.keroles.ewalletddd.pricing.domain.valueObject;

import com.keroles.ewalletddd.shared.domain.Money;

public record FeeCalculationResult(Money senderTotalAmount,
                                   Money receiverTotalAmount,
                                   Money senderNetAmount,
                                   Money senderFeeValue,
                                   Money receiverFeeValue,
                                   Money senderVatValue,
                                   Money receiverVatValue) {

}
