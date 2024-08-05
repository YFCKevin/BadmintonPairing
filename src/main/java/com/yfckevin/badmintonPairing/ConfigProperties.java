package com.yfckevin.badmintonPairing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigProperties {
    @Value("${config.apiKey}")
    private String apiKey;
    @Value("${spring.crawler.email}")
    private String crawlerEmail;
    @Value("${spring.crawler.password}")
    private String crawlerPassword;
    @Value("${spring.security.user.name}")
    private String username;
    @Value("${spring.security.user.password}")
    private String password;
    @Value("${config.jsonPath}")
    private String jsonPath;
    @Value("${config.fileSavePath}")
    private String fileSavePath;
    @Value("${config.picSavePath}")
    private String picSavePath;
    @Value("${config.gptBackupSavePath}")
    private String gptBackupSavePath;
    @Value("${config.crawlerDomain}")
    private String crawlerDomain;
    @Value("${google.client.id}")
    private String clientId;
    @Value("${google.client.secret}")
    private String clientSecret;
    @Value("${google.redirect.uri}")
    private String redirectUri;

    public String getCrawlerDomain() {
        return crawlerDomain;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getJsonPath() {
        return jsonPath;
    }

    public String getFileSavePath() {
        return fileSavePath;
    }

    public String getGptBackupSavePath() {
        return gptBackupSavePath;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getCrawlerEmail() {
        return crawlerEmail;
    }

    public String getCrawlerPassword() {
        return crawlerPassword;
    }

    public String getPicSavePath() {
        return picSavePath;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }
}
