package com.keroles.ewalletddd.pricing.domain.service;

import com.keroles.ewalletddd.pricing.domain.valueObject.FeeCalculationResult;
import com.keroles.ewalletddd.pricing.domain.valueObject.FeeChargeRule;
import com.keroles.ewalletddd.pricing.domain.valueObject.FeeType;
import com.keroles.ewalletddd.pricing.domain.valueObject.TransactionType;
import com.keroles.ewalletddd.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeeCalculationServiceTest {

    private final FeeCalculationService service = new FeeCalculationService();

    @Test
    void topup_senderIsSystemAccount_noFees_receiverPaysFeeAndVat() {
        FeeChargeRule rule = new FeeChargeRule(
                TransactionType.TOPUP,
                new BigDecimal("1.50"), FeeType.PERCENTAGE,
                new BigDecimal("1.00"), FeeType.VALUE,
                new BigDecimal("5.00"));
        Money amount = Money.of("100.00", "AED");

        FeeCalculationResult result = service.calculate(rule, amount);

        assertEquals(Money.of("0.00", "AED"), result.senderFeeValue());
        assertEquals(Money.of("0.00", "AED"), result.senderVatValue());
        assertEquals(Money.of("1.00", "AED"), result.receiverFeeValue());
        assertEquals(Money.of("0.05", "AED"), result.receiverVatValue());
        assertEquals(Money.of("100.00", "AED"), result.senderTotalAmount());
        assertEquals(Money.of("98.95", "AED"), result.receiverTotalAmount());
    }

    @Test
    void cashout_receiverIsSystemAccount_noFees_senderPaysFeeAndVat() {
        FeeChargeRule rule = new FeeChargeRule(
                TransactionType.CASHOUT,
                new BigDecimal("5.00"), FeeType.VALUE,
                new BigDecimal("0.50"), FeeType.PERCENTAGE,
                new BigDecimal("5.00"));
        Money amount = Money.of("1000.00", "AED");

        FeeCalculationResult result = service.calculate(rule, amount);

        assertEquals(Money.of("5.00", "AED"), result.senderFeeValue());
        assertEquals(Money.of("0.25", "AED"), result.senderVatValue());
        assertEquals(Money.of("0.00", "AED"), result.receiverFeeValue());
        assertEquals(Money.of("0.00", "AED"), result.receiverVatValue());
        assertEquals(Money.of("1005.25", "AED"), result.senderTotalAmount());
        assertEquals(Money.of("1000.00", "AED"), result.receiverTotalAmount());
    }

    @Test
    void transfer_bothSidesPayFeeAndVat() {
        FeeChargeRule rule = new FeeChargeRule(
                TransactionType.TRANSFER,
                new BigDecimal("1.00"), FeeType.PERCENTAGE,
                new BigDecimal("2.00"), FeeType.VALUE,
                new BigDecimal("5.00"));
        Money amount = Money.of("200.00", "AED");

        FeeCalculationResult result = service.calculate(rule, amount);

        assertEquals(Money.of("2.00", "AED"), result.senderFeeValue());
        assertEquals(Money.of("0.10", "AED"), result.senderVatValue());
        assertEquals(Money.of("2.00", "AED"), result.receiverFeeValue());
        assertEquals(Money.of("0.10", "AED"), result.receiverVatValue());
        assertEquals(Money.of("202.10", "AED"), result.senderTotalAmount());
        assertEquals(Money.of("197.90", "AED"), result.receiverTotalAmount());
    }

    @Test
    void noRuleConfigured_zeroFeesOnBothSides() {
        FeeChargeRule rule = FeeChargeRule.zero(TransactionType.TRANSFER);
        Money amount = Money.of("50.00", "AED");

        FeeCalculationResult result = service.calculate(rule, amount);

        assertEquals(Money.of("50.00", "AED"), result.senderTotalAmount());
        assertEquals(Money.of("50.00", "AED"), result.receiverTotalAmount());
    }
}
