package com.yfckevin.badmintonPairing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yfckevin.badmintonPairing.ConfigProperties;
import com.yfckevin.badmintonPairing.dto.BadmintonPostDTO;
import com.yfckevin.badmintonPairing.dto.ChatCompletionResponse;
import com.yfckevin.badmintonPairing.entity.Post;
import com.yfckevin.badmintonPairing.repository.PostRepository;
import com.yfckevin.badmintonPairing.utils.ConfigurationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class OpenAiServiceImpl implements OpenAiService {
    Logger logger = LoggerFactory.getLogger(OpenAiServiceImpl.class);
    private final SimpleDateFormat sdf;
    private final SimpleDateFormat svf;
    private final ConfigProperties configProperties;
    private final PostRepository postRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiServiceImpl(@Qualifier("sdf") SimpleDateFormat sdf, @Qualifier("svf") SimpleDateFormat svf, ConfigProperties configProperties, PostRepository postRepository, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.sdf = sdf;
        this.svf = svf;
        this.configProperties = configProperties;
        this.postRepository = postRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }


    @Async
    @Override
    public CompletableFuture<List<Post>> generatePosts(String prompt) throws Exception {

        return CompletableFuture.supplyAsync(() -> {
            try {

                //組final prompt
                String pre_prompt = "資料處理要求\n" +
                        "\n" +
                        "1. 資料格式\n" +
                        "   - 請將每則貼文格式化為 JSON。每則貼文應該包含以下欄位：\n" +
                        "     - `name`: 作者名稱\n" +
                        "     - `userId`: 用戶 ID\n" +
                        "     - `place`: 活動地點\n" +
                        "     - `startTime`: 開始時間，格式為 `2024-MM-dd HH:mm:ss`，且必須是 `2024` 年的日期\n" +
                        "     - `endTime`: 結束時間，格式為 `2024-MM-dd HH:mm:ss`，且必須是 `2024` 年的日期\n" +
                        "     - `level`: 程度（用字串表示）\n" +
                        "     - `fee`: 費用（僅取數字）\n" +
                        "     - `duration`: 時長（以分鐘表示，數字型態）\n" +
                        "     - `brand`: 球種品牌（若無則留空）\n" +
                        "     - `contact`: 聯絡方式（若無則留空）\n" +
                        "     - `parkInfo`: 停車場資訊（若無則留空）\n" +
                        "     - `type`: 固定為 \"disposable\"\n" +
                        "     - `airConditioner`: 冷氣資訊（用字串表示，`present` 表示有，`absent` 表示無，`no_mention` 表示未標示）\n" +
                        "\n" +
                        "2. 資料整理\n" +
                        "   - 每則貼文內容可能包含多個打球資訊，請將每個打球資訊獨立為單獨的 JSON 物件。\n" +
                        "\n" +
                        "3. 輸出要求\n" +
                        "   - 確保最終結果為有效的 JSON 格式，且所有 `startTime` 和 `endTime` 欄位必須是 `2024` 年的日期。\n" +
                        "\n" +
                        "4. 範例格式\n" +
                        "```json\n" +
                        "[\n" +
                        "  {\n" +
                        "    \"name\": \"張三\",\n" +
                        "    \"userId\": \"user123\",\n" +
                        "    \"place\": \"某某羽球場\",\n" +
                        "    \"startTime\": \"2024-09-16 18:00:00\",\n" +
                        "    \"endTime\": \"2024-09-16 20:00:00\",\n" +
                        "    \"level\": \"中級\",\n" +
                        "    \"fee\": \"200\",\n" +
                        "    \"duration\": 120,\n" +
                        "    \"brand\": \"Yonex\",\n" +
                        "    \"contact\": \"0912345678\",\n" +
                        "    \"parkInfo\": \"場外停車場\",\n" +
                        "    \"type\": \"disposable\",\n" +
                        "    \"airConditioner\": \"present\"\n" +
                        "  }\n" +
                        "]\n" +
                        "```";

                return callOpenAI(prompt + "\n" + pre_prompt);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private List<Post> callOpenAI(String prompt) throws Exception {
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(configProperties.getApiKey());

        String data = createPayload(prompt);

        HttpEntity<String> entity = new HttpEntity<>(data, headers);

        ResponseEntity<ChatCompletionResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, ChatCompletionResponse.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("GPT回傳的status code: {}", response);
            ChatCompletionResponse responseBody = response.getBody();
            String content = extractContent(responseBody);
            System.out.println("GPT回傳資料 ======> " + content);

            //備份GPT回傳的資料
            String baseFileName = svf.format(new Date()) + "-GPT_callback_response.json";
            File file = new File(configProperties.getGptBackupSavePath() + baseFileName);
            int counter = 1;
            // 如果存在則創建新file name
            while (file.exists()) {
                String newFileName = svf.format(new Date()) + "-GPT_callback_response(" + counter + ").json";
                file = new File(configProperties.getGptBackupSavePath() + newFileName);
                counter++;
            }
            objectMapper.writeValue(file, response);

            List<Post> postList = constructToEntity(content);
            return postRepository.saveAll(postList);
        } else {
            throw new Exception("GPT回傳的錯誤碼: " + response.getStatusCodeValue());
        }
    }

    private List<Post> constructToEntity(String content) throws JsonProcessingException {

        List<BadmintonPostDTO> postDTOs = objectMapper.readValue(content, new TypeReference<List<BadmintonPostDTO>>() {
        });

        List<Post> postList = new ArrayList<>();
        for (BadmintonPostDTO postDTO : postDTOs) {
            Post post = new Post();
            post.setName(postDTO.getName());
            post.setUserId(postDTO.getUserId());
            post.setPlace(postDTO.getPlace());
            post.setStartTime(postDTO.getStartTime());
            post.setDayOfWeek(postDTO.getStartTime());
            post.setEndTime(postDTO.getEndTime());
            post.setLevel(postDTO.getLevel());
            post.setFee(postDTO.getFee());
            post.setDuration(postDTO.getDuration());
            post.setBrand(postDTO.getBrand());
            post.setContact(postDTO.getContact());
            post.setParkInfo(postDTO.getParkInfo());
            post.setType(postDTO.getType());
            post.setAirConditioner(postDTO.getAirConditioner());
            post.setCreationDate(sdf.format(new Date()));
            postList.add(post);
        }
        return postList;
    }


    private String extractContent(ChatCompletionResponse responseBody) {
        if (responseBody != null && !responseBody.getChoices().isEmpty()) {
            ChatCompletionResponse.Choice choice = responseBody.getChoices().get(0);
            if (choice != null && choice.getMessage() != null) {
                String content = choice.getMessage().getContent().trim();

                // 去掉反引號
                if (content != null) {
                    content = content.replace("```json", "").replace("```", "").trim();
                }

                return content;
            }
        }
        return null;
    }

    private String createPayload(String prompt) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "gpt-4o-mini");

        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        payload.put("messages", new Object[]{message});

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
