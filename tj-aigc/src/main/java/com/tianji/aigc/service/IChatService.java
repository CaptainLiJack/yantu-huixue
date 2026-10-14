package com.tianji.aigc.service;

import com.tianji.aigc.vo.ChatEventVO;
import reactor.core.publisher.Flux;

public interface IChatService {
    Flux<ChatEventVO> chat(String question, String sessionId);

    String getConversationId(String sessionId);

    void stop(String sessionId);

    String chatText(String question);
}
