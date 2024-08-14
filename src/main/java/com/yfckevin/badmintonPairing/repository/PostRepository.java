package com.yfckevin.badmintonPairing.repository;

import com.yfckevin.badmintonPairing.entity.Post;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Date;
import java.util.List;

public interface PostRepository extends MongoRepository<Post, String> {
    @Query("{ 'creationDate' : { $gte: ?0, $lt: ?1 } }")
    List<Post> findByCreationDateBetween(String startDate, String endDate);

    void deleteByIdIn(List<String> postIdIn);
    @Query("{ 'dayOfWeek': ?0, 'startTime': { $gte: ?1, $lte: ?2 } }")
    List<Post> findPostsByDayAndTime(String day, String targetStartDate, String targetEndDate);

    List<Post> findByIdIn(List<String> strings);
}
