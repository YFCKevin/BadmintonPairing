package com.yfckevin.badmintonPairing.service;

import com.yfckevin.badmintonPairing.entity.Court;
import com.yfckevin.badmintonPairing.repository.CourtRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CourtServiceImpl implements CourtService{
    private final CourtRepository courtRepository;

    public CourtServiceImpl(CourtRepository courtRepository) {
        this.courtRepository = courtRepository;
    }

    @Override
    public List<Court> findAll() {
        return courtRepository.findAll();
    }

    @Override
    public void save(Court court) {
        courtRepository.save(court);
    }

    @Override
    public Optional<Court> findById(String id) {
        return courtRepository.findById(id);
    }
}
