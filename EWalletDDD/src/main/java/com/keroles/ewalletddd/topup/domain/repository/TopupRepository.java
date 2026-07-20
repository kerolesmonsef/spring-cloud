package com.keroles.ewalletddd.topup.domain.repository;

import com.keroles.ewalletddd.topup.domain.model.TopupRequest;
import com.keroles.ewalletddd.topup.domain.valueObject.TopupId;

import java.util.Optional;

public interface TopupRepository {
    Optional<TopupRequest> findById(TopupId id);
    void save(TopupRequest topup);
}
