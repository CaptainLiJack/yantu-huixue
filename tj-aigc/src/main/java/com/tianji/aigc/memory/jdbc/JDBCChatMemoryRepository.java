package com.tianji.aigc.memory.jdbc;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.stream.StreamUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianji.aigc.entity.Conversation;
import com.tianji.aigc.memory.MessageUtil;
import com.tianji.aigc.service.IChatService;
import com.tianji.aigc.service.IConversationService;
import com.tianji.common.utils.SPELUtils;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Queue;

import static org.apache.catalina.security.SecurityUtil.remove;

/**
 * 基于MYSQL实现的ChatMemoryRepository
 */

public class JDBCChatMemoryRepository implements ChatMemoryRepository {

    @Autowired
    private IConversationService conversationService;

    @Override
    public List<String> findConversationIds() {
        List<Conversation> list = conversationService.lambdaQuery().select(Conversation::getConversationId).list();
        return CollStreamUtil.toList(list, Conversation::getConversationId);
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<Conversation> list = conversationService.lambdaQuery().eq(Conversation::getConversationId, conversationId).list();
        List<Message> list1 = CollStreamUtil.toList(list, conversation -> MessageUtil.toMessage(conversation.getData()));
        return list1;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        deleteByConversationId(conversationId);
        Long userId = UserContext.getUser();
        long uid = userId == null ? 0L : userId;

        List<Conversation> list = CollStreamUtil.toList(messages, message ->
                new Conversation()
                        .setConversationId(conversationId)
                        .setData(MessageUtil.toJson(message))
                        .setCreator(uid)
                        .setUpdater(uid)
        );
        conversationService.saveBatch(list);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getConversationId, conversationId);
        conversationService.remove(wrapper);
    }
}
