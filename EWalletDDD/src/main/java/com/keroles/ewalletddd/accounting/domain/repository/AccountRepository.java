package com.keroles.ewalletddd.accounting.domain.repository;

import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.model.AccountId;
import com.keroles.ewalletddd.shared.domain.UserId;

import java.util.Currency;
import java.util.List;
import java.util.Optional;

/** One repository per aggregate root, expressed in domain language. Port — infrastructure implements it. */
public interface AccountRepository {
    Optional<Account> findById(AccountId id);
    Optional<Account> findByUserAndCurrency(UserId userId, Currency currency);
    List<Account> findByUser(UserId userId);
    void save(Account account);
}
