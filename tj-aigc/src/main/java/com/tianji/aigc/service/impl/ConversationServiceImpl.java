package com.tianji.aigc.service.impl;

import com.tianji.aigc.entity.Conversation;
import com.tianji.aigc.mapper.ConversationMapper;
import com.tianji.aigc.service.IConversationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 对话记录表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-05-23
 */
@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements IConversationService {

}
