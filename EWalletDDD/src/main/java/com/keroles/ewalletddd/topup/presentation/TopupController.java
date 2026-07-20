package com.keroles.ewalletddd.topup.presentation;

import com.keroles.ewalletddd.topup.application.TopupApplicationService;
import com.keroles.ewalletddd.topup.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.topup.domain.valueObject.Rail;
import com.keroles.ewalletddd.topup.domain.valueObject.TopupId;
import com.keroles.ewalletddd.topup.presentation.requests.CreateTopupRequest;
import com.keroles.ewalletddd.topup.presentation.responses.TopupResponse;
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
@RequestMapping("/topups")
public class TopupController {

    private final TopupApplicationService topupService;

    public TopupController(TopupApplicationService topupService) {
        this.topupService = topupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TopupResponse request(@RequestBody CreateTopupRequest request) {
        TopupId id = topupService.requestTopup(
                new LedgerAccountRef(request.accountId()),
                new Money(request.amount(), Currency.of(request.currency())),
                Rail.valueOf(request.rail().toUpperCase()));
        return TopupResponse.from(topupService.get(id));
    }

    @GetMapping("/{id}")
    public TopupResponse get(@PathVariable String id) {
        return TopupResponse.from(topupService.get(new TopupId(UUID.fromString(id))));
    }

    // simulate the async rail outcome (temporary — becomes webhook-driven when the saga lands)
    @PostMapping("/{id}/confirm")
    public TopupResponse confirm(@PathVariable String id) {
        TopupId topupId = new TopupId(UUID.fromString(id));
        topupService.confirm(topupId);
        return TopupResponse.from(topupService.get(topupId));
    }

    @PostMapping("/{id}/fail")
    public TopupResponse fail(@PathVariable String id) {
        TopupId topupId = new TopupId(UUID.fromString(id));
        topupService.fail(topupId);
        return TopupResponse.from(topupService.get(topupId));
    }
}
