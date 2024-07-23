package com.yfckevin.badmintonPairing.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yfckevin.badmintonPairing.ConfigProperties;
import com.yfckevin.badmintonPairing.dto.RequestPostDTO;
import com.yfckevin.badmintonPairing.entity.Leader;
import com.yfckevin.badmintonPairing.entity.Post;
import com.yfckevin.badmintonPairing.exception.ResultStatus;
import com.yfckevin.badmintonPairing.service.CrawlerService;
import com.yfckevin.badmintonPairing.service.LeaderService;
import com.yfckevin.badmintonPairing.service.PostService;
import com.yfckevin.badmintonPairing.utils.MailUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DataProcessingController {
    private final LeaderService leaderService;
    private final PostService postService;
    private final ObjectMapper objectMapper;
    private final ConfigProperties configProperties;
    private final CrawlerService crawlerService;
    Logger logger = LoggerFactory.getLogger(DataProcessingController.class);
    public DataProcessingController(LeaderService leaderService, PostService postService, ObjectMapper objectMapper, ConfigProperties configProperties, CrawlerService crawlerService) {
        this.leaderService = leaderService;
        this.postService = postService;
        this.objectMapper = objectMapper;
        this.configProperties = configProperties;
        this.crawlerService = crawlerService;
    }


//    @Scheduled(cron = "0 0 8 * * ?")
    @GetMapping("/searchNewLeader")
    public ResponseEntity<?> searchNewLeader() throws IOException {
        logger.info("[searchNewLeader]");
        ResultStatus resultStatus = new ResultStatus();
        final ResultStatus<List<RequestPostDTO>> resultStatus_response = crawlerService.searchNewLeaderByCrawler();
        if ("C000".equals(resultStatus_response.getCode())) {
            final List<RequestPostDTO> postDTOList = resultStatus_response.getData();
            final int savedLeaders = leaderService.selectNewLeadersAndSave(postDTOList);
            resultStatus.setCode("C000");
            resultStatus.setMessage("成功");
            resultStatus.setData(savedLeaders);
        }
        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 每日固定匯出已過期貼文的所有links，給爬蟲用
     * @throws IOException
     */
//    @Scheduled(cron = "0 0 7 * * ?")
    @GetMapping("/selectPastPosts")
    public void selectPastPosts() throws IOException, InterruptedException {
        // 取出該userId最新一則貼文的貼文是過期的貼文
        final List<Post> postList = postService.getPassPostsByLeadersAndTodayBefore();
        // 取出過期貼文的團主userId
        Set<String> userIds = postList.stream().map(Post::getUserId).collect(Collectors.toSet());
        // 蒐集links
        final List<String> linkList = leaderService.findAllByUserIdIn(userIds).stream().map(Leader::getLink).toList();

        // links寫入file，後續供爬蟲用
        File file = new File(configProperties.getFileSavePath() + "preCrawler_link.json");
            // 讀取既有資料
        Set<String> existingLinks = new HashSet<>();
        if (file.exists()) {
            TypeReference<Set<String>> typeRef = new TypeReference<>() {};
            existingLinks = objectMapper.readValue(file, typeRef);
        }
            //添加新的link (不會有重複link)
        existingLinks.addAll(linkList);
            //重新寫入file
        objectMapper.writeValue(file, existingLinks);

        // 寄信通知每日需更新的link筆數
        MailUtils.sendMail("pigmonkey0921@gmail.com", "每日項目", "共" + postList.size() + "筆");
        // call爬蟲API取得新貼文
        crawlerService.callCrawlerAPIGetNewPosts(linkList);
    }


//    /**
//     *
//     * @return 回傳完成資料清洗的筆數，後續要喂GPT產生統一格式
//     * @throws IOException
//     * @throws InterruptedException
//     */
//    @GetMapping("/newPosts")
//    public ResponseEntity<?> newPosts() throws IOException, InterruptedException {
//        logger.info("[newPosts]");
//        //總檔(generalFile.json)比對爬蟲來的dailyPosts.json，最後獲取新貼文的資訊
//        final String filePath = postService.getDifferencePostsAndSaveInGeneralFileAndReturnFilePath();
//        return ResponseEntity.ok(postService.dataCleaning(filePath));
//    }

}
