package com.yfckevin.badmintonPairing.controller;

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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api")
public class DataProcessingController {
    private final LeaderService leaderService;
    private final PostService postService;
    private final CrawlerService crawlerService;
    private final SimpleDateFormat ssf;
    Logger logger = LoggerFactory.getLogger(DataProcessingController.class);
    public DataProcessingController(LeaderService leaderService, PostService postService, CrawlerService crawlerService, @Qualifier("ssf") SimpleDateFormat ssf) {
        this.leaderService = leaderService;
        this.postService = postService;
        this.crawlerService = crawlerService;
        this.ssf = ssf;
    }


//    @Scheduled(cron = "0 0 10 ? * MON,THU,SAT")
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
            MailUtils.sendMail("pigmonkey0921@gmail.com", "尋找searchNewLeader完成", "完成");
        } else {
            MailUtils.sendMail("pigmonkey0921@gmail.com", "尋找searchNewLeader失敗", "失敗");
        }
        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 每日固定匯出已過期貼文的所有links，給爬蟲用
     * @throws IOException
     */
//    @Scheduled(cron = "0 0 8 ? * MON,THU,SAT")
    @GetMapping("/selectPastPosts")
    public void selectPastPosts() throws IOException, InterruptedException {
        // 取出該userId最新一則貼文的貼文是過期的貼文
        final List<Post> postList = postService.getPassPostsByLeadersAndTodayBefore();
        // 取出過期貼文的團主userId
        Set<String> userIds = postList.stream().map(Post::getUserId).collect(Collectors.toSet());
        // 蒐集links
        final List<String> linkList = leaderService.findAllByUserIdIn(userIds).stream().map(Leader::getLink).toList();

        // 寄信通知每日需更新的link筆數
        MailUtils.sendMail("pigmonkey0921@gmail.com", "每日项目", "共" + linkList.size() + "筆");

        int batchSize = 50;
        int totalLinks = linkList.size();

        List<List<String>> batches = IntStream.range(0, (totalLinks + batchSize - 1) / batchSize)
                .mapToObj(i -> linkList.subList(i * batchSize, Math.min(totalLinks, (i + 1) * batchSize)))
                .toList();

        for (int i = 5; i < batches.size(); i++) {
            List<String> batch = batches.get(i);
            final int getNewPosts = crawlerService.callCrawlerAPIGetNewPosts(batch);
            logger.info("(迴圈) 當次/總數：" + i + " / " + batches.size() + "，該次新貼文數: " + getNewPosts);
            Thread.sleep(60 * 1000);
        }
        logger.info(ssf.format(new Date()) + "完成貼文爬蟲");
        MailUtils.sendMail("pigmonkey0921@gmail.com", "尋找selectPastPosts完成", "完成");
    }
}
