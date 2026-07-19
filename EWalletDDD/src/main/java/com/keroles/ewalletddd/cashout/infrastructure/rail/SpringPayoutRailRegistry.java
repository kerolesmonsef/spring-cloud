package com.keroles.ewalletddd.cashout.infrastructure.rail;

import com.keroles.ewalletddd.cashout.domain.port.PayoutRailPort;
import com.keroles.ewalletddd.cashout.domain.port.PayoutRailRegistry;
import com.keroles.ewalletddd.cashout.domain.valueObject.Rail;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Self-assembles the rail routing map from every PayoutRailPort bean.
 * Onboarding rail #4 = one new adapter @Component + one Rail enum value; nothing here changes.
 */
@Component
public class SpringPayoutRailRegistry implements PayoutRailRegistry {

    private final Map<Rail, PayoutRailPort> byRail;

    public SpringPayoutRailRegistry(List<PayoutRailPort> adapters) {
        this.byRail = adapters.stream().collect(Collectors.toMap(PayoutRailPort::rail, Function.identity()));
    }

    @Override
    public PayoutRailPort forRail(Rail rail) {
        PayoutRailPort port = byRail.get(rail);
        if (port == null) throw new IllegalArgumentException("No payout adapter for rail " + rail);
        return port;
    }
}
