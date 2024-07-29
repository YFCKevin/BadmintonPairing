package com.yfckevin.badmintonPairing.controller;

import com.yfckevin.badmintonPairing.dto.PostDTO;
import com.yfckevin.badmintonPairing.dto.SearchDTO;
import com.yfckevin.badmintonPairing.entity.Leader;
import com.yfckevin.badmintonPairing.entity.Post;
import com.yfckevin.badmintonPairing.service.LeaderService;
import com.yfckevin.badmintonPairing.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
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
    public String posts(Model model) throws ParseException {
        logger.info("[posts]");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime threeDaysLater = now.plusDays(3);
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

        return "post";
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
        Leader leader = leaderMap.get(post.getUserId());
        if (leader != null) {
            postDTO.setLink(leader.getLink());
        }


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



}
