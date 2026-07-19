package com.keroles.ewalletddd.cashout.domain.port;

// Rails are normalized to async: dispatch is accepted (PENDING) and the outcome arrives later,
// or rejected synchronously. railReference is the rail's handle on the accepted payout.
public record RailDispatchResult(boolean accepted, String railReference, String reason) {
    public static RailDispatchResult pending(String railReference) {
        return new RailDispatchResult(true, railReference, null);
    }
    public static RailDispatchResult rejected(String reason) {
        return new RailDispatchResult(false, null, reason);
    }
}
