package com.keroles.ewalletddd.cashout.application;

import com.keroles.ewalletddd.cashout.domain.model.CashoutRequest;
import com.keroles.ewalletddd.cashout.domain.port.LedgerAccountPort;
import com.keroles.ewalletddd.cashout.domain.port.CashoutRailPort;
import com.keroles.ewalletddd.cashout.domain.port.CashoutRailRegistry;
import com.keroles.ewalletddd.cashout.domain.port.RailDispatchResult;
import com.keroles.ewalletddd.cashout.domain.repository.CashoutRepository;
import com.keroles.ewalletddd.cashout.domain.valueObject.*;
import com.keroles.ewalletddd.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;



class CashoutRejectAtDispatchTest {

    @Test
    void railRejectsAtDispatch_cashoutFailedAndHoldReleasedNotSettled() {
        RecordingLedger ledger = new RecordingLedger();
        CashoutRailRegistry rejecting = RejectingRail::new;

        InMemoryCashoutRepo repo = new InMemoryCashoutRepo();
        CashoutApplicationService service =
                new CashoutApplicationService(repo, ledger, rejecting, event -> {});

        CashoutId id = service.requestCashout(new LedgerAccountRef(7L), Money.of("25.00", "AED"), Rail.AANI);

        CashoutRequest saved = repo.findById(id).orElseThrow();
        assertEquals(CashoutRequest.Status.FAILED, saved.status());
        assertTrue(ledger.released, "hold must be given back when the rail rejects");
        assertFalse(ledger.settled, "a rejected cashout must never settle");
        assertEquals(ledger.reservation, ledger.releasedRef, "released the reservation we reserved");
    }

    static class RecordingLedger implements LedgerAccountPort {
        final LedgerReservationRef reservation = new LedgerReservationRef(UUID.randomUUID());
        boolean released, settled;
        LedgerReservationRef releasedRef;
        public LedgerReservationRef reserve(LedgerAccountRef account, Money amount) { return reservation; }
        public LedgerSettleRef settle(LedgerReservationRef r) { settled = true; return new LedgerSettleRef(UUID.randomUUID()); }
        public void release(LedgerReservationRef r) { released = true; releasedRef = r; }
    }

    record RejectingRail(Rail rail) implements CashoutRailPort {
        public RailDispatchResult dispatch(CashoutId id, Money amount) {
            return RailDispatchResult.rejected("rail refused");
        }
    }

    static class InMemoryCashoutRepo implements CashoutRepository {
        final Map<UUID, CashoutRequest> store = new HashMap<>();
        public Optional<CashoutRequest> findById(CashoutId id) { return Optional.ofNullable(store.get(id.value())); }
        public void save(CashoutRequest c) { store.put(c.id().value(), c); }
    }
}
