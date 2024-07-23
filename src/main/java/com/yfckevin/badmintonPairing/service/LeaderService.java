package com.yfckevin.badmintonPairing.service;

import com.yfckevin.badmintonPairing.dto.RequestPostDTO;
import com.yfckevin.badmintonPairing.entity.Leader;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface LeaderService {
    int selectNewLeadersAndSave (List<RequestPostDTO> postDTOList) throws IOException;

    List<Leader> findAllByUserIdIn(Set<String> userIdList);

}
