package com.tianji.aigc.controller;


import com.tianji.aigc.service.IChatSessionService;
import com.tianji.aigc.vo.ChatSessionVO;
import com.tianji.aigc.vo.MessageVO;
import com.tianji.aigc.vo.SessionVO;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 对话session 前端控制器
 * </p>
 *
 * @author author
 * @since 2026-05-22
 */
@RestController
@RequiredArgsConstructor
public class ChatSessionController {
    private final IChatSessionService chatSessionService;
    @PostMapping("/session")
    public SessionVO createSession(@RequestParam(value = "n", defaultValue = "3") Integer num) {
        return chatSessionService.createSession(num);
    }

    @GetMapping("/session/hot")
    public List<SessionVO.Example> hotExamples(@RequestParam(value = "n", defaultValue = "3") Integer num) {
        return this.chatSessionService.hotExamples(num);
    }
    /**
     * 查询单个历史对话详情
     *
     * @return 对话记录列表
     */
    @GetMapping("/{sessionId}")
    public List<MessageVO> queryBySessionId(@PathVariable("sessionId") String sessionId) {
        return this.chatSessionService.queryBySessionId(sessionId);
    }

    @PutMapping("/session/history")
    public void updateTitle(@RequestParam("sessionId") String sessionId,@RequestParam("title") String title){
        chatSessionService.updateBytitle(sessionId,title,UserContext.getUser());
    }
    /**
     * 查询历史会话列表
     */
    @GetMapping("/history")
    public Map<String, List<ChatSessionVO>> queryHistorySession() {
        return this.chatSessionService.queryHistorySession();
    }
    @DeleteMapping("/history")
    public void deleteHistorySession(@RequestParam("sessionId") String sessionId) {
        this.chatSessionService.deleteHistorySession(sessionId);
    }

}
