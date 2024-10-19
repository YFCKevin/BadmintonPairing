package com.yfckevin.badmintonPairing.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yfckevin.badmintonPairing.ConfigProperties;
import com.yfckevin.badmintonPairing.dto.PostDTO;
import com.yfckevin.badmintonPairing.entity.Leader;
import com.yfckevin.badmintonPairing.entity.Post;
import com.yfckevin.badmintonPairing.entity.TemplateDetail;
import com.yfckevin.badmintonPairing.entity.TemplateSubject;
import com.yfckevin.badmintonPairing.service.LeaderService;
import com.yfckevin.badmintonPairing.service.PostService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FlexMessageUtil {
    Logger logger = LoggerFactory.getLogger(FlexMessageUtil.class);

    private final PostService postService;
    private final LeaderService leaderService;
    private final DateTimeFormatter ddf;
    private final ConfigProperties configProperties;

    public FlexMessageUtil(PostService postService, LeaderService leaderService, DateTimeFormatter ddf, ConfigProperties configProperties) {
        this.postService = postService;
        this.leaderService = leaderService;
        this.ddf = ddf;
        this.configProperties = configProperties;
    }

    // 組建圖文輪詢
    public Map<String, Object> assembleImageCarouselTemplate(String startDate, String endDate) throws ParseException, JsonProcessingException {
        final List<Post> postList = postService.findPostByConditions("", startDate, endDate)
                .stream().filter(p -> StringUtils.isNotBlank(p.getUserId()) && StringUtils.isNotBlank(p.getStartTime()) && StringUtils.isNotBlank(p.getEndTime()))
                .limit(10).toList();

        if (postList.size() == 0) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", "查無零打資訊");
            return result;
        }

        final Set<String> userIdList = postList.stream().map(Post::getUserId).collect(Collectors.toSet());

        List<PostDTO> postDTOList = new ArrayList<>();
        final Map<String, Leader> leaderMap = leaderService.findAllByUserIdIn(userIdList)
                .stream()
                .filter(l -> StringUtils.isNotBlank(l.getLink()))
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

        // Flex Message
        Map<String, Object> template = new HashMap<>();
        template.put("type", "template");
        template.put("altText", "您收到羽球配對發給您的零打資訊");

        // Carousel內容
        Map<String, Object> carousel = new HashMap<>();
        carousel.put("type", "carousel");

        // 設定columns
        List<Map<String, Object>> columns = new ArrayList<>();

        for (PostDTO postDTO : postDTOList) { // 產生零打資訊column
            Map<String, Object> column = new HashMap<>();
            column.put("thumbnailImageUrl", "https://gurula.cc/badminton/images/column.jpg");
            column.put("imageBackgroundColor", "#FFFFFF");
            column.put("title", postDTO.getPlace()); // 打球地點
            column.put("text", postDTO.getTime()); // 零打日期時間

            // Default action (圖片)
            Map<String, String> defaultAction = new HashMap<>();
            defaultAction.put("type", "uri");
            defaultAction.put("label", "前往首頁");
            defaultAction.put("uri", "https://gurula.cc/badminton/index");
            column.put("defaultAction", defaultAction);

            // Actions
            List<Map<String, Object>> actions = new ArrayList<>();

            // 查看詳情的action
            Map<String, Object> viewDetailAction = new HashMap<>();
            viewDetailAction.put("type", "uri");
            viewDetailAction.put("label", "更多資訊");
            viewDetailAction.put("uri", configProperties.getGlobalDomain() + "card/" + postDTO.getId());
            actions.add(viewDetailAction);

            // 前往報名的action
            Map<String, Object> signupAction = new HashMap<>();
            signupAction.put("type", "uri");
            signupAction.put("label", "前往報名");
            signupAction.put("uri", postDTO.getShortLink());
            actions.add(signupAction);

            column.put("actions", actions);
            columns.add(column);
        }

        carousel.put("columns", columns);
        template.put("template", carousel);

        return template;
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

    public Map<String, Object> assembleTextImageTemplate(TemplateDetail templateDetail) {

        // Hero section
        Map<String, Object> hero = new HashMap<>();
        hero.put("type", "image");
        hero.put("url", templateDetail.getCover());
        hero.put("size", "full");
        hero.put("aspectRatio", "25:13");
        hero.put("aspectMode", "fit");
        hero.put("position", "relative");

        Map<String, Object> heroAction = new HashMap<>();
        heroAction.put("type", "uri");
        heroAction.put("uri", "https://gurula.cc/badminton/index");
        hero.put("action", heroAction);

        // Body section
        Map<String, Object> titleText = new HashMap<>();
        titleText.put("type", "text");
        titleText.put("text", templateDetail.getMainTitle());
        titleText.put("weight", "bold");
        titleText.put("size", "xl");
        titleText.put("margin", "md");

        Map<String, Object> subtitleText = new HashMap<>();
        subtitleText.put("type", "text");
        subtitleText.put("text", templateDetail.getSubTitle());
        subtitleText.put("weight", "regular");
        subtitleText.put("size", "md");
        subtitleText.put("color", "#999999");
        subtitleText.put("margin", "sm");

        Map<String, Object> singleTextContent = new HashMap<>();
        singleTextContent.put("type", "text");
        singleTextContent.put("text", templateDetail.getTextContent());
        singleTextContent.put("size", "sm");
        singleTextContent.put("color", "#666666");
        singleTextContent.put("wrap", true);  // 設定自動換行

        Map<String, Object> singleTextBox = new HashMap<>();
        singleTextBox.put("type", "box");
        singleTextBox.put("layout", "vertical");
        singleTextBox.put("margin", "lg");
        singleTextBox.put("spacing", "sm");
        singleTextBox.put("contents", new Object[]{singleTextContent});

        Map<String, Object> body = new HashMap<>();
        body.put("type", "box");
        body.put("layout", "vertical");
        body.put("contents", new Object[]{titleText, subtitleText, singleTextBox});

        // Footer section
        Map<String, Object> buttonAction = new HashMap<>();
        buttonAction.put("type", "uri");
        buttonAction.put("label", templateDetail.getButtonName());
        buttonAction.put("uri", templateDetail.getButtonUrl());

        Map<String, Object> button = new HashMap<>();
        button.put("type", "button");
        button.put("style", "link");
        button.put("action", buttonAction);

        Map<String, Object> footer = new HashMap<>();
        footer.put("type", "box");
        footer.put("layout", "vertical");
        footer.put("contents", new Object[]{button});

        // Combine into bubble
        Map<String, Object> bubble = new HashMap<>();
        bubble.put("type", "bubble");
        bubble.put("hero", hero);
        bubble.put("body", body);
        bubble.put("footer", footer);

        // Add the bubble into flex message wrapper
        Map<String, Object> flexMessage = new HashMap<>();
        flexMessage.put("type", "flex");
        flexMessage.put("altText", "羽球配對新服務上線 [地圖搜搜]");
        flexMessage.put("contents", bubble);

        return flexMessage;
    }
}