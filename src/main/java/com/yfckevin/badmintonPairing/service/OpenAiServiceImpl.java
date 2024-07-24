package com.yfckevin.badmintonPairing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yfckevin.badmintonPairing.ConfigProperties;
import com.yfckevin.badmintonPairing.dto.BadmintonPostDTO;
import com.yfckevin.badmintonPairing.entity.Post;
import com.yfckevin.badmintonPairing.repository.PostRepository;
import com.yfckevin.badmintonPairing.utils.ConfigurationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class OpenAiServiceImpl implements OpenAiService {
    Logger logger = LoggerFactory.getLogger(OpenAiServiceImpl.class);
    SimpleDateFormat svf = new SimpleDateFormat("yyyyMMdd");
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final ConfigProperties configProperties;
    private final PostRepository postRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiServiceImpl(ConfigProperties configProperties, PostRepository postRepository, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.configProperties = configProperties;
        this.postRepository = postRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }


    @Override
    public void generatePosts() {

        ConfigurationUtil.Configuration();
        //先處理零打資訊
        File file = new File(configProperties.getFileSavePath() + svf.format(new Date()) + "-prompt_disposable.txt");
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }

            final String prompt = content.toString();

            //組final prompt
            String pre_prompt = "要求：\n" +
                    "0.資料格式是作者名稱 | userId::: 貼文內容 /// name::: 貼文內容\n" +
                    "1.羽球社團找人打球的資訊，要從作者名稱和貼文內容整理出JSON檔，欄位包括：\n" +
                    "name：作者名稱\n" +
                    "userId：userId\n" +
                    "place：活動地點\n" +
                    "startTime：開始時間，格式為2024-MM-dd HH:mm:ss\n" +
                    "endTime：結束時間，格式為2024-MM-dd HH:mm:ss\n" +
                    "level：程度，用字串表示\n" +
                    "fee：費用，僅取數字\n" +
                    "duration：時長，以分鐘表示，且為數字型態\n" +
                    "brand：使用的球種品牌，若無則留空\n" +
                    "contact：聯絡方式，若無則留空\n" +
                    "parkInfo：關於停車場的相關資訊，若無則留空\n" +
                    "type：固定為\"disposable\"\n" +
                    "airConditioner：冷氣資訊，以字串表示，present:有，absent:無，no_mention:未標示\n" +
                    "\n" +
                    "3.每則貼文內容可能包含多個打球資訊，必須分開成獨立的JSON物件。\n" +
                    "4.確保只匯出json檔";

            callOpenAI(pre_prompt + "\n" + prompt);


        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void callOpenAI(String prompt) throws Exception {
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(configProperties.getApiKey());

        String data = createPayload(prompt);

        HttpEntity<String> entity = new HttpEntity<>(data, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("GPT回傳的status code: {}", response);
            String responseBody = response.getBody();
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
            postRepository.saveAll(postList);
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
            post.setDayOfWeek();
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


    private String extractContent(String responseBody) {

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = Optional.ofNullable(root)
                    .map(node -> node.path("choices"))
                    .filter(JsonNode::isArray)
                    .map(choices -> choices.get(0))
                    .map(choice -> choice.path("message"))
                    .map(message -> message.path("content"))
                    .map(JsonNode::asText)
                    .map(String::trim)
                    .orElse(null);

            // 去掉反引號
            if (content != null) {
                content = content.replace("```json", "").replace("```", "").trim();
            }

            return content;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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
