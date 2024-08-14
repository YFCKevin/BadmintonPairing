package com.yfckevin.badmintonPairing.service;

import com.yfckevin.badmintonPairing.entity.Court;

import java.util.List;
import java.util.Optional;

public interface CourtService {
    List<Court> findAll();

    void save(Court court);

    Optional<Court> findById(String id);
}
