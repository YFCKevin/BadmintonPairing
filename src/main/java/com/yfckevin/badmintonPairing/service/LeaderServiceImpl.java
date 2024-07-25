package com.yfckevin.badmintonPairing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yfckevin.badmintonPairing.ConfigProperties;
import com.yfckevin.badmintonPairing.dto.RequestPostDTO;
import com.yfckevin.badmintonPairing.entity.Leader;
import com.yfckevin.badmintonPairing.repository.LeaderRepository;
import org.springframework.beans.factory.annotation.Qualifier;
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

    public LeaderServiceImpl(@Qualifier("sdf") SimpleDateFormat sdf, ConfigProperties configProperties, LeaderRepository leaderRepository, PostService postService, ObjectMapper objectMapper) {
        this.sdf = sdf;
        this.configProperties = configProperties;
        this.leaderRepository = leaderRepository;
        this.postService = postService;
        this.objectMapper = objectMapper;
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
            leader.setCreationDate(sdf.format(new Date()));
            leaderList.add(leader);
        });

        leaderRepository.saveAll(leaderList);

        File file = new File(configProperties.getFileSavePath() + "searchNewLeader.json");
        objectMapper.writeValue(file, filteredRequestPostDTOList);

        return postService.dataCleaning(configProperties.getFileSavePath() + "searchNewLeader.json");
    }

    @Override
    public List<Leader> findAllByUserIdIn(Set<String> userIdList) {
        return leaderRepository.findAllByUserIdIn(userIdList);
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
