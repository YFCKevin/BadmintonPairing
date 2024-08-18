package com.yfckevin.badmintonPairing.repository;

import com.yfckevin.badmintonPairing.entity.TemplateDetail;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TemplateDetailRepository extends MongoRepository<TemplateDetail, String> {
    List<TemplateDetail> findByIdIn(List<String> detailIds);
}
