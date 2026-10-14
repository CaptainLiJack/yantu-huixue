package com.tianji.aigc.memory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.tianji.aigc.constants.Constant;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.messages.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息转换工具类，提供消息对象与JSON字符串之间的转换功能，主要用于Redis存储格式转换
 */
@Component
public class MessageUtil {

    private final StringRedisTemplate stringRedisTemplate;
    private static StringRedisTemplate staticRedisTemplate;

    public MessageUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostConstruct
    private void init() {
        staticRedisTemplate = this.stringRedisTemplate;
    }

    /**
     * 将Message对象转换为Redis存储格式的JSON字符串
     *
     * @param message 需要转换的原始消息对象
     * @return 符合Redis存储规范的JSON字符串
     */
    public static String toJson(Message message) {
        var myMessage = BeanUtil.toBean(message, MyMessage.class);

        // 设置消息内容
        myMessage.setTextContent(message.getText());
        if (message instanceof AssistantMessage assistantMessage) {
            myMessage.setToolCalls(assistantMessage.getToolCalls());
            // ★ 通过 messageId 中转，从Redis获取工具参数
            if (assistantMessage.getMetadata() != null
                    && assistantMessage.getMetadata().containsKey(Constant.ID)
                    && staticRedisTemplate != null) {
                String messageId = assistantMessage.getMetadata().get(Constant.ID).toString();
                Object requestIdObj = staticRedisTemplate.opsForHash().get(messageId, Constant.REQUEST_ID);
                if (requestIdObj != null) {
                    String reqId = requestIdObj.toString();
                    Map<Object, Object> params = staticRedisTemplate.opsForHash().entries(reqId);
                    if (ObjectUtil.isNotEmpty(params)) {
                        myMessage.setParams((Map) params);
                    }
                    staticRedisTemplate.delete(messageId);
                    staticRedisTemplate.delete(reqId);
        }}}

        if (message instanceof ToolResponseMessage toolResponseMessage) {
            myMessage.setToolResponses(toolResponseMessage.getResponses());
        }

        return JSONUtil.toJsonStr(myMessage);
    }

    /**
     * 将Redis存储的JSON字符串反序列化为对应的Message对象
     *
     * @param json Redis存储的JSON格式消息数据
     * @return 对应类型的Message对象
     * @throws RuntimeException 当无法识别的消息类型时抛出异常
     */
    public static Message toMessage(String json) {
        var myMessage = JSONUtil.toBean(json, MyMessage.class);
        var messageType = MessageType.valueOf(myMessage.getMessageType());
        switch (messageType) {
            case SYSTEM -> {
                return new SystemMessage(myMessage.getTextContent());
            }
            case USER -> {
                return UserMessage.builder()
                        .text(myMessage.getTextContent())
                        .metadata(myMessage.getMetadata())
                        .media(myMessage.getMedia())
                        .build();
            }
            case ASSISTANT -> {
                return new MyAssistantMessage(myMessage.getTextContent(), myMessage.getMetadata(), myMessage.getToolCalls(), myMessage.getMedia(), myMessage.getParams());
            }
            case TOOL -> {
                return new ToolResponseMessage(myMessage.getToolResponses(), myMessage.getMetadata());
            }
        }

        throw new RuntimeException("Message data conversion failed.");
    }

}
