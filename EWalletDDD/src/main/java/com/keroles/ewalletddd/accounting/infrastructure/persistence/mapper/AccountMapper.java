package com.keroles.ewalletddd.accounting.infrastructure.persistence.mapper;
  import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.AccountJpaEntity; import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.UserJpaEntity;

import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountReference;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountType;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.Money;
import com.keroles.ewalletddd.shared.domain.UserId;

public final class AccountMapper {

    private AccountMapper() {}

    public static Account toDomain(AccountJpaEntity row) {
        Currency currency = Currency.of(row.getCurrency());
        // legacy rows (pre-account-type) have null -> default USER
        AccountType type = row.getAccountType() == null
                ? AccountType.USER
                : AccountType.valueOf(row.getAccountType().toUpperCase());
        return Account.restore(
                new AccountId(row.getId()),
                new AccountReference(row.getAccountReference()),
                new UserId(row.getUserId()),
                currency,
                type,
                new Money(row.getBalance(), currency),
                new Money(row.getHoldBalance(), currency));
    }

    // copies onto a MANAGED entity so Hibernate dirty-checking issues an UPDATE and @Version increments; userRef is a proxy, no user SELECT
    public static void copyOnto(Account account, AccountJpaEntity row, UserJpaEntity userRef) {
        // id NOT copied — auto-increment, owned by the DB
        row.setAccountReference(account.reference().value()); // updatable=false, so only the INSERT persists it
        row.setUser(userRef);
        row.setCurrency(account.currency().code());
        row.setAccountType(account.type().name().toLowerCase()); // matches a_account_types.name; FK ref set in the adapter
        row.setBalance(account.balance().amount());
        row.setHoldBalance(account.holdBalance().amount());
    }
}
