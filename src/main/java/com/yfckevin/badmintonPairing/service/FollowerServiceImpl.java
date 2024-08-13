package com.yfckevin.badmintonPairing.service;

import com.yfckevin.badmintonPairing.entity.Follower;
import com.yfckevin.badmintonPairing.repository.FollowerRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FollowerServiceImpl implements FollowerService{
    private final FollowerRepository followerRepository;

    public FollowerServiceImpl(FollowerRepository followerRepository) {
        this.followerRepository = followerRepository;
    }

    @Override
    public void save(Follower follower) {
        followerRepository.save(follower);
    }

    @Override
    public Optional<Follower> findByUserId(String userId) {
        return followerRepository.findByUserId(userId);
    }
}
