package com.yfckevin.badmintonPairing.config;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

@Configuration
public class ElasticsearchConfig {
    private final ElasticsearchProperties elasticsearchProperties;

    public ElasticsearchConfig(ElasticsearchProperties elasticsearchProperties) {
        this.elasticsearchProperties = elasticsearchProperties;
    }

    @Bean
    public RestHighLevelClient client() {

        HttpHost httpHost = new HttpHost(elasticsearchProperties.getHostname(), elasticsearchProperties.getPort(), "http");

        RestClientBuilder restClientBuilder = RestClient.builder(httpHost);

        if (elasticsearchProperties.getUsername() != null && elasticsearchProperties.getPassword() != null) {
            restClientBuilder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.addInterceptorFirst(
                            (HttpRequestInterceptor) (request, context) -> {
                                String auth = elasticsearchProperties.getUsername() + ":" + elasticsearchProperties.getPassword();
                                String encodedAuth = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
                                request.setHeader("Authorization", encodedAuth);
                            })
            );
        }

        return new RestHighLevelClient(restClientBuilder);
    }
}
