package com.yfckevin.badmintonPairing.repository;

import com.yfckevin.badmintonPairing.entity.Follower;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface FollowerRepository extends MongoRepository<Follower, String> {
    Optional<Follower> findByUserId(String userId);
}
