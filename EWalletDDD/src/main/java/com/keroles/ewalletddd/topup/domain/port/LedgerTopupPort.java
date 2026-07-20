package com.keroles.ewalletddd.topup.domain.port;

import com.keroles.ewalletddd.topup.domain.valueObject.LedgerAccountRef;
import com.keroles.ewalletddd.topup.domain.valueObject.LedgerTransactionRef;
import com.keroles.ewalletddd.shared.domain.Money;

// Driven port: the Ledger front door in Topup's terms. Implemented by the ACL adapter.
// One-shot credit (system -> user) — no reserve/settle/release, because Topup holds nothing.
public interface LedgerTopupPort {
    LedgerTransactionRef topup(LedgerAccountRef account, Money amount);
}
