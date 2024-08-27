package com.yfckevin.badmintonPairing.controller;

import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.yfckevin.badmintonPairing.dto.PostDTO;
import com.yfckevin.badmintonPairing.dto.SearchDTO;
import com.yfckevin.badmintonPairing.entity.Leader;
import com.yfckevin.badmintonPairing.entity.Post;
import com.yfckevin.badmintonPairing.exception.ResultStatus;
import com.yfckevin.badmintonPairing.service.LeaderService;
import com.yfckevin.badmintonPairing.service.PostService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class PostController {

    private final PostService postService;
    private final LeaderService leaderService;
    private final DateTimeFormatter ddf;
    Logger logger = LoggerFactory.getLogger(PostController.class);

    public PostController(PostService postService, LeaderService leaderService, DateTimeFormatter ddf) {
        this.postService = postService;
        this.leaderService = leaderService;
        this.ddf = ddf;
    }


    /**
     * 取得全部貼文
     *
     * @return
     */
    @GetMapping("/posts")
    public String posts(Model model, HttpSession session) throws ParseException {
        logger.info("[posts]");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime threeDaysLater = now.plusWeeks(2);
        LocalDateTime endOfThreeDaysLater = threeDaysLater.withHour(23).withMinute(59).withSecond(59).withNano(0);
        String startOfTodayFormatted = startOfToday.format(ddf);
        String endOfThreeDaysLaterFormatted = endOfThreeDaysLater.format(ddf);

        List<Post> postList = postService.findPostByConditions("", startOfTodayFormatted, endOfThreeDaysLaterFormatted);
        final Set<String> userIdList = postList.stream().map(Post::getUserId).collect(Collectors.toSet());

        final Map<String, Leader> leaderMap = leaderService.findAllByUserIdIn(userIdList)
                .stream()
                .collect(Collectors.toMap(Leader::getUserId, Function.identity()));

        List<PostDTO> postDTOList = postList.stream()
                .map(post -> {
                    try {
                        return constructPostDTO(leaderMap, post);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                })
                .sorted(Comparator.comparing(PostDTO::getStartTime))
                .toList();

        model.addAttribute("posts", postDTOList);

        Credential credential = (Credential) session.getAttribute("credential");
        if (credential != null) {
            ClientParametersAuthentication clientAuth = (ClientParametersAuthentication) credential.getClientAuthentication();

            model.addAttribute("clientId", clientAuth.getClientId());

        }


        return "post";
    }


    /**
     * 取得每日新貼文
     * @param session
     * @return
     */
    @GetMapping("/getTodayNewPosts")
    public ResponseEntity<?> getTodayNewPosts (HttpSession session){

        final String member = (String) session.getAttribute("admin");
        if (member != null) {
            logger.info("[getTodayNewPosts]");
        }

        ResultStatus resultStatus = new ResultStatus();

        // 每日新貼文
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfTodayLater = now.withHour(23).withMinute(59).withSecond(59).withNano(0);
        String startOfTodayNewPosts = startOfToday.format(ddf);
        String endOfTodayNewPosts = endOfTodayLater.format(ddf);

        final List<Post> todayPosts = postService.findTodayNewPosts(startOfTodayNewPosts, endOfTodayNewPosts);
        final Set<String> userIdList = todayPosts.stream().map(Post::getUserId).collect(Collectors.toSet());
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
     * 用團主名、地點、用球、停車資訊、起迄時間做模糊查詢
     *
     * @param dto
     * @return
     */
    @PostMapping("/searchPosts")
    public ResponseEntity<?> searchPosts(@RequestBody SearchDTO dto) throws ParseException {
        logger.info("[searchPosts]");
        List<Post> postList = postService.findPostByConditions(dto.getKeyword().trim(), dto.getStartDate(), dto.getEndDate());

        final Set<String> userIdList = postList.stream().map(Post::getUserId).collect(Collectors.toSet());

        List<PostDTO> postDTOList = new ArrayList<>();
        final Map<String, Leader> leaderMap = leaderService.findAllByUserIdIn(userIdList)
                .stream()
                .collect(Collectors.toMap(Leader::getUserId, Function.identity()));
        postDTOList = postList.stream()
                .map(post -> {
                    try {
                        return constructPostDTO(leaderMap, post);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                })
                .sorted(Comparator.comparing(PostDTO::getStartTime))
                .toList();



        return ResponseEntity.ok(postDTOList);
    }



    @GetMapping("/chooseDayOfWeek/{day}")
    public ResponseEntity<?> chooseDayOfWeek (@PathVariable String day, HttpSession session){
        logger.info("[chooseDayOfWeek]");

        ResultStatus resultStatus = new ResultStatus();

        List<Post> postList = postService.findPostsByDaySorted(day, getNextDate(day).toString());

        final Set<String> userIdList = postList.stream().map(Post::getUserId).collect(Collectors.toSet());

        List<PostDTO> postDTOList = new ArrayList<>();
        final Map<String, Leader> leaderMap = leaderService.findAllByUserIdIn(userIdList)
                .stream()
                .collect(Collectors.toMap(Leader::getUserId, Function.identity()));
        postDTOList = postList.stream()
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
        resultStatus.setData(postDTOList);

        return ResponseEntity.ok(resultStatus);
    }

    public static LocalDate getNextDate(String dayOfWeekStr) {
        DayOfWeek dayOfWeek = DayOfWeek.valueOf(dayOfWeekStr.toUpperCase());
        LocalDate now = LocalDate.now();

        // 找到當前是星期幾
        DayOfWeek currentDayOfWeek = now.getDayOfWeek();

        // 計算相差天數
        int daysToAdd = dayOfWeek.getValue() - currentDayOfWeek.getValue();
        if (daysToAdd < 0) {
            daysToAdd += 7;
        }

        return now.plusDays(daysToAdd);
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

}
