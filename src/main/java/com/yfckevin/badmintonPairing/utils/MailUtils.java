package com.yfckevin.badmintonPairing.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class MailUtils {


    private static JavaMailSender sender;

    @Autowired
    public void setSender(JavaMailSender sender) {
        MailUtils.sender = sender;
    }

    /**
     * 發送email
     * @param receiverMail  接收者信箱
     * @param subject    主題
     * @param content    內容
     */
    public static void sendMail(String receiverMail, String subject, String content) {

        String[] receiver = {receiverMail};

        SimpleMailMessage message = new SimpleMailMessage();
        // 接收者信箱
        message.setTo(receiver);
        // 主旨
        message.setSubject(subject);
        // 內容
        message.setText(content);

        sender.send(message);
    }

}
