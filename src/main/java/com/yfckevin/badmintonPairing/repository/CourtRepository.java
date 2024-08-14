package com.yfckevin.badmintonPairing.repository;


import com.yfckevin.badmintonPairing.entity.Court;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CourtRepository extends MongoRepository<Court, String> {
}
