package com.yfckevin.badmintonPairing.service;

import com.yfckevin.badmintonPairing.entity.Follower;
import com.yfckevin.badmintonPairing.entity.TemplateSubject;
import com.yfckevin.badmintonPairing.repository.TemplateSubjectRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TemplateSubjectServiceImpl implements TemplateSubjectService{
    private final TemplateSubjectRepository templateSubjectRepository;
    private final MongoTemplate mongoTemplate;

    public TemplateSubjectServiceImpl(TemplateSubjectRepository templateSubjectRepository, MongoTemplate mongoTemplate) {
        this.templateSubjectRepository = templateSubjectRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<TemplateSubject> findById(String subjectId) {
        return templateSubjectRepository.findById(subjectId);
    }

    @Override
    public TemplateSubject save(TemplateSubject templateSubject) {
        return templateSubjectRepository.save(templateSubject);
    }

    @Override
    public List<TemplateSubject> findAllAndOrderByCreationDate() {
        return templateSubjectRepository.findAll(Sort.by(Sort.Order.desc("creationDate")));
    }

    @Override
    public List<TemplateSubject> templateSearch(String keyword) {
        List<Criteria> orCriterias = new ArrayList<>();

        Criteria criteria = Criteria.where("deletionDate").exists(false);

        if (StringUtils.isNotBlank(keyword)) {
            Criteria criteria_title = Criteria.where("title").regex(keyword, "i");
            Criteria criteria_altText = Criteria.where("altText").regex(keyword, "i");
            orCriterias.add(criteria_title);
            orCriterias.add(criteria_altText);
        }

        if(!orCriterias.isEmpty()) {
            criteria = criteria.orOperator(orCriterias.toArray(new Criteria[0]));
        }

        Query query = new Query(criteria);
        return mongoTemplate.find(query, TemplateSubject.class);
    }
}
