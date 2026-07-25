package com.keroles.ewalletddd.pricing.domain.service;

import com.keroles.ewalletddd.pricing.domain.valueObject.FeeCalculationResult;
import com.keroles.ewalletddd.pricing.domain.valueObject.FeeChargeRule;
import com.keroles.ewalletddd.shared.domain.Money;

interface FeeCalculationStrategy {

    FeeCalculationResult calculate(FeeChargeRule rule, Money amount);
}
