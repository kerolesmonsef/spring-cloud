package com.keroles.ewalletddd.cashout.infrastructure.rail;

import com.keroles.ewalletddd.cashout.domain.port.CashoutRailPort;
import com.keroles.ewalletddd.cashout.domain.port.RailDispatchResult;
import com.keroles.ewalletddd.cashout.domain.valueObject.CashoutId;
import com.keroles.ewalletddd.cashout.domain.valueObject.Rail;
import com.keroles.ewalletddd.shared.domain.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mbank — SYNCHRONOUS bank rail: dispatch settles the cashout and returns the final result,
 * no webhook/callback. ponytail: fake — logs and returns CONFIRMED. Wire the real sync client in step 4.
 */
@Component
public class MbankAdapter implements CashoutRailPort {

    private static final Logger log = LoggerFactory.getLogger(MbankAdapter.class);

    @Override
    public Rail rail() { return Rail.MBANK; }

    @Override
    public RailDispatchResult dispatch(CashoutId id, Money amount) {
        String railReference = "MBANK-" + UUID.randomUUID();
        log.info("Mbank dispatch cashout={} amount={} {} -> CONFIRMED {}",
                id.value(), amount.amount(), amount.currency().code(), railReference);
        return RailDispatchResult.confirmed(railReference); // sync: final outcome now, no callback
    }
}
