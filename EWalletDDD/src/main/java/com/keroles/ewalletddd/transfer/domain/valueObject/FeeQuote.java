package com.keroles.ewalletddd.transfer.domain.valueObject;

import com.keroles.ewalletddd.shared.domain.Money;

public record FeeQuote(Money senderTotalAmount,
                       Money receiverTotalAmount,
                       Money senderNetAmount,
                       Money senderFeeValue,
                       Money receiverFeeValue,
                       Money senderVatValue,
                       Money receiverVatValue) {
}
