package com.keroles.ewalletddd.transfer.application;

import com.keroles.ewalletddd.transfer.domain.model.Transfer;
import com.keroles.ewalletddd.transfer.domain.port.LedgerTransferPort;
import com.keroles.ewalletddd.transfer.domain.repository.TransferRepository;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerHoldRef;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerSettleRef;
import com.keroles.ewalletddd.transfer.domain.valueObject.TransferId;
import com.keroles.ewalletddd.shared.domain.Money;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferApplicationService {

    private final TransferRepository transfers;
    private final LedgerTransferPort ledger;
    private final ApplicationEventPublisher eventPublisher;

    public TransferApplicationService(TransferRepository transfers,
                                      LedgerTransferPort ledger,
                                      ApplicationEventPublisher eventPublisher) {
        this.transfers = transfers;
        this.ledger = ledger;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TransferId requestTransfer(LedgerAccountRef fromAccount, LedgerAccountRef toAccount, Money amount) {
        if (fromAccount.equals(toAccount))
            throw new IllegalArgumentException("Cannot transfer to the same account");

        LedgerHoldRef hold = ledger.hold(fromAccount, toAccount, amount);
        LedgerSettleRef settle = ledger.settle(hold);
        Transfer transfer = Transfer.complete(fromAccount, toAccount, amount, hold, settle);
        transfers.save(transfer);
        publishEvents(transfer);
        return transfer.id();
    }

    @Transactional(readOnly = true)
    public Transfer get(TransferId id) {
        return load(id);
    }

    private Transfer load(TransferId id) {
        return transfers.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No transfer " + id.value()));
    }

    private void publishEvents(Transfer transfer) {
        transfer.pullEvents().forEach(eventPublisher::publishEvent);
    }
}
