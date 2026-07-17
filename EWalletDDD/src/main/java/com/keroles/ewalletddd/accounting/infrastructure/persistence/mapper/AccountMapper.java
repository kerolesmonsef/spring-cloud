package com.keroles.ewalletddd.accounting.infrastructure.persistence.mapper;
  import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.AccountJpaEntity; import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.UserJpaEntity;

import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.model.AccountId;
import com.keroles.ewalletddd.shared.domain.Money;
import com.keroles.ewalletddd.shared.domain.UserId;

import java.util.Currency;

/** Border plumbing: row <-> aggregate. The only place setBalance() is ever called. */
public final class AccountMapper {

    private AccountMapper() {}

    public static Account toDomain(AccountJpaEntity row) {
        Currency currency = Currency.getInstance(row.getCurrency());
        return Account.restore(
                new AccountId(row.getId()),
                new UserId(row.getUserId()),
                currency,
                new Money(row.getBalance(), currency),
                new Money(row.getHoldBalance(), currency));
    }

    /**
     * Copies aggregate state onto a MANAGED entity so Hibernate dirty-checking issues an UPDATE and @Version increments.
     * userRef: reference proxy for the FK association — caller supplies it, no user SELECT happens.
     */
    public static void copyOnto(Account account, AccountJpaEntity row, UserJpaEntity userRef) {
        row.setId(account.id().value());
        row.setUser(userRef);
        row.setCurrency(account.currency().getCurrencyCode());
        row.setBalance(account.balance().amount());
        row.setHoldBalance(account.holdBalance().amount());
    }
}
