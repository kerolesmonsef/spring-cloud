package com.keroles.ewalletddd.topup.application;

import com.keroles.ewalletddd.topup.domain.model.TopupRequest;
import com.keroles.ewalletddd.topup.domain.port.LedgerTopupPort;
import com.keroles.ewalletddd.topup.domain.port.RailDispatchResult;
import com.keroles.ewalletddd.topup.domain.port.TopupRailPort;
import com.keroles.ewalletddd.topup.domain.port.TopupRailRegistry;
import com.keroles.ewalletddd.topup.domain.repository.TopupRepository;
import com.keroles.ewalletddd.topup.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.topup.domain.valueObject.LedgerTransactionRef;
import com.keroles.ewalletddd.topup.domain.valueObject.Rail;
import com.keroles.ewalletddd.topup.domain.valueObject.TopupId;
import com.keroles.ewalletddd.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;





class TopupDuplicateCallbackTest {

    @Test
    void duplicateConfirm_callsLedgerTopupExactlyOnce() {
        CountingLedger ledger = new CountingLedger();
        TopupRailRegistry pendingRail = PendingRail::new;
        InMemoryTopupRepo repo = new InMemoryTopupRepo();
        TopupApplicationService service =
                new TopupApplicationService(repo, ledger, pendingRail, event -> {});

        TopupId id = service.requestTopup(new LedgerAccountRef(7L), Money.of("30.00", "AED"), Rail.TCS);
        service.confirm(id);                                                  
        assertThrows(IllegalStateException.class, () -> service.confirm(id)); 

        assertEquals(2, ledger.credits,
                "ledger dedupes by idempotencyKey — a duplicate callback must not credit again");
        assertEquals(TopupRequest.Status.COMPLETED, repo.findById(id).orElseThrow().status());
    }

    static class CountingLedger implements LedgerTopupPort {
        int credits;
        private final Set<TopupId> seen = new HashSet<>();
        public LedgerTransactionRef topup(LedgerAccountRef account, Money amount) {
            credits++;
            return new LedgerTransactionRef(UUID.randomUUID());
        }
    }

    record PendingRail(Rail rail) implements TopupRailPort {
        public RailDispatchResult dispatch(TopupId id, Money amount) {
            return RailDispatchResult.pending("TCS-" + id.value()); 
        }
    }

    static class InMemoryTopupRepo implements TopupRepository {
        final Map<UUID, TopupRequest> store = new HashMap<>();
        public Optional<TopupRequest> findById(TopupId id) { return Optional.ofNullable(store.get(id.value())); }
        public void save(TopupRequest t) { store.put(t.id().value(), t); }
    }
}
