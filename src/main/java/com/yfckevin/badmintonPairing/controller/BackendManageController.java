package com.yfckevin.badmintonPairing.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.yfckevin.badmintonPairing.ConfigProperties;
import com.yfckevin.badmintonPairing.dto.*;
import com.yfckevin.badmintonPairing.entity.*;
import com.yfckevin.badmintonPairing.enums.AirConditionerType;
import com.yfckevin.badmintonPairing.enums.TemplateType;
import com.yfckevin.badmintonPairing.exception.ResultStatus;
import com.yfckevin.badmintonPairing.service.*;
import com.yfckevin.badmintonPairing.utils.ConfigurationUtil;
import com.yfckevin.badmintonPairing.utils.FileUtils;
import com.yfckevin.badmintonPairing.utils.FlexMessageUtil;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
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
    private final CourtService courtService;
    private final TemplateSubjectService templateSubjectService;
    private final TemplateDetailService templateDetailService;
    private final FollowerService followerService;
    private final SimpleDateFormat sdf;
    private final DateTimeFormatter ddf;
    private final SimpleDateFormat svf;
    private final SimpleDateFormat ssf;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final FlexMessageUtil flexMessageUtil;
    private final SimpleDateFormat picSuffix;
    private final FileUtils fileUtils;

    public BackendManageController(ConfigProperties configProperties, LeaderService leaderService, PostService postService, OpenAiService openAiService, CourtService courtService, TemplateSubjectService templateSubjectService, TemplateDetailService templateDetailService, FollowerService followerService, @Qualifier("sdf") SimpleDateFormat sdf, DateTimeFormatter ddf, @Qualifier("svf") SimpleDateFormat svf, @Qualifier("ssf") SimpleDateFormat ssf, ObjectMapper objectMapper, RestTemplate restTemplate, FlexMessageUtil flexMessageUtil, SimpleDateFormat picSuffix, FileUtils fileUtils) {
        this.configProperties = configProperties;
        this.leaderService = leaderService;
        this.postService = postService;
        this.openAiService = openAiService;
        this.courtService = courtService;
        this.templateSubjectService = templateSubjectService;
        this.templateDetailService = templateDetailService;
        this.followerService = followerService;
        this.sdf = sdf;
        this.ddf = ddf;
        this.svf = svf;
        this.ssf = ssf;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.flexMessageUtil = flexMessageUtil;
        this.picSuffix = picSuffix;
        this.fileUtils = fileUtils;
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

        //取得所有球館資訊，配對用
        final List<Court> courtList = courtService.findAllByOrderByCreationDateAsc();
        model.addAttribute("courtList", courtList);

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

        if (!"All".equals(searchDTO.getLabelCourt()) && StringUtils.isNotBlank(searchDTO.getLabelCourt())) {
            postDTOList = postDTOList.stream().filter(p -> searchDTO.getLabelCourt().equals(p.getLabelCourt())).toList();
        }

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
     * 自動配對球館與貼文的功能
     * @param session
     * @return
     */
    @PostMapping("/autoMate")
    public ResponseEntity<?> autoMate(HttpSession session) {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[matePostAndCourt]");
        }
        ResultStatus resultStatus = new ResultStatus();

        final List<Post> todayPosts = postService.findTodayNewPosts(ssf.format(new Date()) + " 00:00:00", ssf.format(new Date()) + " 23:59:59");
        Map<String, List<String>> placeToIds = todayPosts.stream().collect(Collectors.toMap(
                Post::getPlace,
                post -> {
                    List<String> idList = new ArrayList<>();
                    idList.add(post.getId());
                    return idList;
                },
                (existingIds, newIds) -> {
                    List<String> ids = new ArrayList<>(existingIds);
                    ids.addAll(newIds);
                    return ids;
                }
        ));

        List<Court> courtList = courtService.findAllByOrderByCreationDateAsc();
        courtList = courtList.stream()
                .map(c -> {
                    //取得球館既有的貼文ids
                    List<String> postIds = Arrays.asList(c.getPostId().split(","));
                    //查出既有貼文細節資訊
                    List<Post> posts = postService.findByIdIn(postIds);

                    //把新貼文id根據place歸檔到所屬的球館的postId屬性
                    if (posts != null && !posts.isEmpty()) {

                        Set<String> places = posts.stream()
                                .map(Post::getPlace)
                                .collect(Collectors.toSet());

                        //執行比對
                        String newIds = places.stream()
                                .flatMap(p -> placeToIds.getOrDefault(p, Collections.emptyList()).stream())
                                .collect(Collectors.joining(","));

                        // 更新postId
                        String currentPostId = c.getPostId();
                        if (currentPostId != null && !currentPostId.isEmpty()) {
                            if (!currentPostId.endsWith(",")) {
                                currentPostId += ",";
                            }
                            c.setPostId(currentPostId + newIds);
                        } else {
                            c.setPostId(newIds);
                        }

                        //更新貼文的labelCourt屬性
                        final List<Post> postList = postService.findByIdIn(Arrays.asList(newIds.split(",")))
                                .stream().peek(p -> p.setLabelCourt(true)).toList();
                        postService.saveAll(postList);
                    }
                    return c;
                }).toList();

        courtService.saveAll(courtList);

        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");

        return ResponseEntity.ok(resultStatus);
    }


    @GetMapping("/notMatchedPosts")
    public ResponseEntity<?> notMatchedPosts (HttpSession session){

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[notMatchedPosts]");
        }
        ResultStatus resultStatus = new ResultStatus();

        final List<Post> todayNewPosts = postService.findTodayNewPosts(ssf.format(new Date()) + " 00:00:00", ssf.format(new Date()) + " 23:59:59");
        final Set<String> userIdList = todayNewPosts.stream().map(Post::getUserId).collect(Collectors.toSet());
        final Map<String, Leader> leaderMap = leaderService.findAllByUserIdIn(userIdList)
                .stream()
                .collect(Collectors.toMap(Leader::getUserId, Function.identity()));

        final List<PostDTO> todayPostDTOsNotYetMatched = todayNewPosts
                .stream().filter(p -> !p.isLabelCourt())
                .map(post -> {
                    try {
                        return constructPostDTO(leaderMap, post);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");
        resultStatus.setData(todayPostDTOsNotYetMatched);

        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 配對貼文與球館，將貼文id存入球館entity，以及在貼文標記labelCourt
     *
     * @param dto
     * @param session
     * @return
     */
    @PostMapping("/matePostsAndCourt")
    public ResponseEntity<?> matePostsAndCourt(@RequestBody MateDTO dto, HttpSession session) {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[matePostAndCourt]");
        }
        ResultStatus resultStatus = new ResultStatus();

        final Optional<Court> opt = courtService.findById(dto.getCourtId());
        if (opt.isPresent()) {
            final Court court = opt.get();
            final List<String> oldPostIds = Arrays.stream(court.getPostId().split(","))
                    .filter(id -> !id.trim().isEmpty())
                    .toList();
            Set<String> uniquePostId = new HashSet<>(oldPostIds);
            uniquePostId.addAll(dto.getMatePostIds());
            final String postId = uniquePostId.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            court.setPostId(postId);
            courtService.save(court);

            final List<Post> postList = postService.findByIdIn(uniquePostId.stream().toList()).stream()
                    .peek(p -> p.setLabelCourt(true)).toList();
            postService.saveAll(postList);

            resultStatus.setCode("C000");
            resultStatus.setMessage("成功");
        }
        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 篩選出費用不符合標準的貼文
     * @param session
     * @return
     */
    @GetMapping("/issueFee")
    public ResponseEntity<?> issueFee (HttpSession session){

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[matePostAndCourt]");
        }
        ResultStatus resultStatus = new ResultStatus();

        List<Post> todayPosts = postService.findTodayNewPosts(ssf.format(new Date()) + " 00:00:00", ssf.format(new Date()) + " 23:59:59");
        final Set<String> userIdList = todayPosts.stream().map(Post::getUserId).collect(Collectors.toSet());
        todayPosts = todayPosts.stream()
                .filter(p -> (p.getFee() / p.getDuration()) < 1.25 || (p.getFee() / p.getDuration()) > 2.09)
                .toList();
        final Map<String, Leader> leaderMap = leaderService.findAllByUserIdIn(userIdList)
                .stream()
                .collect(Collectors.toMap(Leader::getUserId, Function.identity()));

        final List<PostDTO> todayNewPostList = todayPosts
                .stream()
                .map(post -> {
                    try {
                        return constructPostDTO(leaderMap, post);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                })
                .sorted(Comparator.comparing(PostDTO::getStartTime))
                .toList();

        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");
        resultStatus.setData(todayNewPostList);
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

        String keywords = "教練|已滿|已額滿|場地出租|場地釋出|釋出|場地分享|場地轉讓|轉讓|場地轉租|徵場地|單打|售|練球|競標";
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

    /**
     * 打OpenAI的text completion
     *
     * @param date
     * @param userIdList
     * @param session
     * @return
     * @throws ParseException
     * @throws IOException
     */
    @PostMapping("/convertToPosts/{date}")
    public CompletableFuture<ResponseEntity<ResultStatus>> convertToPosts(@PathVariable String date, @RequestBody List<String> userIdList, HttpSession session) throws ParseException, IOException {

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

                return openAiService.generatePosts(prompt)
                        .thenApply(postList -> {
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
                            return ResponseEntity.ok(resultStatus);
                        })
                        .exceptionally(ex -> {
                            logger.error(ex.getMessage(), ex);
                            resultStatus.setCode("C999");
                            resultStatus.setMessage("例外發生");
                            return ResponseEntity.ok(resultStatus);
                        });

            } else {
                resultStatus.setCode("C006");
                resultStatus.setMessage("未選取檔案");
                return CompletableFuture.completedFuture(ResponseEntity.ok(resultStatus));
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            resultStatus.setCode("C999");
            resultStatus.setMessage("例外發生");
            return CompletableFuture.completedFuture(ResponseEntity.ok(resultStatus));
        }
    }

    /**
     * 取得尚未匯入DB的零打資訊
     *
     * @param date
     * @param session
     * @return
     * @throws ParseException
     * @throws IOException
     */
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


    /**
     * 導頁到球館管理介面
     *
     * @param session
     * @param model
     * @return
     */
    @GetMapping("/forwardCourtManagement")
    public String forwardCourtManagement(HttpSession session, Model model) {
        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[forwardCourtManagement]");
        } else {
            return "redirect:/backendLogin";
        }

        List<Court> courtList = courtService.findAllByOrderByCreationDateAsc();
        List<CourtDTO> courtDTOList = courtList.stream()
                .map(this::constructCourtDTO).toList();

        model.addAttribute("courtDTOList", courtDTOList);

        return "courtManagement";
    }


    /**
     * 新增球館
     * @param dto
     * @param session
     * @return
     */
    @PostMapping("/saveCourt")
    public ResponseEntity<?> saveCourt(@RequestBody CourtDTO dto, HttpSession session) {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[saveCourt]");
        }
        ResultStatus resultStatus = new ResultStatus();

        if (StringUtils.isBlank(dto.getId())) { //新增
            Court court = new Court();
            court.setName(dto.getName());
            court.setAddress(dto.getAddress());
            court.setLatitude(dto.getLatitude());
            court.setLongitude(dto.getLongitude());
            court.setCreationDate(sdf.format(new Date()));
            court.setPostId(dto.getPostId());
            court.setCity(dto.getCity());
            court.setDistrict(dto.getDistrict());
            courtService.save(court);
        } else {    //更新
            final Optional<Court> opt = courtService.findById(dto.getId());
            if (opt.isPresent()) {
                final Court court = opt.get();
                court.setLongitude(dto.getLongitude());
                court.setName(dto.getName());
                court.setAddress(dto.getAddress());
                court.setLatitude(dto.getLatitude());
                court.setPostId(dto.getPostId());
                court.setDistrict(dto.getDistrict());
                court.setCity(dto.getCity());
                courtService.save(court);
            }
        }
        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");
        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 球館模糊查詢
     * @param searchDTO
     * @param session
     * @return
     */
    @PostMapping("/searchCourt")
    public ResponseEntity<?> searchCourt(@RequestBody SearchDTO searchDTO, HttpSession session) {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[searchCourt]");
        }
        ResultStatus resultStatus = new ResultStatus();

        final List<CourtDTO> courtDTOList = courtService.findCourtByCondition(searchDTO.getKeyword().trim())
                .stream().map(this::constructCourtDTOSimple).toList();

        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");
        resultStatus.setData(courtDTOList);
        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 用id找球館
     * @param id
     * @param session
     * @return
     */
    @GetMapping("/findCourtById/{id}")
    public ResponseEntity<?> findCourtById(@PathVariable String id, HttpSession session) {

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[findCourtById]");
        }
        ResultStatus resultStatus = new ResultStatus();

        final Optional<Court> opt = courtService.findById(id);
        if (opt.isPresent()) {
            final Court court = opt.get();
            CourtDTO courtDTO = constructCourtDTO(court);
            resultStatus.setCode("C000");
            resultStatus.setMessage("成功");
            resultStatus.setData(courtDTO);
        }
        return ResponseEntity.ok(resultStatus);
    }

    private CourtDTO constructCourtDTO(Court court) {
        CourtDTO courtDTO = new CourtDTO();
        courtDTO.setAddress(court.getAddress());
        courtDTO.setLatitude(court.getLatitude());
        courtDTO.setLongitude(court.getLongitude());
        courtDTO.setName(court.getName());
        courtDTO.setId(court.getId());
        courtDTO.setCreationDate(court.getCreationDate());
        List<String> postIdsList = (court.getPostId() == null || court.getPostId().trim().isEmpty())
                ? Collections.emptyList()
                : Arrays.asList(court.getPostId().split(","));
        final List<PostDTO> postDTOList = postService.findByIdIn(postIdsList)
                .stream().map(p -> {
                    try {
                        return constructPostDTO(p);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();
        courtDTO.setPostDTOList(postDTOList);
        return courtDTO;
    }


    private CourtDTO constructCourtDTOSimple(Court court) {
        CourtDTO courtDTO = new CourtDTO();
        courtDTO.setAddress(court.getAddress());
        courtDTO.setLatitude(court.getLatitude());
        courtDTO.setLongitude(court.getLongitude());
        courtDTO.setName(court.getName());
        courtDTO.setId(court.getId());
        courtDTO.setCreationDate(court.getCreationDate());
        courtDTO.setPostId(court.getPostId());
        return courtDTO;
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
        postDTO.setLabelCourt(String.valueOf(post.isLabelCourt()));

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

            postDTO.setTime(formattedStartDate + "(" + dayOfWeekFormatted + ") " + formattedStartTime + " - " + formattedEndTime + " (" + formatDuration(post.getDuration()) + "h)");
        }

        return postDTO;
    }

    public String formatDuration(double duration) {
        double hours = duration / 60;
        if (hours == (int) hours) {
            // 如果是整數
            return String.format("%.0f", hours);
        } else {
            String hoursStr = String.format("%.2f", hours);
            if (hoursStr.endsWith("0")) {
                // 去掉末尾的零
                hoursStr = hoursStr.substring(0, hoursStr.length() - 1);
            }
            return hoursStr;
        }
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


    /**
     * 導頁到模板管理介面
     *
     * @param session
     * @param model
     * @return
     */
    @GetMapping("/forwardTemplateManagement")
    public String forwardTemplateManagement(HttpSession session, Model model) {
        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[forwardTemplateManagement]");
        } else {
            return "redirect:/backendLogin";
        }
        List<TemplateDTO> templateDTOList = new ArrayList<>();
        final List<TemplateSubject> subjectList = templateSubjectService.findAllAndOrderByCreationDate();
        templateDTOList = subjectList.stream().map(this::constructTemplateDTO).toList();

        model.addAttribute("templateDTOList", templateDTOList);

        return "backend/templateManagement";
    }

    private TemplateDTO constructTemplateDTO(TemplateSubject s) {
        TemplateDTO dto = new TemplateDTO();
        dto.setSubjectId(s.getId());
        dto.setSubjectTitle(s.getTitle());
        dto.setSubjectAltText(s.getAltText());
        dto.setCreationDate(s.getCreationDate());
        dto.setTemplateType(s.getTemplateType().getLabel());

        //組模板細節
        List<TemplateDetail> detailList = templateDetailService.findByIdIn(s.getDetailIds());
        List<TemplateDetailDTO> detailDTOList = detailList.stream().map(d -> {
            TemplateDetailDTO detailDTO = new TemplateDetailDTO();
            detailDTO.setId(d.getId());
            detailDTO.setMainTitle(d.getMainTitle());
            detailDTO.setSubTitle(d.getSubTitle());
            detailDTO.setTextContent(d.getTextContent());
            detailDTO.setCoverPath(d.getCover());
            detailDTO.setCreationDate(d.getCreationDate());
            detailDTO.setButtonName(d.getButtonName());
            detailDTO.setButtonUrl(d.getButtonUrl());
            return detailDTO;
        }).toList();

        dto.setDetailDTOList(detailDTOList);

        if (s.getUserIds().size() > 0) {
            dto.setUserIds(s.getUserIds());
        }

        return dto;
    }


    /**
     * 列出全部追蹤者
     *
     * @param session
     * @return
     */
    @GetMapping("/showFollowers")
    public ResponseEntity<?> showFollowers(HttpSession session) {
        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[showFollowers]");
        }
        ResultStatus resultStatus = new ResultStatus();
        List<Follower> followerList = followerService.findAll();
        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");
        resultStatus.setData(followerList);
        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 新增模板
     * @param dto
     * @param session
     * @return
     */
    @PostMapping("/addTemplateSubject")
    public ResponseEntity<?> addTemplateSubject(@RequestBody TemplateSubjectDTO dto, HttpSession session) {
        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[addTemplateSubject]");
        }
        ResultStatus resultStatus = new ResultStatus();

        if (StringUtils.isBlank(dto.getId())) { //新增
            TemplateSubject templateSubject = new TemplateSubject();
            templateSubject.setTitle(dto.getTitle());
            templateSubject.setAltText(dto.getAltText());
            templateSubject.setCreationDate(sdf.format(new Date()));
            templateSubject.setUserIds(dto.getUserIds());
            templateSubject.setTemplateType(TemplateType.valueOf(dto.getTemplateType()));
            final TemplateSubject savedTemplateSubject = templateSubjectService.save(templateSubject);
            resultStatus.setCode("C000");
            resultStatus.setMessage("成功");
            resultStatus.setData(savedTemplateSubject);
        }
        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 更換發送的追蹤者
     * @param dto
     * @param session
     * @return
     */
    @PostMapping("/changeSendPeople")
    public ResponseEntity<?> changeSendPeople (@RequestBody PushRequestDTO dto, HttpSession session){
        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[changeSendPeople]");
        }
        ResultStatus resultStatus = new ResultStatus();

        final Optional<TemplateSubject> subjectOpt = templateSubjectService.findById(dto.getSubjectId());
        if (subjectOpt.isEmpty()) {
            resultStatus.setCode("C010");
            resultStatus.setMessage("查無模板");
        } else {
            final TemplateSubject templateSubject = subjectOpt.get();
            templateSubject.setUserIds(dto.getUserIdList());
            templateSubjectService.save(templateSubject);
            resultStatus.setCode("C000");
            resultStatus.setMessage("成功");
        }
        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 追蹤者模糊查詢
     * @param searchDTO
     * @param session
     * @return
     */
    @PostMapping("/searchFollower")
    public ResponseEntity<?> searchFollower (@RequestBody SearchDTO searchDTO, HttpSession session){
        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[searchFollower]");
        }
        ResultStatus resultStatus = new ResultStatus();

        List<Follower> followerList = followerService.searchFollower(searchDTO.getKeyword().trim());
        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");
        resultStatus.setData(followerList);

        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 模板模糊查詢
     * @param searchDTO
     * @param session
     * @return
     */
    @PostMapping("/templateSearch")
    public ResponseEntity<?> templateSearch (@RequestBody SearchDTO searchDTO, HttpSession session){
        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[templateSearch]");
        }
        ResultStatus resultStatus = new ResultStatus();

        List<TemplateSubject> subjectList = templateSubjectService.templateSearch(searchDTO.getKeyword().trim());
        List<TemplateDTO> templateDTOList = subjectList.stream().map(this::constructTemplateDTO).toList();

        resultStatus.setCode("C000");
        resultStatus.setMessage("成功");
        resultStatus.setData(templateDTOList);

        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 用id查找模板的userIdList
     * @param id
     * @param session
     * @return
     */
    @GetMapping("/findTemplateSubjectById/{id}")
    public ResponseEntity<?> findTemplateSubjectById (@PathVariable String id, HttpSession session){
        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[findTemplateSubjectById]");
        }
        ResultStatus resultStatus = new ResultStatus();

        final Optional<TemplateSubject> subjectOpt = templateSubjectService.findById(id);
        if (subjectOpt.isEmpty()) {
            resultStatus.setCode("C010");
            resultStatus.setMessage("查無模板");
        } else {
            final TemplateSubject templateSubject = subjectOpt.get();
            resultStatus.setCode("C000");
            resultStatus.setMessage("成功");
            resultStatus.setData(templateSubject.getUserIds()); //傳送userIds
        }
        return ResponseEntity.ok(resultStatus);
    }


    /**
     * 新增模板細節
     * @param dto
     * @param session
     * @return
     * @throws IOException
     */
    @PostMapping("/addTemplateDetail")
    public String addTemplateDetail(@ModelAttribute TemplateDetailDTO dto, HttpSession session) throws IOException {
        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[addTemplateDetail]");
        }
        ResultStatus resultStatus = new ResultStatus();

        if (StringUtils.isBlank(dto.getId())) { //新增
            Path uploadPath = Path.of(configProperties.getPicSavePath());
            if (!Files.exists(uploadPath)) {
                Files.createDirectory(uploadPath);
            }

            TemplateDetail templateDetail = new TemplateDetail();

            final MultipartFile nameFile = dto.getMultipartFile();

            if (nameFile != null && !nameFile.isEmpty() && nameFile.getSize() != 0) {
                final String extension = FilenameUtils.getExtension(nameFile.getOriginalFilename());
                String fileName = picSuffix.format(new Date()) + "." + extension;
                System.out.println("fileName: " + fileName);

                try {
                    System.out.println("上傳圖片");
                    fileUtils.saveUploadedFile(nameFile, configProperties.getPicSavePath() + fileName);
                } catch (IOException e) {
                    System.out.println("圖片上傳失敗");
                    logger.error(e.getMessage(), e);
                    resultStatus.setCode("C009");
                    resultStatus.setMessage("圖片上傳失敗");
                    return "/error/50x";
                }
                templateDetail.setCover(configProperties.getPicShowPath() + fileName);
            }

            templateDetail.setMainTitle(dto.getMainTitle());
            templateDetail.setSubTitle(dto.getSubTitle());
            templateDetail.setTextContent(dto.getTextContent());
            templateDetail.setButtonName(dto.getButtonName());
            templateDetail.setButtonUrl(dto.getButtonUrl());
            templateDetail.setCreationDate(sdf.format(new Date()));
            TemplateDetail savedTemplateDetail = templateDetailService.save(templateDetail);

            //模板細節的id存入模板主題內
            Optional<TemplateSubject> opt = templateSubjectService.findById(dto.getSubjectId());
            if (opt.isPresent()) {
                TemplateSubject templateSubject = opt.get();
                final List<String> detailIds = templateSubject.getDetailIds();
                detailIds.add(savedTemplateDetail.getId());
                templateSubject.setDetailIds(detailIds);
                templateSubjectService.save(templateSubject);
            }
        }

        return "redirect:/forwardTemplateManagement";
    }


    /**
     * 後台推播功能 (多人推播)
     * @param dto
     * @param session
     * @return
     * @throws JsonProcessingException
     */
    @PostMapping("/pushSelectedFollowers")
    public ResponseEntity<?> pushSelectedFollowers(@RequestBody PushRequestDTO dto, HttpSession session) throws JsonProcessingException {
        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[pushSelectedFollowers]");
        }
        ResultStatus resultStatus = new ResultStatus();

        try {

            final Optional<TemplateSubject> subjectOpt = templateSubjectService.findById(dto.getSubjectId());
            if (subjectOpt.isEmpty()) {
                resultStatus.setCode("C010");
                resultStatus.setMessage("查無模板");
                return ResponseEntity.ok(resultStatus);
            } else {
                final TemplateSubject templateSubject = subjectOpt.get();
                if (templateSubject.getUserIds().size() == 0){
                    resultStatus.setCode("C012");
                    resultStatus.setMessage("推播人數不得為0");
                    return ResponseEntity.ok(resultStatus);
                }
                final Optional<TemplateDetail> detailOpt = templateDetailService.findById(templateSubject.getDetailIds().get(0));
                if (detailOpt.isEmpty()) {
                    resultStatus.setCode("C011");
                    resultStatus.setMessage("查無模板細節");
                    return ResponseEntity.ok(resultStatus);
                } else {
                    final TemplateDetail templateDetail = detailOpt.get();
                    //組建傳給line的template
                    Map<String, Object> textImageTemplate = flexMessageUtil.assembleTextImageTemplate(templateDetail);
                    String url = "https://api.line.me/v2/bot/message/multicast";
                    Map<String, Object> data = new HashMap<>();
                    data.put("to", templateSubject.getUserIds());
                    data.put("messages", List.of(textImageTemplate));
                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(configProperties.getChannelAccessToken());
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(data, headers);
                    ResponseEntity<LineUserProfileResponseDTO> response = restTemplate.exchange(url, HttpMethod.POST, entity, LineUserProfileResponseDTO.class);

                    if (response.getStatusCode().is2xxSuccessful()) {
                        // 成功處理
                        templateSubject.setSendDate(sdf.format(new Date()));
                        templateSubjectService.save(templateSubject);
                        resultStatus.setCode("C000");
                        resultStatus.setMessage("成功");
                        return ResponseEntity.ok(resultStatus);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return ResponseEntity.ok(resultStatus);
    }


    private PostDTO constructPostDTO(Map<String, Leader> leaderMap, Post post) throws ParseException {
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
        postDTO.setLabelCourt(String.valueOf(post.isLabelCourt()));
        Leader leader = leaderMap.get(post.getUserId());
        if (leader != null) {
            postDTO.setLink(leader.getLink());
            postDTO.setShortLink("https://www.facebook.com/" + leader.getUserId());
        }


        if (post.getStartTime() != null && post.getEndTime() != null) {
            LocalDateTime startDateTime = LocalDateTime.parse(post.getStartTime(), ddf);
            LocalDateTime endDateTime = LocalDateTime.parse(post.getEndTime(), ddf);

            // 取得星期
            DayOfWeek dayOfWeek = startDateTime.getDayOfWeek();
            // 格式化星期
            String dayOfWeekFormatted = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.TAIWAN);
            postDTO.setDayOfWeek(dayOfWeekFormatted);
            final String formattedStartDate = startDateTime.format(DateTimeFormatter.ofPattern("MM/dd"));
            final String formattedStartTime = startDateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            final String formattedEndTime = endDateTime.format(DateTimeFormatter.ofPattern("HH:mm"));

            postDTO.setTime(formattedStartDate + "(" + dayOfWeekFormatted + ") " + formattedStartTime + " - " + formattedEndTime + " (" + formatDuration(post.getDuration()) + "h)");
        }

        return postDTO;
    }
}
