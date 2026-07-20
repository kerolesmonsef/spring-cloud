package com.keroles.ewalletddd.topup.infrastructure.rail;

import com.keroles.ewalletddd.topup.domain.port.TopupRailPort;
import com.keroles.ewalletddd.topup.domain.port.TopupRailRegistry;
import com.keroles.ewalletddd.topup.domain.valueObject.Rail;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Self-assembles the rail routing map from every TopupRailPort bean.
 * Adding a rail = one new adapter @Component + one Rail enum value; nothing here changes.
 */
@Component
public class SpringTopupRailRegistry implements TopupRailRegistry {

    private final Map<Rail, TopupRailPort> byRail;

    public SpringTopupRailRegistry(List<TopupRailPort> adapters) {
        this.byRail = adapters.stream().collect(Collectors.toMap(TopupRailPort::rail, Function.identity()));
    }

    @Override
    public TopupRailPort forRail(Rail rail) {
        TopupRailPort port = byRail.get(rail);
        if (port == null) throw new IllegalArgumentException("No topup adapter for rail " + rail);
        return port;
    }
}
