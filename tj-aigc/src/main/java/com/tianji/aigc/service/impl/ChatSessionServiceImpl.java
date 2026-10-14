package com.tianji.aigc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianji.aigc.config.SessionProperties;
import com.tianji.aigc.entity.ChatSession;
import com.tianji.aigc.enums.MessageTypeEnum;
import com.tianji.aigc.mapper.ChatSessionMapper;
import com.tianji.aigc.memory.MyAssistantMessage;
import com.tianji.aigc.service.IChatService;
import com.tianji.aigc.service.IChatSessionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.aigc.vo.ChatSessionVO;
import com.tianji.aigc.vo.MessageVO;
import com.tianji.aigc.vo.SessionVO;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 对话session 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-05-22
 */
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements IChatSessionService {

    private final SessionProperties sessionProperties;
    private final ChatSessionMapper chatSessionMapper;
    private final IChatService ChatService;
    private final ChatMemory  chatMemory;

    @Override
    public SessionVO createSession(Integer num) {
        SessionVO sessionVO = BeanUtil.toBean(sessionProperties, SessionVO.class);
        // 随机获取examples
            sessionVO.setExamples(RandomUtil.randomEleList(sessionProperties.getExamples(), num));
            // 随机生成sessionId
            sessionVO.setSessionId(IdUtil.fastSimpleUUID());
            ChatSession chatSession = new ChatSession();
            // 构建持久化对象，并持久化
            chatSession.setSessionId(sessionVO.getSessionId());
            chatSession.setUserId(UserContext.getUser());
            save(chatSession);
            return sessionVO;
        }

    @Override
    public List<SessionVO.Example> hotExamples(Integer num) {
        return RandomUtil.randomEleList(sessionProperties.getExamples(), num);
    }

    @Override
    public List<MessageVO> queryBySessionId(String sessionId) {
        // 根据会话ID获取对话ID
        String conversationId =UserContext.getUser() + sessionId;
        // 从Redis中获取历史消息
        List<Message> messageList = chatMemory.get(conversationId);
        // 过滤并转换消息列表
        return StreamUtil.of(messageList)
                // 过滤掉非用户消息和助手消息
                .filter(message -> message.getMessageType() == MessageType.ASSISTANT || message.getMessageType() == MessageType.USER)
                // 转换为MessageVO对象
                .map(message -> {
                    if (message instanceof MyAssistantMessage) {
                        return MessageVO.builder()
                                .content(message.getText())
                                .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                                .params(((MyAssistantMessage) message).getParams())
                                .build();
                    }
                    return MessageVO.builder()
                            .content(message.getText())
                            .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                            .build();
                })
                .toList();
    }

    @Override
    public void updateBytitle(String sessionId, String title, Long userId) {
        List<ChatSession> list = lambdaQuery()
                .eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .list();
        // 如果列表为空，直接返回，无需进一步处理
        if (CollUtil.isEmpty(list)) {
            return;
        }

        // 获取列表中的第一个聊天会话实例
        ChatSession chatSession = list.get(0);
        // 如果聊天会话的标题为空，并且新标题不为空，则更新标题
        if (StrUtil.isEmpty(chatSession.getTitle()) && !StrUtil.isEmpty(title)) {
            chatSession.setTitle(StrUtil.sub(title, 0, 100));
        }
        // 设置更新字段为updateTime为当前时间
        chatSession.setUpdateTime(LocalDateTimeUtil.now());
        // 更新数据库中的聊天会话信息
        updateById(chatSession);
    }

    @Override
    public Map<String, List<ChatSessionVO>> queryHistorySession() {
        Long userId = UserContext.getUser();
        List<ChatSession> list = lambdaQuery().eq(ChatSession::getUserId, userId)
                .isNotNull(ChatSession::getTitle).orderByDesc(ChatSession::getUpdateTime).last("LIMIT 30").list();
        if (CollUtil.isEmpty(list)) {
            return Map.of();
        }


        // 转换为 ChatSessionVO 列表
        List<ChatSessionVO> chatSessionVOS = CollStreamUtil.toList(list, chatSession ->
                ChatSessionVO.builder()
                        .sessionId(chatSession.getSessionId())
                        .title(chatSession.getTitle())
                        .updateTime(chatSession.getUpdateTime())
                        .build()
        );
        final var TODAY = "当天";
        final var LAST_7_DAYS = "最近7天";
        final var LAST_30_DAYS = "最近30天";
        final var LAST_YEAR = "最近1年";
        final var MORE_THAN_YEAR = "1年以上";
        LocalDate localDateNow = LocalDateTime.now().toLocalDate();
        return  CollStreamUtil.groupByKey(chatSessionVOS,vo ->{
            long between = Math.abs(ChronoUnit.DAYS.between(vo.getUpdateTime().toLocalDate(), localDateNow));
            if (between == 0) {
                return TODAY;
            }
            else if (between <= 7) {
                return LAST_7_DAYS;
            }
            else if (between <= 30) {
                return LAST_30_DAYS;
            } else if (between <= 365) {
                return LAST_YEAR;
            } else {
                return MORE_THAN_YEAR;
            }
        } );
    }

    @Override
    public void deleteHistorySession(String sessionId) {
        //删除数据库的数据
        super.remove(Wrappers.<ChatSession>lambdaQuery()
                .eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, UserContext.getUser()));
        //删除redis中的数据
        var conversationId = UserContext.getUser() + sessionId;
        this.chatMemory.clear(conversationId);
    }
}
