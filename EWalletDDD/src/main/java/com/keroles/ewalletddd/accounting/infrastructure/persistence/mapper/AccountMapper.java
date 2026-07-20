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

    
    public static void copyOnto(Account account, AccountJpaEntity row, UserJpaEntity userRef) {
        
        row.setAccountReference(account.reference().value()); 
        row.setUser(userRef);
        row.setCurrency(account.currency().code());
        row.setAccountType(account.type().name().toLowerCase()); 
        row.setBalance(account.balance().amount());
        row.setHoldBalance(account.holdBalance().amount());
    }
}
