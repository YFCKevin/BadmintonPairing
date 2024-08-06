package com.yfckevin.badmintonPairing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.yfckevin.badmintonPairing.ConfigProperties;
import com.yfckevin.badmintonPairing.dto.RequestPostDTO;
import com.yfckevin.badmintonPairing.entity.Leader;
import com.yfckevin.badmintonPairing.entity.Post;
import com.yfckevin.badmintonPairing.repository.LeaderRepository;
import com.yfckevin.badmintonPairing.repository.PostRepository;
import com.yfckevin.badmintonPairing.utils.ConfigurationUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class LeaderServiceImpl implements LeaderService {
    private final SimpleDateFormat sdf;
    private final ConfigProperties configProperties;
    private final LeaderRepository leaderRepository;
    private final PostService postService;
    private final ObjectMapper objectMapper;
    private final MongoTemplate mongoTemplate;
    private final PostRepository postRepository;

    public LeaderServiceImpl(@Qualifier("sdf") SimpleDateFormat sdf, ConfigProperties configProperties, LeaderRepository leaderRepository, PostService postService, ObjectMapper objectMapper, MongoTemplate mongoTemplate,
                             PostRepository postRepository) {
        this.sdf = sdf;
        this.configProperties = configProperties;
        this.leaderRepository = leaderRepository;
        this.postService = postService;
        this.objectMapper = objectMapper;
        this.mongoTemplate = mongoTemplate;
        this.postRepository = postRepository;
    }

    @Override
    public int selectNewLeadersAndSave(List<RequestPostDTO> postDTOList) throws IOException {

        final List<String> userIdList = postDTOList.stream().map(RequestPostDTO::getUserId).toList();
        List<String> filteredUserIdList = new ArrayList<>();    //新團主
        final List<String> dbUserIdList = leaderRepository.findAll().stream().map(Leader::getUserId).toList();
        userIdList.forEach(u -> {
            if (!dbUserIdList.contains(u)) {
                filteredUserIdList.add(u);
            }
        });

        List<RequestPostDTO> filteredRequestPostDTOList = postDTOList.stream()
                .filter(requestPostDTO -> filteredUserIdList.contains(requestPostDTO.getUserId()))
                .peek(requestPostDTO -> requestPostDTO.setCreationDate(sdf.format(new Date())))
                .toList();

        List<Leader> leaderList = new ArrayList<>();
        filteredRequestPostDTOList = filteredRequestPostDTOList
                .stream().filter(p -> StringUtils.isNotBlank(p.getUserId()))
                .peek(requestPostDTO -> {
                    String name = requestPostDTO.getName();
                    String link = requestPostDTO.getLink();
                    String userId = extractUserId(link);
                    String groupId = extractGroupId(link);

                    Leader leader = new Leader();
                    leader.setName(name);
                    leader.setUserId(userId);
                    leader.setLink(link);
                    leader.setGroupId(groupId);
                    leader.setCreationDate(requestPostDTO.getCreationDate());
                    leaderList.add(leader);
                }).toList();

        leaderRepository.saveAll(leaderList);

        File file = new File(configProperties.getFileSavePath() + "searchNewLeader.json");
        objectMapper.writeValue(file, filteredRequestPostDTOList);

        // 讀取既有的 generalFile.json 資料
        ConfigurationUtil.Configuration();
        File generalFile = new File(configProperties.getFileSavePath() + "generalFile.json");
        TypeRef<List<RequestPostDTO>> typeRef = new TypeRef<>() {
        };
        List<RequestPostDTO> generalPostList = JsonPath.parse(generalFile).read("$", typeRef);
        generalPostList.addAll(filteredRequestPostDTOList);
        objectMapper.writeValue(generalFile, generalPostList);

        return postService.dataCleaning(configProperties.getFileSavePath() + "searchNewLeader.json");
    }

    @Override
    public List<Leader> findAllByUserIdIn(Set<String> userIdList) {
        return leaderRepository.findAllByUserIdIn(userIdList);
    }

    @Override
    public List<Leader> findAllAndOrderByCreationDate() {
        return leaderRepository.findAll(Sort.by(Sort.Order.desc("creationDate")));
    }

    @Override
    public void save(Leader leader) {
        leaderRepository.save(leader);
    }

    @Override
    public Optional<Leader> findById(String id) {
        return leaderRepository.findById(id);
    }

    @Override
    public List<Leader> findLeaderByConditions(String keyword, String startDate, String endDate) {
        List<Criteria> orCriterias = new ArrayList<>();
        List<Criteria> andCriterias = new ArrayList<>();

        Criteria criteria = Criteria.where("deletionDate").exists(false);

        if (StringUtils.isNotBlank(keyword)) {
            Criteria criteria_name = Criteria.where("name").regex(keyword, "i");
            Criteria criteria_groupId = Criteria.where("groupId").regex(keyword, "i");
            Criteria criteria_userId = Criteria.where("userId").regex(keyword, "i");
            orCriterias.add(criteria_name);
            orCriterias.add(criteria_groupId);
            orCriterias.add(criteria_userId);
        }

        if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
            Criteria criteria_start = Criteria.where("creationDate").gte(startDate);
            Criteria criteria_end = Criteria.where("creationDate").lte(endDate);
            andCriterias.add(criteria_start);
            andCriterias.add(criteria_end);
        }

        if (!orCriterias.isEmpty()) {
            criteria = criteria.orOperator(orCriterias.toArray(new Criteria[0]));
        }
        if(!andCriterias.isEmpty()) {
            criteria = criteria.andOperator(andCriterias.toArray(new Criteria[0]));
        }

        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Order.desc("creationDate")));

        return mongoTemplate.find(query, Leader.class);
    }

    @Override
    public void deleteById(String id) {
        leaderRepository.deleteById(id);
    }

    private static String extractUserId(String link) {
        String[] parts = link.split("/");
        return parts[parts.length - 1];
    }

    private static String extractGroupId(String link) {
        String[] parts = link.split("/");
        for (int i = 0; i < parts.length; i++) {
            if ("groups".equals(parts[i])) {
                return parts[i + 1];
            }
        }
        return "";
    }
}
