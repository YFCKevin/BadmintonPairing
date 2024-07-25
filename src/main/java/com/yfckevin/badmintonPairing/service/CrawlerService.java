package com.yfckevin.badmintonPairing.service;

import com.yfckevin.badmintonPairing.ConfigProperties;
import com.yfckevin.badmintonPairing.dto.RequestCrawlerDTO;
import com.yfckevin.badmintonPairing.dto.RequestPostDTO;
import com.yfckevin.badmintonPairing.exception.ResultStatus;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
public class CrawlerService {
    private final ConfigProperties configProperties;
    private final RestTemplate restTemplate;
    private final PostService postService;

    public CrawlerService(ConfigProperties configProperties, RestTemplate restTemplate, PostService postService) {
        this.configProperties = configProperties;
        this.restTemplate = restTemplate;
        this.postService = postService;
    }

    public int callCrawlerAPIGetNewPosts(List<String> linkList) throws IOException, InterruptedException {
        String url = configProperties.getCrawlerDomain() + "crawlerNewPosts";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        RequestCrawlerDTO dto = new RequestCrawlerDTO();
        dto.setEmail(configProperties.getCrawlerEmail());
        dto.setPassword(configProperties.getCrawlerPassword());
        dto.setUrls(linkList);

        HttpEntity<RequestCrawlerDTO> entity = new HttpEntity<>(dto, headers);
        ResponseEntity<ResultStatus<List<RequestPostDTO>>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {}
        );
        if ("C000".equals(Objects.requireNonNull(response.getBody()).getCode())) {
            final List<RequestPostDTO> dailyPosts = response.getBody().getData();
            //總檔(generalFile.json)比對爬蟲來的dailyPosts.json，最後獲取新貼文的資訊
            final String filePath = postService.getDifferencePostsAndSaveInGeneralFileAndReturnFilePath(dailyPosts);
            return postService.dataCleaning(filePath);
        } else {
            return 0;
        }
    }

    public ResultStatus<List<RequestPostDTO>> searchNewLeaderByCrawler() {
        String url = configProperties.getCrawlerDomain() + "crawlerNewLeaders";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        RequestCrawlerDTO dto = new RequestCrawlerDTO();
        dto.setEmail(configProperties.getCrawlerEmail());
        dto.setPassword(configProperties.getCrawlerPassword());
        dto.setUrl("https://www.facebook.com/groups/392553431115145/?sorting_setting=CHRONOLOGICAL");

        HttpEntity<RequestCrawlerDTO> entity = new HttpEntity<>(dto, headers);
        ResponseEntity<ResultStatus<List<RequestPostDTO>>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {
                }
        );

        ResultStatus resultStatus = new ResultStatus();
        if ("C000".equals(Objects.requireNonNull(response.getBody()).getCode())) {
            final List<RequestPostDTO> dailyPosts = response.getBody().getData();
            resultStatus.setCode("C000");
            resultStatus.setMessage("成功");
            resultStatus.setData(dailyPosts);
        } else {
            resultStatus.setCode("C999");
            resultStatus.setMessage("例外發生");
        }
        return resultStatus;
    }
}
