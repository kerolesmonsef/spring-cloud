package com.keroles.ewalletddd.pricing.application;

import com.keroles.ewalletddd.pricing.domain.repository.FeeChargeRepository;
import com.keroles.ewalletddd.pricing.domain.service.FeeCalculationService;
import com.keroles.ewalletddd.pricing.domain.valueObject.FeeChargeRule;
import com.keroles.ewalletddd.pricing.domain.valueObject.FeeCalculationResult;
import com.keroles.ewalletddd.pricing.domain.valueObject.TransactionType;
import com.keroles.ewalletddd.shared.domain.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingApplicationService {

    private final FeeChargeRepository feeCharges;
    private final FeeCalculationService calculator;

    public PricingApplicationService(FeeChargeRepository feeCharges) {
        this.feeCharges = feeCharges;
        this.calculator = new FeeCalculationService();
    }

    @Transactional(readOnly = true)
    public FeeCalculationResult calculateFees(TransactionType transactionType, Money amount) {
        FeeChargeRule rule = feeCharges.findByTransactionType(transactionType);
        return calculator.calculate(rule, amount);
    }
}
