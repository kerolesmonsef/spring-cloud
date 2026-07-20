package com.keroles.ewalletddd.topup.infrastructure.rail;

import com.keroles.ewalletddd.topup.domain.port.RailDispatchResult;
import com.keroles.ewalletddd.topup.domain.port.TopupRailPort;
import com.keroles.ewalletddd.topup.domain.valueObject.Rail;
import com.keroles.ewalletddd.topup.domain.valueObject.TopupId;
import com.keroles.ewalletddd.shared.domain.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mbank — SYNCHRONOUS topup rail: dispatch settles the topup and returns the final result,
 * no webhook/callback. ponytail: fake — logs and returns CONFIRMED. Wire the real sync client
 * when the saga lands.
 */
@Component("topupMbankAdapter") // cashout also has an MbankAdapter — disambiguate the bean name (routing is by rail(), not name)
public class MbankAdapter implements TopupRailPort {

    private static final Logger log = LoggerFactory.getLogger(MbankAdapter.class);

    @Override
    public Rail rail() { return Rail.MBANK; }

    @Override
    public RailDispatchResult dispatch(TopupId id, Money amount) {
        String railReference = "MBANK-" + UUID.randomUUID();
        log.info("Mbank dispatch topup={} amount={} {} -> CONFIRMED {}",
                id.value(), amount.amount(), amount.currency().code(), railReference);
        return RailDispatchResult.confirmed(railReference); // sync: final outcome now, no callback
    }
}
