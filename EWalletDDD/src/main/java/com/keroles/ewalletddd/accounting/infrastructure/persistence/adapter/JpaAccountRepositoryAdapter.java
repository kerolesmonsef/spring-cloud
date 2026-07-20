package com.keroles.ewalletddd.accounting.infrastructure.persistence.adapter;
  import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.AccountJpaEntity; import com.keroles.ewalletddd.accounting.infrastructure.persistence.mapper.AccountMapper; import com.keroles.ewalletddd.accounting.infrastructure.persistence.repository.SpringDataAccountJpa; import com.keroles.ewalletddd.accounting.infrastructure.persistence.repository.SpringDataUserJpa;
import com.keroles.ewalletddd.accounting.infrastructure.reference.AccountTypeRepository;
import com.keroles.ewalletddd.accounting.infrastructure.reference.CurrencyRepository;

import com.keroles.ewalletddd.accounting.domain.model.Account;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountId;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountReference;
import com.keroles.ewalletddd.accounting.domain.valueObject.AccountType;
import com.keroles.ewalletddd.accounting.domain.repository.AccountRepository;
import com.keroles.ewalletddd.shared.domain.Currency;
import com.keroles.ewalletddd.shared.domain.UserId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;


@Component
public class JpaAccountRepositoryAdapter implements AccountRepository {

    private final SpringDataAccountJpa jpa;
    private final SpringDataUserJpa userJpa;
    private final CurrencyRepository currencyJpa;
    private final AccountTypeRepository accountTypeJpa;

    public JpaAccountRepositoryAdapter(SpringDataAccountJpa jpa, SpringDataUserJpa userJpa,
                                       CurrencyRepository currencyJpa, AccountTypeRepository accountTypeJpa) {
        this.jpa = jpa;
        this.userJpa = userJpa;
        this.currencyJpa = currencyJpa;
        this.accountTypeJpa = accountTypeJpa;
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        return jpa.findById(id.value()).map(AccountMapper::toDomain);
    }

    @Override
    public Optional<Account> findByReference(AccountReference reference) {
        return jpa.findByAccountReference(reference.value()).map(AccountMapper::toDomain);
    }

    @Override
    public Optional<Account> findByUserAndCurrency(UserId userId, Currency currency) {
        return jpa.findByUserIdAndCurrency(userId.value(), currency.code())
                .map(AccountMapper::toDomain);
    }

    @Override
    public Optional<Account> findByTypeAndCurrency(AccountType type, Currency currency) {
        return jpa.findByAccountTypeAndCurrency(type.name().toLowerCase(), currency.code())
                .map(AccountMapper::toDomain);
    }

    @Override
    public List<Account> findByUser(UserId userId) {
        return jpa.findByUserId(userId.value()).stream().map(AccountMapper::toDomain).toList();
    }

    @Override
    public void save(Account account) {
        boolean isNew = account.id() == null;
        AccountJpaEntity row = isNew
                ? new AccountJpaEntity()             
                : jpa.findById(account.id().value())
                      .orElseThrow(() -> new IllegalStateException("Account row vanished: " + account.id().value()));
        
        AccountMapper.copyOnto(account, row, userJpa.getReferenceById(account.userId().value()));
        if (isNew) {
            
            linkReferenceData(account, row);
        }
        AccountJpaEntity saved = jpa.save(row);
        if (isNew) {
            account.assignId(new AccountId(saved.getId())); 
            
            
            
            saved.setUserId(account.userId().value());
        }
    }

    private void linkReferenceData(Account account, AccountJpaEntity row) {
        String code = account.currency().code();
        row.setCurrencyRef(currencyJpa.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported currency: " + code)));
        String typeName = account.type().name().toLowerCase();
        row.setAccountTypeRef(accountTypeJpa.findByName(typeName)
                .orElseThrow(() -> new IllegalStateException("Missing account type row: " + typeName)));
    }
}
