package com.keroles.ewalletddd.cashout.application;

import com.keroles.ewalletddd.cashout.domain.model.CashoutRequest;
import com.keroles.ewalletddd.cashout.domain.port.LedgerAccountPort;
import com.keroles.ewalletddd.cashout.domain.port.CashoutRailRegistry;
import com.keroles.ewalletddd.cashout.domain.port.RailDispatchResult;
import com.keroles.ewalletddd.cashout.domain.repository.CashoutRepository;
import com.keroles.ewalletddd.cashout.domain.valueObject.*;
import com.keroles.ewalletddd.shared.domain.Money;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashoutApplicationService {

    private final CashoutRepository cashouts;
    private final LedgerAccountPort ledger;   
    private final CashoutRailRegistry rails;
    private final ApplicationEventPublisher eventPublisher; 

    public CashoutApplicationService(CashoutRepository cashouts,
                                     LedgerAccountPort ledger,
                                     CashoutRailRegistry rails,
                                     ApplicationEventPublisher eventPublisher) {
        this.cashouts = cashouts;
        this.ledger = ledger;
        this.rails = rails;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CashoutId requestCashout(LedgerAccountRef account, Money amount, Rail rail) {
        LedgerReservationRef reservation = ledger.reserve(account, amount); 
        CashoutRequest cashout = CashoutRequest.request(account, amount, rail, reservation);
        
        
        RailDispatchResult result = rails.forRail(rail).dispatch(cashout.id(), amount);
        switch (result.outcome()) {
            case PENDING -> cashout.markDispatched(result.railReference()); 
            case CONFIRMED -> {                                             
                cashout.markDispatched(result.railReference());
                cashout.confirm(ledger.settle(reservation));
            }
            case REJECTED -> {
                cashout.fail();
                ledger.release(reservation); 
            }
        }
        cashouts.save(cashout);
        publishEvents(cashout);
        return cashout.id();
    }

    
    @Transactional
    public void confirm(CashoutId id) {
        CashoutRequest cashout = load(id);
        cashout.confirm(ledger.settle(cashout.reservationRef())); 
        cashouts.save(cashout);
        publishEvents(cashout);
    }

    
    @Transactional
    public void fail(CashoutId id) {
        CashoutRequest cashout = load(id);
        cashout.fail();
        ledger.release(cashout.reservationRef()); 
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
