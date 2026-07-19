package com.keroles.ewalletddd.accounting.domain.repository;

import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountType;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.UserId;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findById(AccountId id);
    Optional<Account> findByUserAndCurrency(UserId userId, Currency currency);
    Optional<Account> findByTypeAndCurrency(AccountType type, Currency currency); // resolve the house account for a currency
    List<Account> findByUser(UserId userId);
    void save(Account account);
}
