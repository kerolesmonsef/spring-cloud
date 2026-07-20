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
 * Tcs — ASYNCHRONOUS topup rail: dispatch accepts the request and returns PENDING; the real
 * funding outcome arrives later via a callback (simulated by confirm/fail). ponytail: fake —
 * logs and returns PENDING. Wire the real Tcs client + webhook when the saga lands.
 */
@Component
public class TcsAdapter implements TopupRailPort {

    private static final Logger log = LoggerFactory.getLogger(TcsAdapter.class);

    @Override
    public Rail rail() { return Rail.TCS; }

    @Override
    public RailDispatchResult dispatch(TopupId id, Money amount) {
        String railReference = "TCS-" + UUID.randomUUID();
        log.info("Tcs dispatch topup={} amount={} {} -> PENDING {}",
                id.value(), amount.amount(), amount.currency().code(), railReference);
        return RailDispatchResult.pending(railReference); // async: final outcome arrives via callback
    }
}
