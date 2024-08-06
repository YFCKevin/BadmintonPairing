package com.yfckevin.badmintonPairing.controller;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.yfckevin.badmintonPairing.ConfigProperties;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/oauth2")
public class AuthController {
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final ConfigProperties configProperties;

    public AuthController(ConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    @GetMapping("/authorize")
    public String authorize() throws IOException {
        String authorizationUrl = new AuthorizationCodeRequestUrl(
                "https://accounts.google.com/o/oauth2/auth",
                configProperties.getClientId())
                .setRedirectUri(configProperties.getRedirectUri())
                .setScopes(SCOPES)
                .build();

        return "redirect:" + authorizationUrl;
    }

    @GetMapping("/callback")
    public String callback(@RequestParam("code") String code, HttpSession session) {
        logger.info("[callback]");
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                    .setInstalled(new GoogleClientSecrets.Details()
                            .setClientId(configProperties.getClientId())
                            .setClientSecret(configProperties.getClientSecret()));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                    .setAccessType("offline")
                    .build();

            // code 交換 token
            GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
                    .setRedirectUri(configProperties.getRedirectUri())
                    .execute();

            // 將 GoogleTokenResponse 轉換成 Credential
            Credential credential = flow.createAndStoreCredential(tokenResponse, "user");
            // Credential存入session
            session.setAttribute("credential", credential);

            return "redirect:/posts";

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }

    }
}
