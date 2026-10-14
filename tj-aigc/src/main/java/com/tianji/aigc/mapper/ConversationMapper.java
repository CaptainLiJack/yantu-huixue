package com.tianji.aigc.mapper;

import com.tianji.aigc.entity.Conversation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 对话记录表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2026-05-23
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

}
