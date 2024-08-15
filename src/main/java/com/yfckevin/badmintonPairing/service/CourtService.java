package com.yfckevin.badmintonPairing.service;

import com.yfckevin.badmintonPairing.entity.Court;

import java.util.List;
import java.util.Optional;

public interface CourtService {

    void save(Court court);

    Optional<Court> findById(String id);

    List<Court> findAllByOrderByCreationDateAsc();

    void delete(Court court);

    List<Court> findCourtByCondition(String keyword);
}
