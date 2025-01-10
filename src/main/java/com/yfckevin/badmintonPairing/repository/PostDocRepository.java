package com.yfckevin.badmintonPairing.repository;

import com.yfckevin.badmintonPairing.entity.PostDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PostDocRepository extends ElasticsearchRepository<PostDoc, String> {
}
