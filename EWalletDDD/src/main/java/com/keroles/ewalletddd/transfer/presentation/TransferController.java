package com.keroles.ewalletddd.transfer.presentation;

import com.keroles.ewalletddd.transfer.application.TransferApplicationService;
import com.keroles.ewalletddd.transfer.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.transfer.domain.valueObject.TransferId;
import com.keroles.ewalletddd.transfer.presentation.requests.CreateTransferRequest;
import com.keroles.ewalletddd.transfer.presentation.responses.TransferResponse;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.Money;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferApplicationService transferService;

    public TransferController(TransferApplicationService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse request(@RequestBody CreateTransferRequest request) {
        TransferId id = transferService.requestTransfer(
                new LedgerAccountRef(request.fromAccountId()),
                new LedgerAccountRef(request.toAccountId()),
                new Money(request.amount(), Currency.of(request.currency())));
        return TransferResponse.from(transferService.get(id));
    }

    @GetMapping("/{id}")
    public TransferResponse get(@PathVariable String id) {
        return TransferResponse.from(transferService.get(new TransferId(UUID.fromString(id))));
    }
}
