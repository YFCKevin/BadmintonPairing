package com.yfckevin.badmintonPairing.service;

import com.yfckevin.badmintonPairing.entity.Court;
import com.yfckevin.badmintonPairing.entity.Post;
import com.yfckevin.badmintonPairing.repository.CourtRepository;
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
public class CourtServiceImpl implements CourtService{
    private final CourtRepository courtRepository;
    private final MongoTemplate mongoTemplate;

    public CourtServiceImpl(CourtRepository courtRepository, MongoTemplate mongoTemplate) {
        this.courtRepository = courtRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(Court court) {
        courtRepository.save(court);
    }

    @Override
    public Optional<Court> findById(String id) {
        return courtRepository.findById(id);
    }

    @Override
    public List<Court> findAllByOrderByCreationDateAsc() {
        return courtRepository.findAllByOrderByCreationDateAsc();
    }

    @Override
    public void delete(Court court) {
        courtRepository.delete(court);
    }

    @Override
    public List<Court> findCourtByCondition(String keyword) {
        List<Criteria> orCriterias = new ArrayList<>();

        Criteria criteria = Criteria.where("deletionDate").exists(false);

        if (StringUtils.isNotBlank(keyword)) {
            Criteria criteria_name = Criteria.where("name").regex(keyword, "i");
            Criteria criteria_place = Criteria.where("address").regex(keyword, "i");
            orCriterias.add(criteria_name);
            orCriterias.add(criteria_place);
        }

        if(!orCriterias.isEmpty()) {
            criteria = criteria.orOperator(orCriterias.toArray(new Criteria[0]));
        }

        Query query = new Query(criteria);

        return mongoTemplate.find(query, Court.class);
    }

    @Override
    public void saveAll(List<Court> courtList) {
        courtRepository.saveAll(courtList);
    }
}
