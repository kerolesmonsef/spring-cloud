package com.keroles.ewalletddd.cashout.infrastructure.rail;

import com.keroles.ewalletddd.cashout.domain.port.PayoutRailPort;
import com.keroles.ewalletddd.cashout.domain.port.RailDispatchResult;
import com.keroles.ewalletddd.cashout.domain.valueObject.CashoutId;
import com.keroles.ewalletddd.cashout.domain.valueObject.Rail;
import com.keroles.ewalletddd.shared.domain.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * LuLu — async correspondent rail, outcome arrives by webhook.
 * ponytail: fake — logs and returns PENDING. Wire the real client + webhook in step 4.
 */
@Component
public class LuLuAdapter implements PayoutRailPort {

    private static final Logger log = LoggerFactory.getLogger(LuLuAdapter.class);

    @Override
    public Rail rail() { return Rail.LULU; }

    @Override
    public RailDispatchResult dispatch(CashoutId id, Money amount) {
        String railReference = "LULU-" + UUID.randomUUID();
        log.info("LuLu dispatch cashout={} amount={} {} -> {}",
                id.value(), amount.amount(), amount.currency().getCurrencyCode(), railReference);
        return RailDispatchResult.pending(railReference);
    }
}
