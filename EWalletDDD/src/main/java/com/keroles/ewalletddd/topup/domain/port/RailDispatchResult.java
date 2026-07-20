package com.keroles.ewalletddd.topup.domain.port;

// Dispatch outcome. An async rail (Tcs) returns PENDING (final result arrives later via callback);
// a sync rail (Mbank) returns CONFIRMED (credit now, no callback) or REJECTED up front.
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
