package com.tianji.aigc.service;

import com.tianji.aigc.entity.ChatSession;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.aigc.vo.ChatSessionVO;
import com.tianji.aigc.vo.MessageVO;
import com.tianji.aigc.vo.SessionVO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 对话session 服务类
 * </p>
 *
 * @author author
 * @since 2026-05-22
 */
public interface IChatSessionService extends IService<ChatSession> {

    SessionVO createSession(Integer num);

    List<SessionVO.Example> hotExamples(Integer num);

    List<MessageVO> queryBySessionId(String sessionId);

    void updateBytitle(String sessionId, String title, Long user);

    Map<String,List<ChatSessionVO>> queryHistorySession();

    void deleteHistorySession(String sessionId);
}
