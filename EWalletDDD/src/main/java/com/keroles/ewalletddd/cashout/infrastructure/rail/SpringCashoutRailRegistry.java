package com.keroles.ewalletddd.cashout.infrastructure.rail;

import com.keroles.ewalletddd.cashout.domain.port.CashoutRailPort;
import com.keroles.ewalletddd.cashout.domain.port.CashoutRailRegistry;
import com.keroles.ewalletddd.cashout.domain.valueObject.Rail;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Component
public class SpringCashoutRailRegistry implements CashoutRailRegistry {

    private final Map<Rail, CashoutRailPort> byRail;

    public SpringCashoutRailRegistry(List<CashoutRailPort> adapters) {
        this.byRail = adapters.stream().collect(Collectors.toMap(CashoutRailPort::rail, Function.identity()));
    }

    @Override
    public CashoutRailPort forRail(Rail rail) {
        CashoutRailPort port = byRail.get(rail);
        if (port == null) throw new IllegalArgumentException("No cashout adapter for rail " + rail);
        return port;
    }
}
