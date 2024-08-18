package com.yfckevin.badmintonPairing.service;

import com.yfckevin.badmintonPairing.entity.Follower;

import java.util.List;
import java.util.Optional;

public interface FollowerService {
    void save(Follower follower);

    Optional<Follower> findByUserId(String userId);

    List<Follower> findByIdIn(List<String> userIds);

    List<Follower> findAll();

    List<Follower> searchFollower(String keyword);
}
