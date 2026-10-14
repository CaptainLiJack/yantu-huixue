package com.tianji.aigc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.mapper.ChatSessionMapper;
import com.tianji.aigc.service.IChatService;
import com.tianji.aigc.service.IChatSessionService;
import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tj.ai", name = "chat-type", havingValue = "EXCHANGE")
public class ChatServiceImpl implements IChatService {

    private final ChatClient chatClient;

    private final SystemPromptConfig systemPromptConfig;

//    private final Map<String,Boolean> GENSTATU = new ConcurrentHashMap<>();
    private static final ChatEventVO STOP_EVENT = ChatEventVO.builder().eventType(ChatEventTypeEnum.STOP.getValue()).build();

    private final ChatMemory  chatMemory;

    private static final String GENSTATU = "GENSTATU";

    private final StringRedisTemplate stringRedisTemplate;

    private final VectorStore vectorStore;

    private final IChatSessionService chatSessionService ;

    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        StringBuilder outputBuilder = new StringBuilder();
        String conversationId = UserContext.getUser() + sessionId;
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(GENSTATU);
        // 生成请求id
        var userId = UserContext.getUser();
        var requestId = IdUtil.fastSimpleUUID();
        var qaAdvisor = QuestionAnswerAdvisor.builder(this.vectorStore)
                .searchRequest(SearchRequest.builder().similarityThreshold(0.6d).topK(6).build())
                .build();
        chatSessionService.updateBytitle(sessionId,question,userId);
        return chatClient.prompt()
                .system(s -> s
                        .text(systemPromptConfig.getChatSystemMessage())  // 去掉 .get()
                        .param("now", DateUtil.now())
                )
                .advisors(advisor -> advisor.advisors(qaAdvisor).param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(question)
                .toolContext(Map.of(Constant.REQUEST_ID, requestId, Constant.USER_ID, userId)) //通过工具上下文传递参数
                .stream()
                .chatResponse()
                .doFirst(()-> hashOps.put(sessionId,"true"))
                .doOnError(throwable -> {hashOps.delete(sessionId);})
                .doOnComplete(() -> {hashOps.delete(sessionId);})
                .doOnCancel(()->saveStopHistoryRecord(conversationId, outputBuilder.toString()))
                .filter(chatResponse -> chatResponse.getResult() != null)
                .takeWhile(chatResponse -> {
                   return hashOps.get(sessionId) != null;
                })
                .map(chatResponse -> {
                    String text = chatResponse.getResult().getOutput().getText();
                    outputBuilder.append(text);
                    var finishReason = chatResponse.getResult().getMetadata().getFinishReason();
                    if (StrUtil.equals(Constant.STOP, finishReason)) {
                        var messageId = chatResponse.getMetadata().getId();
                        stringRedisTemplate.opsForHash().put(messageId, Constant.REQUEST_ID, requestId);
                    }
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                .filter(vo -> vo.getEventData() != null)
                .concatWith(Flux.defer(() -> {
                    // 通过请求id获取到参数列表，如果不为空，就将其追加到返回结果中
                    Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(requestId);
                    if (CollUtil.isNotEmpty(map)) {
                        // 注意：不在这里删除requestId，由MessageUtil.toJson()读取后再清理
                        // 响应给前端的参数数据
                        var chatEventVO = ChatEventVO.builder()
                                .eventData(map)
                                .eventType(ChatEventTypeEnum.PARAM.getValue())
                                .build();
                        return Flux.just(chatEventVO, STOP_EVENT);
                    }
                    return Flux.just(STOP_EVENT);
                }));
    }

    @Override
    public String getConversationId(String sessionId) {
        String conversationId = UserContext.getUser() + sessionId;
        return conversationId;
    }

    private void saveStopHistoryRecord(String conversationId, String content) {
        chatMemory.add(conversationId, new AssistantMessage(content));
    }

    @Override
    public void stop(String sessionId) {
        stringRedisTemplate.delete(sessionId);
    }

    @Override
    public String chatText(String question) {
        return "";
    }
}
