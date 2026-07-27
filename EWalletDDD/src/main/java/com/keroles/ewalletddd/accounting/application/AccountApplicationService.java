package com.keroles.ewalletddd.accounting.application;

import com.keroles.ewalletddd.accounting.domain.event.AccountOpenedEvent;
import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountReference;
import com.keroles.ewalletddd.accounting.domain.model.User;
import com.keroles.ewalletddd.accounting.domain.repository.AccountRepository;
import com.keroles.ewalletddd.accounting.domain.repository.UserRepository;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class AccountApplicationService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher; 


    @Transactional
    public AccountReference openAccount(UserId userId, Currency currency) {
        UserId owner = (userId == null) ? registerUser() : existingUser(userId);
        accountRepository.findByUserAndCurrency(owner, currency).ifPresent(existing -> {
            throw new IllegalStateException("User already has a " + currency + " account");
        });
        Account account = Account.open(owner, currency);
        accountRepository.save(account);

        eventPublisher.publishEvent(new AccountOpenedEvent(account.id(), owner, currency));
        publishEvents(account);
        return account.reference();
    }

    @Transactional(readOnly = true)
    public Account getAccount(AccountReference reference) {
        return accountRepository.findByReference(reference)
                .orElseThrow(() -> new IllegalArgumentException("No account " + reference.value()));
    }

    @Transactional(readOnly = true)
    public List<Account> getUserAccounts(UserId userId) {
        return accountRepository.findByUser(userId);
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
