package com.yfckevin.badmintonPairing.service;

import com.yfckevin.badmintonPairing.entity.Follower;

import java.util.Optional;

public interface FollowerService {
    void save(Follower follower);

    Optional<Follower> findByUserId(String userId);
}
