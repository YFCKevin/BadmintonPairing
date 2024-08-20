package com.yfckevin.badmintonPairing.repository;


import com.yfckevin.badmintonPairing.entity.Court;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CourtRepository extends MongoRepository<Court, String> {
    List<Court> findAllByOrderByCreationDateAsc();
    @Query("{ 'postId': { $regex: ?0, $options: 'i' } }")
    Optional<Court> findByPostId(String id);
}
