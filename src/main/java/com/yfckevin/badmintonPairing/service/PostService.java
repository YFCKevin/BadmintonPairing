package com.yfckevin.badmintonPairing.service;

import com.yfckevin.badmintonPairing.dto.RequestPostDTO;
import com.yfckevin.badmintonPairing.entity.Post;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface PostService {

    int dataCleaning(String filePath) throws IOException;
    String getDifferencePostsAndSaveInGeneralFileAndReturnFilePath(List<RequestPostDTO> dailyPosts) throws IOException, InterruptedException;
    List<Post> findPostByConditions(String keyword, String startDate, String endDate) throws ParseException;
    List<Post> getPassPostsByLeadersAndTodayBefore();

    void save(Post post);

    Optional<Post> findById(String id);

    List<Post> findTodayNewPosts(String startOfToday, String endOfToday);

    List<Post> findSamePosts();

    void deleteById(String id);

    List<Post> getPostsForToday();
}
