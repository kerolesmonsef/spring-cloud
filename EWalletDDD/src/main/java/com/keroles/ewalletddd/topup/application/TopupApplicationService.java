package com.keroles.ewalletddd.topup.application;

import com.keroles.ewalletddd.topup.domain.model.TopupRequest;
import com.keroles.ewalletddd.topup.domain.port.LedgerTopupPort;
import com.keroles.ewalletddd.topup.domain.port.RailDispatchResult;
import com.keroles.ewalletddd.topup.domain.port.TopupRailRegistry;
import com.keroles.ewalletddd.topup.domain.repository.TopupRepository;
import com.keroles.ewalletddd.topup.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.topup.domain.valueObject.Rail;
import com.keroles.ewalletddd.topup.domain.valueObject.TopupId;
import com.keroles.ewalletddd.shared.domain.Money;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopupApplicationService {

    private final TopupRepository topups;
    private final LedgerTopupPort ledger;   // ACL to the Accounting front door
    private final TopupRailRegistry rails;
    private final ApplicationEventPublisher eventPublisher; // ponytail: in-process; Outbox in step 5

    public TopupApplicationService(TopupRepository topups,
                                   LedgerTopupPort ledger,
                                   TopupRailRegistry rails,
                                   ApplicationEventPublisher eventPublisher) {
        this.topups = topups;
        this.ledger = ledger;
        this.rails = rails;
        this.eventPublisher = eventPublisher;
    }

    // money system -> user. No hold: the Ledger is credited only on success, never up front.
    @Transactional
    public TopupId requestTopup(LedgerAccountRef account, Money amount, Rail rail) {
        TopupRequest topup = TopupRequest.request(account, amount, rail); // PENDING — Ledger untouched
        // ponytail: dispatch runs inside the request tx because the fake rail is in-process.
        // Real rails dispatch AFTER commit via the Outbox (step 5) — don't hold a tx open across an external call.
        RailDispatchResult result = rails.forRail(rail).dispatch(topup.id(), amount);
        switch (result.outcome()) {
            case PENDING -> topup.recordDispatch(result.railReference()); // async (Tcs): await confirm/fail callback
            case CONFIRMED -> {                                           // sync (Mbank): credit now, no callback
                topup.recordDispatch(result.railReference());
                topup.complete();                                        // guard FIRST — before money moves
                topup.recordLedgerRef(ledger.topup(account, amount));    // ...then credit + attach the link
            }
            case REJECTED -> topup.fail(result.reason());                // rail refused up front — Ledger never touched
        }
        topups.save(topup);
        publishEvents(topup);
        return topup.id();
    }

    // simulates the RailConfirmed callback (becomes an event handler when the saga lands)
    @Transactional
    public void confirm(TopupId id) {
        TopupRequest topup = load(id);
        topup.complete();                                                // guard FIRST — throws before money moves
        topup.recordLedgerRef(ledger.topup(topup.account(), topup.amount())); // credit now
        topups.save(topup);
        publishEvents(topup);
    }

    // simulates the RailFailed callback (becomes an event handler when the saga lands)
    @Transactional
    public void fail(TopupId id) {
        TopupRequest topup = load(id);
        topup.fail("rail reported failure"); // nothing to undo — the user was never credited
        topups.save(topup);
        publishEvents(topup);
    }

    @Transactional(readOnly = true)
    public TopupRequest get(TopupId id) {
        return load(id);
    }

    private TopupRequest load(TopupId id) {
        return topups.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No topup " + id.value()));
    }

    private void publishEvents(TopupRequest topup) {
        topup.pullEvents().forEach(eventPublisher::publishEvent);
    }
}
