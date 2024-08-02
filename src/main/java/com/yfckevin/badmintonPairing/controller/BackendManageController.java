package com.yfckevin.badmintonPairing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.yfckevin.badmintonPairing.ConfigProperties;
import com.yfckevin.badmintonPairing.dto.*;
import com.yfckevin.badmintonPairing.entity.Leader;
import com.yfckevin.badmintonPairing.entity.Post;
import com.yfckevin.badmintonPairing.enums.AirConditionerType;
import com.yfckevin.badmintonPairing.exception.ResultStatus;
import com.yfckevin.badmintonPairing.service.LeaderService;
import com.yfckevin.badmintonPairing.service.OpenAiService;
import com.yfckevin.badmintonPairing.service.PostService;
import com.yfckevin.badmintonPairing.utils.ConfigurationUtil;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
public class BackendManageController {
    Logger logger = LoggerFactory.getLogger(BackendManageController.class);
    private final ConfigProperties configProperties;
    private final LeaderService leaderService;
    private final PostService postService;
    private final OpenAiService openAiService;
    private final SimpleDateFormat sdf;
    private final DateTimeFormatter ddf;
    private final SimpleDateFormat svf;
    private final SimpleDateFormat ssf;
    private final ObjectMapper objectMapper;

    public BackendManageController(ConfigProperties configProperties, LeaderService leaderService, PostService postService, OpenAiService openAiService, @Qualifier("sdf") SimpleDateFormat sdf, DateTimeFormatter ddf, @Qualifier("svf") SimpleDateFormat svf, @Qualifier("ssf") SimpleDateFormat ssf, ObjectMapper objectMapper) {
        this.configProperties = configProperties;
        this.leaderService = leaderService;
        this.postService = postService;
        this.openAiService = openAiService;
        this.sdf = sdf;
        this.ddf = ddf;
        this.svf = svf;
        this.ssf = ssf;
        this.objectMapper = objectMapper;
    }

    /**
     * 導登入頁面
     *
     * @return
     */
    @GetMapping("/backendLogin")
    public String backendLogin() {
        logger.info("[backendLogin]");
        return "backend/login";
    }

    /**
     * 登入驗證
     *
     * @return
     */
    @PostMapping("/loginCheck")
    public ResponseEntity<?> loginCheck(@RequestBody LoginDTO dto, HttpSession session) throws IOException {
        logger.info("[loginCheck]");
        ResultStatus resultStatus = new ResultStatus();
        ConfigurationUtil.Configuration();
        File file = new File(configProperties.getJsonPath() + "backendAccount.js");
        TypeRef<LoginDTO> typeRef = new TypeRef<>() {
        };
        final LoginDTO loginDTO = JsonPath.parse(file).read("$", typeRef);
        if (!loginDTO.getAccount().equals(dto.getAccount()) || !loginDTO.getPassword().equals(dto.getPassword())) {
            resultStatus.setCode("C001");
            resultStatus.setMessage("帳號或密碼錯誤");
        } else {
            session.setAttribute("admin", loginDTO.getAccount());
            resultStatus.setCode("C000");
            resultStatus.setMessage("成功");
        }
        return ResponseEntity.ok(resultStatus);
    }

    /**
     * 登出
     *
     * @param session
     * @return
     */
    @GetMapping("/backendLogout")
    public String backendLogout(HttpSession session) {
        logger.info("[backendLogout]");
        session.removeAttribute("admin");
        return "redirect:/backendLogin";
    }

    /**
     * 導團主管理頁面
     *
     * @param session
     * @param model
     * @return
     */
    @GetMapping("/forwardLeaderManagement")
    public String forwardLeaderPage(HttpSession session, Model model) {
        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[forwardLeaderManagement]");
        } else {
            return "redirect:/backendLogin";
        }
        final List<LeaderDTO> leaderDTOList = leaderService.findAllAndOrderByCreationDate()
                .stream()
                .filter(l -> StringUtils.isBlank(l.getDeletionDate()))
                .map(BackendManageController::constructLeaderDTO).toList();

        model.addAttribute("leaderList", leaderDTOList);
        return "backend/leaderManagement";
    }

    /**
     * 新增/修改團主
     *
     * @param session
     * @return
     */
    @PostMapping("/saveLeader")
    public ResponseEntity<?> saveLeader(@RequestBody LeaderDTO dto, HttpSession session) {
        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[saveLeader]");
        }
        ResultStatus resultStatus = new ResultStatus();

        String regex = "groups/(\\d+)/user/(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(dto.getLink());

        if (StringUtils.isBlank(dto.getId())) { // 新增
            Leader leader = new Leader();
            leader.setName(dto.getName());
            leader.setLink(dto.getLink());
            if (matcher.find()) {
                leader.setGroupId(matcher.group(1));
                leader.setUserId(matcher.group(2));
            }
            leader.setCreationDate(sdf.format(new Date()));
            leaderService.save(leader);
            resultStatus.setCode("C000");
            resultStatus.setMessage("成功");
        } else {    // 修改
            leaderService.findById(dto.getId())
                    .map(leader -> {
                        leader.setName(dto.getName());
                        leader.setLink(dto.getLink());
                        if (matcher.find()) {
                            leader.setGroupId(matcher.group(1));
                            leader.setUserId(matcher.group(2));
                        }
                        leader.setModificationDate(sdf.format(new Date()));
                        leaderService.save(leader);
                        resultStatus.setCode("C000");
                        resultStatus.setMessage("成功");
                        return resultStatus;
                    })
                    .orElseGet(() -> {
                        resultStatus.setCode("C002");
                        resultStatus.setMessage("查無團主");
                        return resultStatus;
                    });
        }

        return ResponseEntity.ok(resultStatus);
    }

    /**
     * 刪除團主
     *
     * @param session
     * @return
     */
    @GetMapping("/deleteLeader/{id}")
    public ResponseEntity<?> deleteLeader(@PathVariable String id, HttpSession session) {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[deleteLeader]");
        }

        ResultStatus resultStatus = new ResultStatus();

        leaderService.findById(id)
                .map(leader -> {
                    leaderService.deleteById(id);
                    resultStatus.setCode("C000");
                    resultStatus.setMessage("成功");
                    return resultStatus;
                })
                .orElseGet(() -> {
                    resultStatus.setCode("C002");
                    resultStatus.setMessage("查無團主");
                    return resultStatus;
                });

        return ResponseEntity.ok(resultStatus);
    }

    /**
     * 查詢團主
     *
     * @param searchDTO
     * @param session
     * @return
     */
    @PostMapping("/leaderSearch")
    public ResponseEntity<?> leaderSearch(@RequestBody SearchDTO searchDTO, HttpSession session) {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[searchLeaders]");
        }

        ResultStatus resultStatus = new ResultStatus();

        List<LeaderDTO> leaderDTOList = leaderService.findLeaderByConditions(searchDTO.getKeyword().trim(), searchDTO.getStartDate(), searchDTO.getEndDate())
                .stream()
                .map(BackendManageController::constructLeaderDTO)
                .toList();

        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");
        resultStatus.setData(leaderDTOList);

        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 查詢單一團主資訊
     *
     * @param id
     * @param session
     * @return
     */
    @GetMapping("/findLeaderById/{id}")
    public ResponseEntity<?> findLeaderById(@PathVariable String id, HttpSession session) {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[findLeaderById]");
        }

        ResultStatus resultStatus = new ResultStatus();

        final Optional<Leader> leaderOpt = leaderService.findById(id);
        if (!leaderOpt.isPresent()) {
            resultStatus.setCode("C002");
            resultStatus.setMessage("查無團主");
        } else {
            final Leader leader = leaderOpt.get();
            resultStatus.setCode("C000");
            resultStatus.setMessage("成功");
            resultStatus.setData(leader);
        }

        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 導貼文管理頁面
     *
     * @param session
     * @param model
     * @return
     */
    @GetMapping("/forwardPostManagement")
    public String forwardPostManagement(HttpSession session, Model model) throws ParseException {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[forwardPostManagement]");
        } else {
            return "redirect:/backendLogin";
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfThreeDaysLater = now.withHour(23).withMinute(59).withSecond(59).withNano(0);
        String startOfTodayFormatted = startOfToday.format(ddf);
        String endOfTodayFormatted = endOfThreeDaysLater.format(ddf);

        final List<PostDTO> postDTOList = postService.findTodayNewPosts(startOfTodayFormatted, endOfTodayFormatted)
                .stream()
                .map(post -> {
                    try {
                        return constructPostDTO(post);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();

        model.addAttribute("postDTOList", postDTOList);

        return "backend/postManagement";
    }

    /**
     * 新增/修改貼文
     *
     * @param session
     * @return
     */
    @PostMapping("/savePost")
    public ResponseEntity<?> savePost(@RequestBody PostDTO dto, HttpSession session) {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[savePost]");
        }

        ResultStatus resultStatus = new ResultStatus();

        if (StringUtils.isBlank(dto.getId())) { //新增
            Post p = new Post();
            final Post post = constructPostEntity(p, dto);
            post.setCreationDate(sdf.format(new Date()));
            postService.save(post);
            resultStatus.setCode("C000");
            resultStatus.setMessage("成功");
        } else {    //修改
            postService.findById(dto.getId())
                    .map(p -> {
                        final Post post = constructPostEntity(p, dto);
                        p.setId(dto.getId());
                        p.setModificationDate(sdf.format(new Date()));
                        postService.save(post);
                        resultStatus.setCode("C000");
                        resultStatus.setMessage("成功");
                        return resultStatus;
                    })
                    .orElseGet(() -> {
                        resultStatus.setCode("C003");
                        resultStatus.setMessage("查無貼文");
                        return resultStatus;
                    });
        }

        return ResponseEntity.ok(resultStatus);
    }

    /**
     * 刪除貼文
     *
     * @param session
     * @return
     */
    @GetMapping("/deletePost/{id}")
    public ResponseEntity<?> deletePost(@PathVariable String id, HttpSession session) {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[deletePost]");
        }

        ResultStatus resultStatus = new ResultStatus();

        postService.findById(id)
                .map(post -> {
                    postService.deleteById(id);
                    resultStatus.setCode("C000");
                    resultStatus.setMessage("成功");
                    return resultStatus;
                })
                .orElseGet(() -> {
                    resultStatus.setCode("C003");
                    resultStatus.setMessage("查無貼文");
                    return resultStatus;
                });

        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 刪除全部貼文
     *
     * @param postIdList
     * @param session
     * @return
     */
    @PostMapping("/deleteAllPosts")
    public ResponseEntity<?> deleteAllPosts(@RequestBody List<String> postIdList, HttpSession session) {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[deleteAllPosts]");
        }

        ResultStatus resultStatus = new ResultStatus();

        postService.deleteByIdIn(postIdList);
        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");

        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 查詢貼文
     *
     * @param searchDTO
     * @param session
     * @return
     */
    @PostMapping("/postSearch")
    public ResponseEntity<?> postSearch(@RequestBody SearchDTO searchDTO, HttpSession session) throws ParseException {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[postSearch]");
        }

        ResultStatus resultStatus = new ResultStatus();

        List<PostDTO> postDTOList = postService.findPostByConditions(searchDTO.getKeyword().trim(), searchDTO.getStartDate() + " 00:00:00", searchDTO.getEndDate() + " 23:59:59")
                .stream()
                .map(post -> {
                    try {
                        return constructPostDTO(post);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();

        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");
        resultStatus.setData(postDTOList);

        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 查詢單一貼文
     *
     * @param id
     * @param session
     * @return
     */
    @GetMapping("/findPostById/{id}")
    public ResponseEntity<?> findPostById(@PathVariable String id, HttpSession session) {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[findPostById]");
        }

        ResultStatus resultStatus = new ResultStatus();

        postService.findById(id)
                .map(post -> {
                    resultStatus.setCode("C000");
                    resultStatus.setMessage("成功");
                    resultStatus.setData(post);
                    return resultStatus;
                })
                .orElseGet(() -> {
                    resultStatus.setCode("C003");
                    resultStatus.setMessage("查無貼文");
                    return resultStatus;
                });

        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 查找相同userId和startTime的Post
     *
     * @param session
     * @return
     */
    @GetMapping("/searchSamePosts")
    public ResponseEntity<?> searchSamePosts(HttpSession session) {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[searchSamePosts]");
        }

        ResultStatus resultStatus = new ResultStatus();

        final List<PostDTO> postDTOList = postService.findSamePosts().stream()
                .map(p -> {
                    PostDTO postDTO;
                    try {
                        postDTO = constructPostDTO(p);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                    return postDTO;
                }).toList();

        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");
        resultStatus.setData(postDTOList);

        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 導檔案管理頁面
     *
     * @param session
     * @param model
     * @return
     */
    @GetMapping("/forwardFileManagement")
    public String forwardFileManagement(HttpSession session, Model model) throws IOException {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[forwardFileManagement]");
        } else {
            return "redirect:/backendLogin";
        }

        final List<RequestPostDTO> postDTOList = constructPostDTOFromDailyPostsFile(svf.format(new Date()));
        model.addAttribute("postDTOList", postDTOList);

        try {
            Pattern pattern = Pattern.compile("(\\d+)-data_disposable\\.json");
            List<String> sortedDateList = Files.walk(Paths.get(configProperties.getFileSavePath()))
                    .filter(Files::isRegularFile)
                    .map(path -> pattern.matcher(path.getFileName().toString()))
                    .filter(Matcher::matches)
                    .map(matcher -> matcher.group(1))
                    .sorted((s1, s2) -> Integer.compare(Integer.parseInt(s2), Integer.parseInt(s1))) // 按數字大小降冪排序
                    .distinct() // 去除重複項
                    .map(d -> {
                        try {
                            return ssf.format(svf.parse(d));
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

            model.addAttribute("sortedDateList", sortedDateList);

            model.addAttribute("today", ssf.format(new Date()));

            // 取出喂openAI的貼文結果列表
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
            LocalDateTime endOfTodayLater = now.withHour(23).withMinute(59).withSecond(59).withNano(0);
            String startOfTodayFormatted = startOfToday.format(ddf);
            String endOfTodayFormatted = endOfTodayLater.format(ddf);

            final List<PostDTO> todayNewPostList = postService.findTodayNewPosts(startOfTodayFormatted, endOfTodayFormatted)
                    .stream()
                    .map(post -> {
                        try {
                            return constructPostDTO(post);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }).toList();

            model.addAttribute("postData", todayNewPostList);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return "backend/fileManagement";
    }

    /**
     * 查詢檔案
     *
     * @param dto
     * @param session
     * @return
     */
    @PostMapping("/searchFiles/{date}")
    public ResponseEntity<?> searchFiles(@RequestBody SearchDTO dto, @PathVariable String date, HttpSession session) throws IOException, ParseException {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[searchFiles]");
        }

        ResultStatus resultStatus = new ResultStatus();

        date = svf.format(ssf.parse(date)); // 日期更換回yyyyMMdd

        List<RequestPostDTO> postDTOList = constructPostDTOFromDailyPostsFile(date);

        postDTOList = postDTOList.stream()
                .filter(p -> p.getName().contains(dto.getKeyword()) ||
                        p.getPostContent().contains(dto.getKeyword()))
                .toList();

        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");
        resultStatus.setData(postDTOList);

        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 修改檔案內的貼文內容
     *
     * @param dto
     * @param session
     * @return
     * @throws IOException
     */
    @PostMapping("/editFile/{date}")
    public ResponseEntity<?> editFile(@RequestBody RequestPostDTO dto, @PathVariable String date, HttpSession session) throws IOException, ParseException {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[editFile]");
        }

        ResultStatus resultStatus = new ResultStatus();

        date = svf.format(ssf.parse(date)); // 日期更換回yyyyMMdd

        List<RequestPostDTO> postDTOList = constructPostDTOFromDailyPostsFile(date);
        postDTOList = postDTOList.stream()
                .map(p -> {
                    if (p.getUserId().equals(dto.getUserId())) {
                        p.setPostContent(dto.getPostContent());
                    }
                    return p;
                })
                .toList();

        File file = new File(configProperties.getFileSavePath() + date + "-data_disposable.json");
        objectMapper.writeValue(file, postDTOList);

        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");

        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 刪除檔案
     *
     * @param userId
     * @param session
     * @return
     * @throws IOException
     */
    @GetMapping("/deleteFile/{userId}/{date}")
    public ResponseEntity<?> deleteFile(@PathVariable String userId, @PathVariable String date, HttpSession session) throws IOException, ParseException {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[deleteFile]");
        }

        ResultStatus resultStatus = new ResultStatus();

        date = svf.format(ssf.parse(date)); // 日期更換回yyyyMMdd

        List<RequestPostDTO> postDTOList = constructPostDTOFromDailyPostsFile(date);
        postDTOList = postDTOList.stream()
                .filter(p -> !p.getUserId().equals(userId))
                .toList();

        File file = new File(configProperties.getFileSavePath() + date + "-data_disposable.json");
        objectMapper.writeValue(file, postDTOList);

        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");

        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 取得單筆零打資訊的檔案內文
     *
     * @param date
     * @param session
     * @return
     * @throws IOException
     * @throws ParseException
     */
    @GetMapping("/showDisposableData/{date}")
    public ResponseEntity<?> showDisposableData(@PathVariable String date, HttpSession session) throws IOException, ParseException {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[showDisposableData]");
        }

        ResultStatus resultStatus = new ResultStatus();

        date = svf.format(ssf.parse(date)); // 日期更換回yyyyMMdd

        List<RequestPostDTO> postDTOList = constructPostDTOFromDailyPostsFile(date);

        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");
        resultStatus.setData(postDTOList);

        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 透過唯一值userId取得file
     *
     * @param userId
     * @param date
     * @param session
     * @return
     * @throws IOException
     * @throws ParseException
     */
    @GetMapping("/findFileByUserId/{userId}/{date}")
    public ResponseEntity<?> findFileByUserId(@PathVariable String userId, @PathVariable String date, HttpSession session) throws IOException, ParseException {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[findFileByUserId]");
        }

        ResultStatus resultStatus = new ResultStatus();

        date = svf.format(ssf.parse(date)); // 日期更換回yyyyMMdd

        List<RequestPostDTO> postDTOList = constructPostDTOFromDailyPostsFile(date);

        Optional<RequestPostDTO> optionalPost = postDTOList.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst();

        if (optionalPost.isPresent()) {
            resultStatus.setCode("C000");
            resultStatus.setMessage("成功");
            resultStatus.setData(optionalPost.get());
        } else {
            resultStatus.setCode("C004");
            resultStatus.setMessage("查無檔案內貼文");
        }
        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 篩選出問題文章
     *
     * @param session
     * @return
     */
    @GetMapping("/searchIssues/{date}")
    public ResponseEntity<?> searchIssues(@PathVariable String date, HttpSession session) throws IOException, ParseException {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[searchIssues]");
        }

        ResultStatus resultStatus = new ResultStatus();

        date = svf.format(ssf.parse(date)); // 日期更換回yyyyMMdd

        String keywords = "教練|場地出租|場地釋出|釋出|場地分享|場地轉讓|轉讓|場地轉租";
        Pattern issuePattern = Pattern.compile(keywords);

        List<RequestPostDTO> postDTOList = constructPostDTOFromDailyPostsFile(date);
        postDTOList = postDTOList.stream()
                .filter(p -> issuePattern.matcher(p.getPostContent()).find())
                .toList();

        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("keywords", keywords);
        dataMap.put("postDTOList", postDTOList);

        resultStatus.setData(dataMap);

        return ResponseEntity.ok(resultStatus);
    }


    @PostMapping("/convertToPosts/{date}")
    public ResponseEntity<?> convertToPosts(@PathVariable String date, @RequestBody List<String> userIdList, HttpSession session) throws ParseException, IOException {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[convertToPosts]");
        }

        ResultStatus resultStatus = new ResultStatus();

        try {

            date = svf.format(ssf.parse(date)); // 日期更換回yyyyMMdd

            List<RequestPostDTO> requestPostDTOList = constructPostDTOFromDailyPostsFile(date);
            requestPostDTOList = requestPostDTOList.stream()
                    .filter(p -> userIdList.contains(p.getUserId()))
                    .toList();

            if (requestPostDTOList.size() > 0) {
                final String prompt = constructPrompt(requestPostDTOList);
                // call openAi text completion
                final List<Post> postList = openAiService.generatePosts(prompt);
                final List<PostDTO> postDTOList = postList.stream().map(p -> {
                    try {
                        return constructPostDTO(p);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();

                // 結果呈現回前端
                resultStatus.setCode("C000");
                resultStatus.setMessage("成功");
                resultStatus.setData(postDTOList);
            } else {
                resultStatus.setCode("C006");
                resultStatus.setMessage("未選取檔案");
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            resultStatus.setCode("C999");
            resultStatus.setMessage("例外發生");
        }

        return ResponseEntity.ok(resultStatus);
    }


    @GetMapping("/getUnfinishedFiles/{date}")
    public ResponseEntity<?> getUnfinishedFiles(@PathVariable String date, HttpSession session) throws ParseException, IOException {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[findMissingFiles]");
        }

        ResultStatus resultStatus = new ResultStatus();

        date = svf.format(ssf.parse(date)); // 日期更換回yyyyMMdd

        List<RequestPostDTO> requestPostDTOList = constructPostDTOFromDailyPostsFile(date);

        final List<String> savedUserIdList = postService.getPostsForToday().stream().map(Post::getUserId).toList();

        requestPostDTOList = requestPostDTOList.stream()
                .filter(p -> !savedUserIdList.contains(p.getUserId()))
                .toList();

        if (requestPostDTOList.size() > 0) {
            resultStatus.setCode("C005");
            resultStatus.setMessage("尚有檔案未匯入資料庫");
            resultStatus.setData(requestPostDTOList);
        } else {
            resultStatus.setCode("C000");
            resultStatus.setMessage("成功");
        }

        return ResponseEntity.ok(resultStatus);
    }


    private String constructPrompt(List<RequestPostDTO> postDTOList) {
        // 使用 StringBuilder 來構建文件的內容
        StringBuilder builder = new StringBuilder();

        // 將新貼文資料 append 到 builder
        for (RequestPostDTO post : postDTOList) {
            String name = post.getName();
            String postContent = post.getPostContent();
            String userId = post.getUserId();
            builder.append(name).append(" | ").append(userId).append("::: ").append(postContent).append(" /// ");
        }

        // 去掉最後的 " /// "
        if (builder.length() > 0) {
            builder.setLength(builder.length() - 4);
        }

        logger.info("建構完成prompt");

        return builder.toString();
    }


    private List<RequestPostDTO> constructPostDTOFromDailyPostsFile(String date) throws IOException {
        ConfigurationUtil.Configuration();
        File file = new File(configProperties.getFileSavePath() + date + "-data_disposable.json");

        if (!file.exists()) {
            return Collections.emptyList();
        }
        TypeRef<List<RequestPostDTO>> typeRef = new TypeRef<>() {
        };
        return JsonPath.parse(file).read("$", typeRef);
    }


    private static LeaderDTO constructLeaderDTO(Leader leader) {
        LeaderDTO dto = new LeaderDTO();
        dto.setId(leader.getId());
        dto.setName(leader.getName());
        dto.setUserId(leader.getUserId());
        final String groupId = leader.getGroupId();
        dto.setGroupId(groupId);
        dto.setLink(leader.getLink());
        dto.setCreationDate(leader.getCreationDate());
        dto.setModificationDate(leader.getModificationDate());
        dto.setDeletionDate(leader.getDeletionDate());
        if ("392553431115145".equals(groupId)) {
            dto.setGroupName("大台北羽球同好交流版");
        } else if ("1882953728686436".equals(groupId)) {
            dto.setGroupName("新北市羽球臨打揪團");
        } else if ("480573685305042".equals(groupId)) {
            dto.setGroupName("台北基隆北北基羽球同好交流區");
        }
        return dto;
    }

    private PostDTO constructPostDTO(Post post) throws ParseException {
        PostDTO postDTO = new PostDTO();
        postDTO.setId(post.getId());
        postDTO.setCreationDate(post.getCreationDate());
        postDTO.setBrand(post.getBrand());
        postDTO.setDuration(post.getDuration());
        postDTO.setName(post.getName());
        postDTO.setContact(post.getContact());
        postDTO.setFee(post.getFee());
        postDTO.setAirConditioner(post.getAirConditioner().getLabel());
        postDTO.setType(post.getType());
        postDTO.setEndTime(post.getEndTime());
        postDTO.setStartTime(post.getStartTime());
        postDTO.setLevel(post.getLevel());
        postDTO.setParkInfo(post.getParkInfo());
        postDTO.setPlace(post.getPlace());
        postDTO.setUserId(post.getUserId());

        if (post.getStartTime() != null && post.getEndTime() != null) {
            LocalDateTime startDateTime = LocalDateTime.parse(post.getStartTime(), ddf);
            LocalDateTime endDateTime = LocalDateTime.parse(post.getEndTime(), ddf);

            // 取得星期
            DayOfWeek dayOfWeek = startDateTime.getDayOfWeek();
            // 格式化星期
            String dayOfWeekFormatted = dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.TAIWAN);

            final String formattedStartDate = startDateTime.format(DateTimeFormatter.ofPattern("MM/dd"));
            final String formattedStartTime = startDateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            final String formattedEndTime = endDateTime.format(DateTimeFormatter.ofPattern("HH:mm"));

            postDTO.setTime(formattedStartDate + "(" + dayOfWeekFormatted + ") " + formattedStartTime + " - " + formattedEndTime + " (" + post.getDuration() / 60 + "h)");
        }

        return postDTO;
    }


    private Post constructPostEntity(Post post, PostDTO postDTO) {
        post.setBrand(postDTO.getBrand());
        post.setDuration(postDTO.getDuration());
        post.setName(postDTO.getName());
        post.setContact(postDTO.getContact());
        post.setFee(postDTO.getFee());
        switch (postDTO.getAirConditioner()) {
            case "present" -> post.setAirConditioner(AirConditionerType.present);
            case "absent" -> post.setAirConditioner(AirConditionerType.absent);
            case "no_mention" -> post.setAirConditioner(AirConditionerType.no_mention);
        }
        post.setType(postDTO.getType());
        post.setEndTime(postDTO.getEndTime());
        post.setStartTime(postDTO.getStartTime());
        post.setLevel(postDTO.getLevel());
        post.setParkInfo(postDTO.getParkInfo());
        post.setPlace(postDTO.getPlace());
        post.setUserId(postDTO.getUserId());
        post.setStartTime(postDTO.getStartTime());
        post.setEndTime(postDTO.getEndTime());

        if (StringUtils.isNotBlank(postDTO.getStartTime())) {
            post.setDayOfWeek(postDTO.getStartTime());
        }

        if (StringUtils.isNotBlank(postDTO.getEndTime()) && StringUtils.isNotBlank(postDTO.getStartTime())) {
            LocalDateTime startDateTime = LocalDateTime.parse(postDTO.getStartTime(), ddf);
            LocalDateTime endDateTime = LocalDateTime.parse(postDTO.getEndTime(), ddf);
            long duration = java.time.Duration.between(startDateTime, endDateTime).toMinutes();
            post.setDuration((int) duration);

        }

        return post;
    }
}
