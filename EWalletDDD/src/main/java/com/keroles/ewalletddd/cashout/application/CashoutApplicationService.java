package com.keroles.ewalletddd.cashout.application;

import com.keroles.ewalletddd.cashout.domain.model.CashoutRequest;
import com.keroles.ewalletddd.cashout.domain.port.LedgerAccountPort;
import com.keroles.ewalletddd.cashout.domain.port.PayoutRailRegistry;
import com.keroles.ewalletddd.cashout.domain.port.RailDispatchResult;
import com.keroles.ewalletddd.cashout.domain.repository.CashoutRepository;
import com.keroles.ewalletddd.cashout.domain.valueObject.CashoutId;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerReservationRef;
import com.keroles.ewalletddd.cashout.domain.valueObject.Rail;
import com.keroles.ewalletddd.shared.domain.Money;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashoutApplicationService {

    private final CashoutRepository cashouts;
    private final LedgerAccountPort ledger;   // ACL to the Accounting front door
    private final PayoutRailRegistry rails;
    private final ApplicationEventPublisher eventPublisher; // ponytail: in-process; Outbox in step 5

    public CashoutApplicationService(CashoutRepository cashouts,
                                     LedgerAccountPort ledger,
                                     PayoutRailRegistry rails,
                                     ApplicationEventPublisher eventPublisher) {
        this.cashouts = cashouts;
        this.ledger = ledger;
        this.rails = rails;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CashoutId requestCashout(LedgerAccountRef account, Money amount, Rail rail) {
        LedgerReservationRef reservation = ledger.reserve(account, amount); // hold on the Ledger
        CashoutRequest cashout = CashoutRequest.request(account, amount, rail, reservation);
        // ponytail: dispatch runs inside the reserve tx because the fake rail is in-process.
        // Real rails dispatch AFTER commit via the Outbox (step 5) — don't hold a tx open across an external call.
        RailDispatchResult result = rails.forRail(rail).dispatch(cashout.id(), amount);
        if (result.accepted()) {
            cashout.markDispatched(result.railReference());
        } else {
            cashout.fail();
            ledger.release(reservation); // rail refused up front — give the hold back
        }
        cashouts.save(cashout);
        publishEvents(cashout);
        return cashout.id();
    }

    // simulates the RailConfirmed callback (becomes an event handler in step 4)
    @Transactional
    public void confirm(CashoutId id) {
        CashoutRequest cashout = load(id);
        cashout.confirm();
        ledger.settle(cashout.reservationRef()); // hold -> 0
        cashouts.save(cashout);
        publishEvents(cashout);
    }

    // simulates the RailFailed callback (becomes an event handler in step 4)
    @Transactional
    public void fail(CashoutId id) {
        CashoutRequest cashout = load(id);
        cashout.fail();
        ledger.release(cashout.reservationRef()); // hold -> main
        cashouts.save(cashout);
        publishEvents(cashout);
    }

    @Transactional(readOnly = true)
    public CashoutRequest get(CashoutId id) {
        return load(id);
    }

    private CashoutRequest load(CashoutId id) {
        return cashouts.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No cashout " + id.value()));
    }

    private void publishEvents(CashoutRequest cashout) {
        cashout.pullEvents().forEach(eventPublisher::publishEvent);
    }
}
