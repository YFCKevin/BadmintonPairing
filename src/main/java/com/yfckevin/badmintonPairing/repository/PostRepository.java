package com.yfckevin.badmintonPairing.repository;

import com.yfckevin.badmintonPairing.entity.Post;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Date;
import java.util.List;

public interface PostRepository extends MongoRepository<Post, String> {
    @Query("{ 'creationDate' : { $gte: ?0, $lt: ?1 } }")
    List<Post> findByCreationDateBetween(Date startDate, Date endDate);
}
