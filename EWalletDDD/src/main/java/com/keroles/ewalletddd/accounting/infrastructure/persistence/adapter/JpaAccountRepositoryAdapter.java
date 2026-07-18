package com.keroles.ewalletddd.accounting.infrastructure.persistence.adapter;
  import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.AccountJpaEntity; import com.keroles.ewalletddd.accounting.infrastructure.persistence.mapper.AccountMapper; import com.keroles.ewalletddd.accounting.infrastructure.persistence.repository.SpringDataAccountJpa; import com.keroles.ewalletddd.accounting.infrastructure.persistence.repository.SpringDataUserJpa;

import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.repository.AccountRepository;
import com.keroles.ewalletddd.shared.domain.UserId;
import org.springframework.stereotype.Component;

import java.util.Currency;
import java.util.List;
import java.util.Optional;

/** Implements the domain port. Option B save: load-then-copy onto the managed entity — domain stays version-free. */
@Component
public class JpaAccountRepositoryAdapter implements AccountRepository {

    private final SpringDataAccountJpa jpa;
    private final SpringDataUserJpa userJpa;

    public JpaAccountRepositoryAdapter(SpringDataAccountJpa jpa, SpringDataUserJpa userJpa) {
        this.jpa = jpa;
        this.userJpa = userJpa;
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        return jpa.findById(id.value()).map(AccountMapper::toDomain);
    }

    @Override
    public Optional<Account> findByUserAndCurrency(UserId userId, Currency currency) {
        return jpa.findByUserIdAndCurrency(userId.value(), currency.getCurrencyCode())
                .map(AccountMapper::toDomain);
    }

    @Override
    public List<Account> findByUser(UserId userId) {
        return jpa.findByUserId(userId.value()).stream().map(AccountMapper::toDomain).toList();
    }

    @Override
    public void save(Account account) {
        AccountJpaEntity row = (account.id() == null)
                ? new AccountJpaEntity()             // new aggregate -> INSERT, DB generates id
                : jpa.findById(account.id().value())
                      .orElseThrow(() -> new IllegalStateException("Account row vanished: " + account.id().value()));
        // getReferenceById = proxy holding only the id; sets FK without SELECTing the user
        AccountMapper.copyOnto(account, row, userJpa.getReferenceById(account.userId().value()));
        AccountJpaEntity saved = jpa.save(row);
        if (account.id() == null) {
            account.assignId(new AccountId(saved.getId())); // hand the DB-born identity back to the aggregate
        }
    }
}
