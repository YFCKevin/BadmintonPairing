package com.yfckevin.badmintonPairing.controller;

import com.yfckevin.badmintonPairing.ConfigProperties;
import com.yfckevin.badmintonPairing.dto.LeaderDTO;
import com.yfckevin.badmintonPairing.dto.LoginDTO;
import com.yfckevin.badmintonPairing.dto.PostDTO;
import com.yfckevin.badmintonPairing.dto.SearchDTO;
import com.yfckevin.badmintonPairing.exception.ResultStatus;
import com.yfckevin.badmintonPairing.service.LeaderService;
import com.yfckevin.badmintonPairing.service.PostService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api")
public class BackendManageController {
    private final ConfigProperties configProperties;
    private final LeaderService leaderService;
    private final PostService postService;

    public BackendManageController(ConfigProperties configProperties, LeaderService leaderService, PostService postService) {
        this.configProperties = configProperties;
        this.leaderService = leaderService;
        this.postService = postService;
    }

    /**
     * 導登入頁面
     *
     * @return
     */
    @GetMapping("/login")
    public String loginPage() {
        return "backend/login";
    }

    /**
     * 登入驗證
     *
     * @return
     */
    @PostMapping("/loginCheck")
    public ResponseEntity<?> loginCheck(@RequestBody LoginDTO dto) {
        ResultStatus resultStatus = new ResultStatus();
        //TODO
        return ResponseEntity.ok(resultStatus);
    }

    /**
     * 登出
     * @param session
     * @return
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        ResultStatus resultStatus = new ResultStatus();
        //TODO
        return ResponseEntity.ok(resultStatus);
    }

    /**
     * 導團主管理頁面
     *
     * @param session
     * @param model
     * @return
     */
    @GetMapping("/forwardLeaderManagement")
    public String forwardLeaderPage(HttpSession session, Model model) {
        //TODO
        return "backend/leaderManagement";
    }

    /**
     * 新增/修改團主
     *
     * @param session
     * @return
     */
    @PostMapping("/saveLeader")
    public ResponseEntity<?> saveLeader(@RequestBody LeaderDTO dto, HttpSession session) {
        ResultStatus resultStatus = new ResultStatus();
        //TODO
        return ResponseEntity.ok(resultStatus);
    }

    /**
     * 刪除團主
     *
     * @param session
     * @return
     */
    @GetMapping("/deleteLeader/{id}")
    public ResponseEntity<?> deleteLeader(@PathVariable String id, HttpSession session) {
        ResultStatus resultStatus = new ResultStatus();
        //TODO
        return ResponseEntity.ok(resultStatus);
    }

    /**
     * 查詢團主
     *
     * @param dto
     * @param session
     * @return
     */
    @PostMapping("/searchLeaders")
    public ResponseEntity<?> searchLeaders(@RequestBody SearchDTO dto, HttpSession session) {
        ResultStatus resultStatus = new ResultStatus();
        //TODO
        return ResponseEntity.ok(resultStatus);
    }

    /**
     * 導貼文管理頁面
     *
     * @param session
     * @param model
     * @return
     */
    @GetMapping("/forwardPostManagement")
    public String forwardPostManagement(HttpSession session, Model model) {
        //TODO
        return "backend/postManagement";
    }

    /**
     * 新增/修改貼文
     *
     * @param session
     * @return
     */
    @PostMapping("/savePost")
    public ResponseEntity<?> savePost(@RequestBody PostDTO dto, HttpSession session) {
        ResultStatus resultStatus = new ResultStatus();
        //TODO
        return ResponseEntity.ok(resultStatus);
    }

    /**
     * 刪除貼文
     *
     * @param session
     * @return
     */
    @GetMapping("/deletePost/{id}")
    public ResponseEntity<?> deletePost(@PathVariable String id, HttpSession session) {
        ResultStatus resultStatus = new ResultStatus();
        //TODO
        return ResponseEntity.ok(resultStatus);
    }

    /**
     * 查詢貼文
     *
     * @param dto
     * @param session
     * @return
     */
    @PostMapping("/searchPosts")
    public ResponseEntity<?> searchPosts(@RequestBody SearchDTO dto, HttpSession session) {
        ResultStatus resultStatus = new ResultStatus();
        //TODO
        return ResponseEntity.ok(resultStatus);
    }

    /**
     * 導檔案管理頁面
     *
     * @param session
     * @param model
     * @return
     */
    @GetMapping("/forwardFileManagement")
    public String forwardFileManagement(HttpSession session, Model model) {
        //TODO
        return "backend/fileManagement";
    }

    /**
     * 查詢檔案
     *
     * @param dto
     * @param session
     * @return
     */
    @PostMapping("/searchFiles")
    public ResponseEntity<?> searchFiles(@RequestBody SearchDTO dto, HttpSession session) {
        ResultStatus resultStatus = new ResultStatus();
        //TODO
        return ResponseEntity.ok(resultStatus);
    }
}
