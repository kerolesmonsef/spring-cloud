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
    private final LedgerTopupPort ledger;   
    private final TopupRailRegistry rails;
    private final ApplicationEventPublisher eventPublisher; 

    public TopupApplicationService(TopupRepository topups,
                                   LedgerTopupPort ledger,
                                   TopupRailRegistry rails,
                                   ApplicationEventPublisher eventPublisher) {
        this.topups = topups;
        this.ledger = ledger;
        this.rails = rails;
        this.eventPublisher = eventPublisher;
    }

    
    @Transactional
    public TopupId requestTopup(LedgerAccountRef account, Money amount, Rail rail) {
        TopupRequest topup = TopupRequest.request(account, amount, rail); 
        
        
        RailDispatchResult result = rails.forRail(rail).dispatch(topup.id(), amount);
        switch (result.outcome()) {
            case PENDING -> topup.recordDispatch(result.railReference()); 
            case CONFIRMED -> {                                           
                topup.recordDispatch(result.railReference());
                topup.complete(ledger.topup(account, amount));
            }
            case REJECTED -> topup.fail(result.reason());                
        }
        topups.save(topup);
        publishEvents(topup);
        return topup.id();
    }

    
    @Transactional
    public void confirm(TopupId id) {
        TopupRequest topup = load(id);
        topup.complete(ledger.topup(topup.account(), topup.amount()));
        topups.save(topup);
        publishEvents(topup);
    }

    
    @Transactional
    public void fail(TopupId id) {
        TopupRequest topup = load(id);
        topup.fail("rail reported failure"); 
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
