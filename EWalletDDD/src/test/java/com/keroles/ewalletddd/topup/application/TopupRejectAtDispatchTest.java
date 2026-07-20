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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

// Pure-fakes test for the reject-at-dispatch branch — the only path where a rail refuses up front.
// The Spring IT can't reach it (real fakes return PENDING/CONFIRMED, never REJECTED).
class TopupRejectAtDispatchTest {

    @Test
    void railRejectsAtDispatch_topupFailedAndLedgerNeverTouched() {
        RecordingLedger ledger = new RecordingLedger();
        TopupRailRegistry rejecting = RejectingRail::new;

        InMemoryTopupRepo repo = new InMemoryTopupRepo();
        TopupApplicationService service =
                new TopupApplicationService(repo, ledger, rejecting, event -> {});

        TopupId id = service.requestTopup(new LedgerAccountRef(7L), Money.of("25.00", "AED"), Rail.TCS);

        TopupRequest saved = repo.findById(id).orElseThrow();
        assertEquals(TopupRequest.Status.FAILED, saved.status());
        assertFalse(ledger.credited, "a rejected topup must never credit the Ledger");
    }

    static class RecordingLedger implements LedgerTopupPort {
        boolean credited;
        public LedgerTransactionRef topup(LedgerAccountRef account, Money amount) {
            credited = true;
            return new LedgerTransactionRef(UUID.randomUUID());
        }
    }

    record RejectingRail(Rail rail) implements TopupRailPort {
        public RailDispatchResult dispatch(TopupId id, Money amount) {
            return RailDispatchResult.rejected("rail refused");
        }
    }

    static class InMemoryTopupRepo implements TopupRepository {
        final Map<UUID, TopupRequest> store = new HashMap<>();
        public Optional<TopupRequest> findById(TopupId id) { return Optional.ofNullable(store.get(id.value())); }
        public void save(TopupRequest t) { store.put(t.id().value(), t); }
    }
}
