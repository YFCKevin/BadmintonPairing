package com.yfckevin.badmintonPairing.repository;

import com.yfckevin.badmintonPairing.entity.Post;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PostRepository extends MongoRepository<Post, String> {
}
