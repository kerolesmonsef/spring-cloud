package com.keroles.ewalletddd.cashout.presentation;

import com.keroles.ewalletddd.cashout.application.CashoutApplicationService;
import com.keroles.ewalletddd.cashout.domain.valueObject.CashoutId;
import com.keroles.ewalletddd.cashout.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.cashout.domain.valueObject.Rail;
import com.keroles.ewalletddd.cashout.presentation.requests.CreateCashoutRequest;
import com.keroles.ewalletddd.cashout.presentation.responses.CashoutResponse;
import com.keroles.ewalletddd.shared.domain.Money;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Currency;
import java.util.UUID;

@RestController
@RequestMapping("/cashouts")
public class CashoutController {

    private final CashoutApplicationService cashoutService;

    public CashoutController(CashoutApplicationService cashoutService) {
        this.cashoutService = cashoutService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CashoutResponse request(@RequestBody CreateCashoutRequest request) {
        CashoutId id = cashoutService.requestCashout(
                new LedgerAccountRef(request.accountId()),
                new Money(request.amount(), Currency.getInstance(request.currency())),
                Rail.valueOf(request.rail().toUpperCase()));
        return CashoutResponse.from(cashoutService.get(id));
    }

    @GetMapping("/{id}")
    public CashoutResponse get(@PathVariable String id) {
        return CashoutResponse.from(cashoutService.get(new CashoutId(UUID.fromString(id))));
    }

    // simulate the async rail outcome (temporary — becomes webhook-driven in step 4)
    @PostMapping("/{id}/confirm")
    public CashoutResponse confirm(@PathVariable String id) {
        CashoutId cashoutId = new CashoutId(UUID.fromString(id));
        cashoutService.confirm(cashoutId);
        return CashoutResponse.from(cashoutService.get(cashoutId));
    }

    @PostMapping("/{id}/fail")
    public CashoutResponse fail(@PathVariable String id) {
        CashoutId cashoutId = new CashoutId(UUID.fromString(id));
        cashoutService.fail(cashoutId);
        return CashoutResponse.from(cashoutService.get(cashoutId));
    }
}
