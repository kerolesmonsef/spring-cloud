package com.keroles.ewalletddd.topup.domain.valueObject;

// The topup rails. Add a value here + one TopupRailPort adapter to onboard a new rail.
// Tcs is async (awaits a callback); Mbank is synchronous (settles at dispatch).
public enum Rail { TCS, MBANK }
