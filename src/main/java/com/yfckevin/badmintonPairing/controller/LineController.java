package com.yfckevin.badmintonPairing.controller;

import com.yfckevin.badmintonPairing.service.OpenAiServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LineController {

    Logger logger = LoggerFactory.getLogger(LineController.class);
    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody String payload){
        logger.info("Received payload: " + payload);
        return ResponseEntity.ok("ok");
    }
}
