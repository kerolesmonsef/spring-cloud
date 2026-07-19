package com.keroles.ewalletddd.accounting.presentation;

import com.keroles.ewalletddd.accounting.application.AccountApplicationService;
import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.presentation.requests.MoneyRequest;
import com.keroles.ewalletddd.accounting.presentation.requests.OpenAccountRequest;
import com.keroles.ewalletddd.accounting.presentation.responses.AccountResponse;
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

import java.util.Currency;
import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountApplicationService accountService;

    public AccountController(AccountApplicationService accountService) {
        this.accountService = accountService;
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
    public AccountResponse get(@PathVariable Long id) {
        return AccountResponse.from(accountService.getAccount(new AccountId(id)));
    }

    @GetMapping("/user/{userId}")
    public List<AccountResponse> byUser(@PathVariable Long userId) {
        return accountService.getUserAccounts(new UserId(userId))
                .stream().map(AccountResponse::from).toList();
    }

    @PostMapping("/{id}/deposit")
    public AccountResponse deposit(@PathVariable Long id, @RequestBody MoneyRequest request) {
        AccountId accountId = new AccountId(id);
        Account account = accountService.getAccount(accountId);
        accountService.deposit(accountId, new Money(request.amount(), account.currency()));
        return AccountResponse.from(accountService.getAccount(accountId));
    }

    @PostMapping("/{id}/withdraw")
    public AccountResponse withdraw(@PathVariable Long id, @RequestBody MoneyRequest request) {
        AccountId accountId = new AccountId(id);
        Account account = accountService.getAccount(accountId);
        accountService.withdraw(accountId, new Money(request.amount(), account.currency()));
        return AccountResponse.from(accountService.getAccount(accountId));
    }
}
