package com.yfckevin.badmintonPairing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;

public interface LineService {
    ResponseEntity<?> autoReply(String msg, String replyToken) throws JsonProcessingException;
}
