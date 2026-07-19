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
 * Aani — UAE NPSS. Real integration = ISO 20022 message, sync-ish, irreversible once accepted.
 * ponytail: fake — logs and returns PENDING. Wire the real NPSS client + normalize-to-async webhook in step 4.
 */
@Component
public class AaniAdapter implements CashoutRailPort {

    private static final Logger log = LoggerFactory.getLogger(AaniAdapter.class);

    @Override
    public Rail rail() { return Rail.AANI; }

    @Override
    public RailDispatchResult dispatch(CashoutId id, Money amount) {
        String railReference = "AANI-" + UUID.randomUUID();
        log.info("Aani dispatch cashout={} amount={} {} -> {}",
                id.value(), amount.amount(), amount.currency().code(), railReference);
        return RailDispatchResult.pending(railReference);
    }
}
