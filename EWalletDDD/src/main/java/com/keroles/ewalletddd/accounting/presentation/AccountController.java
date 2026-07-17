package com.keroles.ewalletddd.accounting.presentation;

import com.keroles.ewalletddd.accounting.application.AccountApplicationService;
import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.model.AccountId;
import com.keroles.ewalletddd.shared.domain.Money;
import com.keroles.ewalletddd.shared.domain.UserId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

/**
 * Presentation edge: HTTP shapes in, HTTP shapes out. Translates primitives <-> VOs
 * and delegates to the front door. Zero business rules.
 *
 * reserve/settle/release are NOT exposed here — they're the internal contract for the
 * Cashout context, not a public API.
 */
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountApplicationService accountService;

    public AccountController(AccountApplicationService accountService) {
        this.accountService = accountService;
    }

    /** userId optional: omit it to register a new user together with the account. */
    public record OpenAccountRequest(UUID userId, String currency) {}
    public record MoneyRequest(BigDecimal amount) {}
    public record AccountResponse(UUID id, UUID userId, String currency,
                                  BigDecimal balance, BigDecimal holdBalance) {
        static AccountResponse from(Account account) {
            return new AccountResponse(
                    account.id().value(),
                    account.userId().value(),
                    account.currency().getCurrencyCode(),
                    account.balance().amount(),
                    account.holdBalance().amount());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse open(@RequestBody OpenAccountRequest request) {
        AccountId id = accountService.openAccount(
                request.userId() == null ? null : new UserId(request.userId()),
                Currency.getInstance(request.currency()));
        return AccountResponse.from(accountService.getAccount(id));
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable UUID id) {
        return AccountResponse.from(accountService.getAccount(new AccountId(id)));
    }

    @GetMapping("/user/{userId}")
    public List<AccountResponse> byUser(@PathVariable UUID userId) {
        return accountService.getUserAccounts(new UserId(userId))
                .stream().map(AccountResponse::from).toList();
    }

    @PostMapping("/{id}/deposit")
    public AccountResponse deposit(@PathVariable UUID id, @RequestBody MoneyRequest request) {
        AccountId accountId = new AccountId(id);
        Account account = accountService.getAccount(accountId);
        accountService.deposit(accountId, new Money(request.amount(), account.currency()));
        return AccountResponse.from(accountService.getAccount(accountId));
    }

    @PostMapping("/{id}/withdraw")
    public AccountResponse withdraw(@PathVariable UUID id, @RequestBody MoneyRequest request) {
        AccountId accountId = new AccountId(id);
        Account account = accountService.getAccount(accountId);
        accountService.withdraw(accountId, new Money(request.amount(), account.currency()));
        return AccountResponse.from(accountService.getAccount(accountId));
    }
}
