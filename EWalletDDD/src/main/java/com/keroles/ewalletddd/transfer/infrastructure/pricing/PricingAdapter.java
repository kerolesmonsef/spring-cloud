package com.keroles.ewalletddd.transfer.infrastructure.pricing;

import com.keroles.ewalletddd.pricing.application.PricingApplicationService;
import com.keroles.ewalletddd.pricing.domain.valueObject.FeeCalculationResult;
import com.keroles.ewalletddd.pricing.domain.valueObject.TransactionType;
import com.keroles.ewalletddd.shared.domain.Money;
import com.keroles.ewalletddd.transfer.domain.port.PricingPort;
import com.keroles.ewalletddd.transfer.domain.valueObject.FeeQuote;
import org.springframework.stereotype.Component;

@Component
public class PricingAdapter implements PricingPort {

    private final PricingApplicationService pricing;

    public PricingAdapter(PricingApplicationService pricing) {
        this.pricing = pricing;
    }

    @Override
    public FeeQuote calculateTransferFees(Money amount) {
        FeeCalculationResult result = pricing.calculateFees(TransactionType.TRANSFER, amount);
        return new FeeQuote(
                result.senderTotalAmount(),
                result.receiverTotalAmount(),
                result.senderNetAmount(),
                result.senderFeeValue(), result.receiverFeeValue(),
                result.senderVatValue(), result.receiverVatValue()
        );
    }
}
