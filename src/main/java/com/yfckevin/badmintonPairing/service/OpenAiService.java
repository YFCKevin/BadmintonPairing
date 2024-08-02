package com.yfckevin.badmintonPairing.service;

import com.yfckevin.badmintonPairing.entity.Post;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface OpenAiService {
    CompletableFuture<List<Post>> generatePosts(String prompt) throws Exception;
}
