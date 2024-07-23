package com.yfckevin.badmintonPairing.controller;

import com.yfckevin.badmintonPairing.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OpenAiController {

    Logger logger = LoggerFactory.getLogger(OpenAiController.class);

    private final OpenAiService openAiService;

    public OpenAiController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    /**
     * 喂GPT text completion
     * @return
     */
    @PostMapping("/completion")
    public ResponseEntity<?> completion (){
        logger.info("[completion]");
        openAiService.generatePosts();
        return ResponseEntity.ok("成功");
    }
}
