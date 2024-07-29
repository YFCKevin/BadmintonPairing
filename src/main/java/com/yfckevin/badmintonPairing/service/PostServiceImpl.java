package com.yfckevin.badmintonPairing.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.yfckevin.badmintonPairing.ConfigProperties;
import com.yfckevin.badmintonPairing.dto.DataCleaningDTO;
import com.yfckevin.badmintonPairing.dto.RequestPostDTO;
import com.yfckevin.badmintonPairing.entity.Post;
import com.yfckevin.badmintonPairing.repository.PostRepository;
import com.yfckevin.badmintonPairing.utils.ConfigurationUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PostServiceImpl implements PostService {
    Logger logger = LoggerFactory.getLogger(PostServiceImpl.class);
    private final SimpleDateFormat svf;
    private final SimpleDateFormat ssf;
    private final ObjectMapper objectMapper;
    private final MongoTemplate mongoTemplate;
    private final ConfigProperties configProperties;
    private final PostRepository postRepository;

    public PostServiceImpl(@Qualifier("svf") SimpleDateFormat svf, @Qualifier("ssf") SimpleDateFormat ssf, ObjectMapper objectMapper, MongoTemplate mongoTemplate, ConfigProperties configProperties,
                           PostRepository postRepository) {
        this.svf = svf;
        this.ssf = ssf;
        this.objectMapper = objectMapper;
        this.mongoTemplate = mongoTemplate;
        this.configProperties = configProperties;
        this.postRepository = postRepository;
    }

    /**
     * 清多空格 -> 去掉查看更多 -> 分類零打與轉讓 -> 寫入json file -> 組建call GPT的prompt的資料型態的文字檔
     *
     * @return 資料清洗成功的筆數
     * @throws IOException
     */
    @Override
    public int dataCleaning(String filePath) throws IOException {
        Pattern pattern = Pattern.compile("\\s+");
        Pattern courtPattern = Pattern.compile("場地出租|場地釋出|釋出|場地分享|場地轉讓|轉讓|場地轉租");

        List<RequestPostDTO> posts = convertJsonToDto(filePath);

        List<DataCleaningDTO> disposableDataList = new ArrayList<>();
        List<DataCleaningDTO> releaseDataList = new ArrayList<>();
        posts.stream()
                .filter(post -> !post.getPostContent().contains("查看更多"))  // 去掉有「查看更多」的資料
                .filter(post -> StringUtils.isNotBlank(post.getUserId()))   // 去掉userId是空值的貼文
                .filter(post -> StringUtils.isNotBlank(post.getPostContent()))  // 去掉貼文內容是空值的
                .forEach(post -> {
                    // 去掉多空格
                    Matcher matcher = pattern.matcher(post.getPostContent());
                    post.setPostContent(matcher.replaceAll(""));

                    DataCleaningDTO dto = new DataCleaningDTO();
                    dto.setName(post.getName());
                    dto.setPostContent(post.getPostContent());
                    dto.setUserId(post.getUserId());

                    // 標記資料(零打 or 場地轉讓)
                    if (courtPattern.matcher(post.getPostContent()).find()) {
                        releaseDataList.add(dto);
                    } else {
                        disposableDataList.add(dto);
                    }
                });

        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // 寫入 JSON 到 data_disposable.json，不覆蓋既有文件
        String disposableFilePath = saveJsonToFile(disposableDataList, "data_disposable");
        // 寫入 JSON 到 data_release.json，不覆蓋既有文件
        String releaseFilePath = saveJsonToFile(releaseDataList, "data_release");

        //組建喂GPT的prompt的資料型態的文字檔，不覆蓋既有文件
        if (new File(disposableFilePath).exists()) {
            constructPrompt(disposableFilePath, "prompt_disposable");
        }
        if (new File(releaseFilePath).exists()) {
            constructPrompt(releaseFilePath, "prompt_release");
        }

        return releaseDataList.size() + disposableDataList.size();
    }


    private void constructPrompt(String filePath, String fileName) {
        String date = svf.format(new Date());
        String outputTextFilePath = configProperties.getFileSavePath() + date + "-" + fileName + ".txt";
        File file = new File(outputTextFilePath);

        try {
            // 將 JSON 資料轉換成 List<Map<String, String>>
            List<Map<String, String>> postList = objectMapper.readValue(new File(filePath), new TypeReference<>() {});

            // 使用 StringBuilder 來構建文件的內容
            StringBuilder builder = new StringBuilder();

            // 如果文件存在，讀取現有內容並存入 StringBuilder
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                }
            }

            // 將新貼文資料 append 到 builder
            for (Map<String, String> post : postList) {
                String name = post.get("name");
                String postContent = post.get("postContent");
                String userId = post.get("userId");
                builder.append(name).append(" | ").append(userId).append("::: ").append(postContent).append(" /// ");
            }

            // 去掉最後的 " /// "
            if (builder.length() > 0) {
                builder.setLength(builder.length() - 4);
            }

            //  清空舊資料，再利用 FileWriter 把 builder 資料寫入
            try (FileWriter fileWriter = new FileWriter(outputTextFilePath, false)) {
                fileWriter.write(builder.toString());
            }

            logger.info("轉換完成，結果已寫入 {}", outputTextFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    /**
     * 總檔(generalFile.json)比對爬蟲來的dailyPosts.json，最後獲取新貼文的資訊
     * @return 新貼文存放的json檔位置，供後續data clean使用
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    public String getDifferencePostsAndSaveInGeneralFileAndReturnFilePath(List<RequestPostDTO> dailyPosts) throws IOException, InterruptedException {

        // 讀取json檔的資料並轉換成List<RequestPostDTO>形式
        ConfigurationUtil.Configuration();
        File generalFile = new File(configProperties.getFileSavePath() + "generalFile.json");
        TypeRef<List<RequestPostDTO>> typeRef = new TypeRef<>() {
        };
        List<RequestPostDTO> generalDatas = JsonPath.parse(generalFile).read("$", typeRef);

        //取出新貼文的邏輯，並放入differencePosts
        List<RequestPostDTO> differencePosts = dailyPosts.stream()
                .filter(post -> generalDatas.stream()
                        //用團主編號userId和貼文內容文章postContent比對，取出不同的
                        .noneMatch(r -> r.getUserId().equals(post.getUserId()) &&
                                r.getPostContent().equals(post.getPostContent())))
                .peek(post -> post.setCreationDate(ssf.format(new Date()))) // 壓匯入日期yyyy-MM-dd
                .toList();

        // 將新貼文匯入到generalFile歸檔
        saveInGeneralFile(generalFile, differencePosts);

        // 將新貼文寫入dailyPosts.json，並回傳檔案路徑，後續做data clean
        return saveJsonFileForDataCleaning(differencePosts);
    }


    @Override
    public List<Post> findPostByConditions(String keyword, String startDate, String endDate) throws ParseException {
        List<Criteria> andCriterias = new ArrayList<>();
        List<Criteria> orCriterias = new ArrayList<>();

        Criteria criteria = Criteria.where("deletionDate").exists(false);

        if (StringUtils.isNotBlank(keyword)) {
            Criteria criteria_name = Criteria.where("name").regex(keyword, "i");
            Criteria criteria_place = Criteria.where("place").regex(keyword, "i");
            Criteria criteria_brand = Criteria.where("brand").regex(keyword, "i");
            Criteria criteria_packInfo = Criteria.where("packInfo").regex(keyword, "i");
            orCriterias.add(criteria_name);
            orCriterias.add(criteria_place);
            orCriterias.add(criteria_brand);
            orCriterias.add(criteria_packInfo);
        }

        if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
            Criteria criteria_start = Criteria.where("startTime").gte(startDate);
            Criteria criteria_end = Criteria.where("endTime").lte(endDate);
            andCriterias.add(criteria_start);
            andCriterias.add(criteria_end);
        }

        if(!orCriterias.isEmpty()) {
            criteria = criteria.orOperator(orCriterias.toArray(new Criteria[0]));
        }
        if(!andCriterias.isEmpty()) {
            criteria = criteria.andOperator(andCriterias.toArray(new Criteria[0]));
        }

        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Order.desc("startTime")));

        return mongoTemplate.find(query, Post.class);
    }

    /**
     * 用Leader的userId比對所有的Post，取出該userId最新一則貼文的貼文是過期的 (利用endTime與toady比對)
     * @return
     */
    @Override
    public List<Post> getPassPostsByLeadersAndTodayBefore() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        LookupOperation lookupOperation = LookupOperation.newLookup()
                .from("leader")
                .localField("userId")
                .foreignField("userId")
                .as("leaderInfo");

        MatchOperation matchOperation = Aggregation.match(Criteria.where("leaderInfo").ne(new Object[]{}));

        SortOperation sortOperation = Aggregation.sort(Sort.by("userId").ascending().and(Sort.by("endTime").descending()));

        GroupOperation groupOperation = Aggregation.group("userId")
                .first(Aggregation.ROOT).as("lastPost");

        MatchOperation matchEndTime = Aggregation.match(Criteria.where("lastPost.endTime").lt(today + " 23:59:59"));

        ReplaceRootOperation replaceRootOperation = Aggregation.replaceRoot("lastPost");

        Aggregation aggregation = Aggregation.newAggregation(
                lookupOperation,
                matchOperation,
                sortOperation,
                groupOperation,
                matchEndTime,
                replaceRootOperation
        );

        AggregationResults<Post> results = mongoTemplate.aggregate(aggregation, "post", Post.class);

        return results.getMappedResults();
    }

    @Override
    public void save(Post post) {
        postRepository.save(post);
    }

    @Override
    public Optional<Post> findById(String id) {
        return postRepository.findById(id);
    }

    @Override
    public List<Post> findTodayNewPosts(String startOfToday, String endOfToday) {
        List<Criteria> andCriterias = new ArrayList<>();

        Criteria criteria = Criteria.where("deletionDate").exists(false);

        if (StringUtils.isNotBlank(startOfToday) && StringUtils.isNotBlank(endOfToday)) {
            Criteria criteria_start = Criteria.where("creationDate").gte(startOfToday);
            Criteria criteria_end = Criteria.where("creationDate").lte(endOfToday);
            andCriterias.add(criteria_start);
            andCriterias.add(criteria_end);
        }

        if(!andCriterias.isEmpty()) {
            criteria = criteria.andOperator(andCriterias.toArray(new Criteria[0]));
        }

        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Order.desc("startTime")));

        return mongoTemplate.find(query, Post.class);
    }

    private String saveJsonFileForDataCleaning(List<RequestPostDTO> differencePosts) throws IOException {
        File outputFile = new File(configProperties.getFileSavePath() + "dailyPosts.json");
        objectMapper.writeValue(outputFile, differencePosts);
        return outputFile.getAbsolutePath();
    }


    private void saveInGeneralFile(File resultFile, List<RequestPostDTO> differencePosts) throws IOException {

        List<RequestPostDTO> existingData;
        if (resultFile.exists()) {
            existingData = objectMapper.readValue(resultFile, new TypeReference<>() {
            });
        } else {
            existingData = new ArrayList<>();
        }
        existingData.addAll(differencePosts);
        objectMapper.writeValue(resultFile, existingData);
    }


    private String saveJsonToFile(List<DataCleaningDTO> dataList, String baseFileName) throws IOException {
        String fileName = baseFileName + ".json";
        String date = svf.format(new Date());
        File file = new File(configProperties.getFileSavePath() + date + "-" + fileName);
        List<DataCleaningDTO> existingDataList = new ArrayList<>();
        if (file.exists()) {
            existingDataList = objectMapper.readValue(file, objectMapper.getTypeFactory().constructCollectionType(List.class, DataCleaningDTO.class));
        }
        existingDataList.addAll(dataList);
        objectMapper.writeValue(file, existingDataList);
        return file.getAbsolutePath(); // return file的絕對路徑
    }


    private List<RequestPostDTO> convertJsonToDto(String filePath) throws IOException {
        ConfigurationUtil.Configuration();
        File file = new File(filePath);
        TypeRef<List<RequestPostDTO>> typeRef = new TypeRef<>() {
        };
        return JsonPath.parse(file).read("$", typeRef);
    }
}
