package com.keroles.ewalletddd.topup.domain.port;



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
