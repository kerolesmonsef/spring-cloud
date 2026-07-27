package com.keroles.ewalletddd.transfer.application;

import com.keroles.ewalletddd.transfer.domain.model.Transfer;
import com.keroles.ewalletddd.transfer.domain.port.LedgerTransferPort;
import com.keroles.ewalletddd.transfer.domain.port.PricingPort;
import com.keroles.ewalletddd.transfer.domain.repository.TransferRepository;
import com.keroles.ewalletddd.transfer.domain.valueObject.*;
import com.keroles.ewalletddd.shared.domain.Money;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransferApplicationService {

    private final TransferRepository transfers;
    private final LedgerTransferPort ledger;
    private final PricingPort pricing;
    private final ApplicationEventPublisher eventPublisher;

    public TransferApplicationService(TransferRepository transfers,
                                      LedgerTransferPort ledger,
                                      PricingPort pricing,
                                      ApplicationEventPublisher eventPublisher) {
        this.transfers = transfers;
        this.ledger = ledger;
        this.pricing = pricing;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TransferId requestTransfer(LedgerAccountReference fromAccount, LedgerAccountReference toAccount, Money amount) {
        if (fromAccount.equals(toAccount))
            throw new IllegalArgumentException("Cannot transfer to the same account");

        FeeQuote feeResult = pricing.calculateTransferFees(amount);
        LedgerHoldRef hold = ledger.hold(fromAccount, toAccount, feeResult.senderTotalAmount());

        LedgerAccountReference feeAccount = new LedgerAccountReference("FEE");
        LedgerAccountReference vatAccount = new LedgerAccountReference("VAT");
        List<FeeLeg> legs = List.of(
                new FeeLeg(fromAccount, toAccount, feeResult.senderNetAmount()),
                new FeeLeg(fromAccount, feeAccount, feeResult.senderFeeValue()),
                new FeeLeg(fromAccount, vatAccount, feeResult.senderVatValue()),
                new FeeLeg(toAccount, feeAccount, feeResult.receiverFeeValue()),
                new FeeLeg(toAccount, vatAccount, feeResult.receiverVatValue()));
        LedgerSettleRef settle = ledger.settle(hold, amount, legs);
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
