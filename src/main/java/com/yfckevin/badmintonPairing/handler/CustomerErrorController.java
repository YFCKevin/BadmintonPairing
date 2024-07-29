package com.yfckevin.badmintonPairing.handler;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;


public class CustomerErrorController implements ErrorController {
    private static final String ERROR_PATH = "/error";

    @GetMapping(ERROR_PATH)
    public void error(HttpServletResponse response, HttpSession session, Model model) throws IOException {

        int status = response.getStatus();
        System.out.println("status="+status);
        switch (status) {
            case 404:
                response.sendRedirect("/badminton/error/404");
                break;
            default:
                response.sendRedirect("/badminton/error/50x");
        }

    }

    public String getErrorPath() { return ERROR_PATH; }
}
