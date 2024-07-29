package com.yfckevin.badmintonPairing.repository;

import com.yfckevin.badmintonPairing.entity.Leader;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Set;

public interface LeaderRepository extends MongoRepository<Leader, String> {
    List<Leader> findAllByUserIdIn(Set<String> userIdList);
    List<Leader> findAll(Sort sort);
}
