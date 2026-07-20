package com.keroles.ewalletddd.accounting.domain.valueObject;





public record Party(String reference, AccountType type) {

    
    
    public static final Party EXTERNAL = new Party("EXTERNAL", AccountType.EXTERNAL);

    public static Party internal(AccountReference reference, AccountType type) {
        return new Party(reference.value().toString(), type);
    }
}
