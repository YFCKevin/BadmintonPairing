package com.yfckevin.badmintonPairing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yfckevin.badmintonPairing.ConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class LineServiceImpl implements LineService{
    Logger logger = LoggerFactory.getLogger(LineServiceImpl.class);
    private final ConfigProperties configProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LineServiceImpl(ConfigProperties configProperties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.configProperties = configProperties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResponseEntity<?> autoReply(String msg, String replyToken) throws JsonProcessingException {

        final HashMap msgMap = objectMapper.readValue(msg, HashMap.class);
        List messages = new ArrayList();
        messages.add(msgMap);

        String url = "https://api.line.me/v2/bot/message/reply";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(configProperties.getChannelAccessToken());
        HashMap data = new HashMap<>();
        data.put("replyToken", replyToken);
        data.put("messages", messages);
        HttpEntity<HashMap> request = new HttpEntity<>(data, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println(response);
            }
        } catch (Exception e) {
            logger.error("發送訊息時報錯：{}", e.getMessage());
        }

        return ResponseEntity.ok("");
    }
}
