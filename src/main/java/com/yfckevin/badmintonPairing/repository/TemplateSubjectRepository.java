package com.yfckevin.badmintonPairing.repository;

import com.yfckevin.badmintonPairing.entity.TemplateSubject;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TemplateSubjectRepository extends MongoRepository<TemplateSubject, String> {
}
