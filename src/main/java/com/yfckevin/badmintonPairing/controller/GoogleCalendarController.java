package com.yfckevin.badmintonPairing.controller;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.yfckevin.badmintonPairing.dto.CalendarDTO;
import com.yfckevin.badmintonPairing.exception.ResultStatus;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.yfckevin.badmintonPairing.controller.AuthController.JSON_FACTORY;

@Controller
public class GoogleCalendarController {
    Logger logger = LoggerFactory.getLogger(GoogleCalendarController.class);
    private final SimpleDateFormat sdf;
    private final SimpleDateFormat isoSdf;
    private static final String APPLICATION_NAME = "Google Calendar API Java Quickstart";

    public GoogleCalendarController(@Qualifier("sdf") SimpleDateFormat sdf, @Qualifier("isoSdf") SimpleDateFormat isoSdf) {
        this.sdf = sdf;
        this.isoSdf = isoSdf;
    }

    @PostMapping("/importCalendar")
    public ResponseEntity<?> importCalendar(@RequestBody CalendarDTO dto, HttpSession session) {
        logger.info("[importCalendar]");

        ResultStatus resultStatus = new ResultStatus();

        try {
            Credential credential = (Credential) session.getAttribute("credential");
            System.out.println(credential);
            if (credential == null) {   // 重新做google授權認證
                resultStatus.setCode("C007");
                resultStatus.setMessage("credential已失效");
            } else {
                final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                        .setApplicationName(APPLICATION_NAME)
                        .build();

                // 建立 Google Calendar 事件
                Event event = new Event()
                        .setSummary(dto.getSummary())
                        .setLocation(dto.getLocation())
                        .setDescription(dto.getDescription());

                // 轉換成ISO 8601格式
                String startDateTimeStr = isoSdf.format(sdf.parse(dto.getStart()));
                String endDateTimeStr = isoSdf.format(sdf.parse(dto.getEnd()));

                DateTime startDateTime = new DateTime(startDateTimeStr);
                EventDateTime start = new EventDateTime().setDateTime(startDateTime).setTimeZone("Asia/Taipei");
                event.setStart(start);

                DateTime endDateTime = new DateTime(endDateTimeStr);
                EventDateTime end = new EventDateTime().setDateTime(endDateTime).setTimeZone("Asia/Taipei");
                event.setEnd(end);

                String calendarId = "primary";
                event = service.events().insert(calendarId, event).execute();
                logger.info("行事曆連結: " + event.getHtmlLink());

                resultStatus.setCode("C000");
                resultStatus.setMessage("成功");
            }
            return ResponseEntity.ok(resultStatus);


        } catch (Exception e) {
            e.printStackTrace();
            resultStatus.setCode("C999");
            resultStatus.setMessage("例外發生");
        }
        return ResponseEntity.ok(resultStatus);
    }
}
