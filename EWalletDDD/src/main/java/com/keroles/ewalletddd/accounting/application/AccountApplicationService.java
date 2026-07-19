package com.keroles.ewalletddd.accounting.application;

import com.keroles.ewalletddd.accounting.domain.event.AccountOpenedEvent;
import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.model.User;
import com.keroles.ewalletddd.accounting.domain.repository.AccountRepository;
import com.keroles.ewalletddd.accounting.domain.repository.UserRepository;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.UserId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Account lifecycle + queries. Money movements live in TransactionApplicationService.
@Service
public class AccountApplicationService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher; // ponytail: in-process events; Outbox pattern when we need reliability across services

    public AccountApplicationService(AccountRepository accounts,
                                     UserRepository users,
                                     ApplicationEventPublisher eventPublisher) {
        this.accountRepository = accounts;
        this.userRepository = users;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AccountId openAccount(UserId userId, Currency currency) {
        UserId owner = (userId == null) ? registerUser() : existingUser(userId);
        accountRepository.findByUserAndCurrency(owner, currency).ifPresent(existing -> {
            throw new IllegalStateException("User already has a " + currency + " account"); // TODO is this legal to throw here ?
        });
        Account account = Account.open(owner, currency);
        accountRepository.save(account); // adapter assigns the auto-increment id back onto the aggregate
        // raised here, not in Account.open(): the id is born in the DB, so the event can't exist before save
        eventPublisher.publishEvent(new AccountOpenedEvent(account.id(), owner, currency));
        publishEvents(account);
        return account.id();
    }

    @Transactional(readOnly = true)
    public Account getAccount(AccountId accountId) {
        return loadAccount(accountId);
    }

    @Transactional(readOnly = true)
    public List<Account> getUserAccounts(UserId userId) {
        return accountRepository.findByUser(userId);
    }

    private Account loadAccount(AccountId id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No account " + id.value()));
    }

    private void publishEvents(Account account) {
        account.pullEvents().forEach(eventPublisher::publishEvent);
    }

    private UserId registerUser() {
        User user = User.register();
        userRepository.save(user);
        return user.id();
    }

    private UserId existingUser(UserId userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("No user " + userId.value()))
                .id();
    }
}
