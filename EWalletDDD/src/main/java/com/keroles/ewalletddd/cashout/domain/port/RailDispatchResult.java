package com.keroles.ewalletddd.cashout.domain.port;

// Dispatch outcome. Async rails return PENDING (final result arrives later via callback);
// a sync rail returns CONFIRMED (already settled, no callback coming) or REJECTED up front.
public record RailDispatchResult(Outcome outcome, String railReference, String reason) {

    public enum Outcome { PENDING, CONFIRMED, REJECTED }

    public static RailDispatchResult pending(String railReference) {
        return new RailDispatchResult(Outcome.PENDING, railReference, null);
    }
    public static RailDispatchResult confirmed(String railReference) {
        return new RailDispatchResult(Outcome.CONFIRMED, railReference, null);
    }
    public static RailDispatchResult rejected(String reason) {
        return new RailDispatchResult(Outcome.REJECTED, null, reason);
    }
}
