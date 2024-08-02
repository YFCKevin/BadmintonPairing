package com.yfckevin.badmintonPairing.service;

import com.yfckevin.badmintonPairing.entity.Post;

import java.util.List;

public interface OpenAiService {
    List<Post> generatePosts(String prompt) throws Exception;
}
