package com.yfckevin.badmintonPairing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.yfckevin.badmintonPairing.ConfigProperties;
import com.yfckevin.badmintonPairing.dto.RequestPostDTO;
import com.yfckevin.badmintonPairing.entity.Leader;
import com.yfckevin.badmintonPairing.entity.Post;
import com.yfckevin.badmintonPairing.repository.LeaderRepository;
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

    public LeaderServiceImpl(@Qualifier("sdf") SimpleDateFormat sdf, ConfigProperties configProperties, LeaderRepository leaderRepository, PostService postService, ObjectMapper objectMapper, MongoTemplate mongoTemplate) {
        this.sdf = sdf;
        this.configProperties = configProperties;
        this.leaderRepository = leaderRepository;
        this.postService = postService;
        this.objectMapper = objectMapper;
        this.mongoTemplate = mongoTemplate;
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

        final List<RequestPostDTO> filteredRequestPostDTOList = postDTOList.stream()
                .filter(requestPostDTO -> filteredUserIdList.contains(requestPostDTO.getUserId()))
                .peek(requestPostDTO -> requestPostDTO.setCreationDate(sdf.format(new Date())))
                .toList();

        List<Leader> leaderList = new ArrayList<>();
        filteredRequestPostDTOList.forEach(requestPostDTO -> {
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
        });

        leaderRepository.saveAll(leaderList);

        File file = new File(configProperties.getFileSavePath() + "searchNewLeader.json");
        objectMapper.writeValue(file, filteredRequestPostDTOList);

        // 讀取既有的 generalFile.json 資料
        ConfigurationUtil.Configuration();
        File generalFile = new File(configProperties.getJsonPath() + "generalFile.json");
        TypeRef<List<RequestPostDTO>> typeRef = new TypeRef<>() {};
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
        return leaderRepository.findAllAndOrderByCreationDate();
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
    public List<Leader> findLeaderByConditions(String keyword) {
        List<Criteria> orCriterias = new ArrayList<>();

        Criteria criteria = Criteria.where("deletionDate").exists(false);

        if (StringUtils.isNotBlank(keyword)) {
            Criteria criteria_name = Criteria.where("name").regex(keyword, "i");
            Criteria criteria_link = Criteria.where("link").regex(keyword, "i");
            Criteria criteria_userId = Criteria.where("userId").regex(keyword, "i");
            orCriterias.add(criteria_name);
            orCriterias.add(criteria_link);
            orCriterias.add(criteria_userId);
        }

        if(!orCriterias.isEmpty()) {
            criteria = criteria.orOperator(orCriterias.toArray(new Criteria[0]));
        }

        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Order.desc("creationDate")));

        return mongoTemplate.find(query, Leader.class);
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
