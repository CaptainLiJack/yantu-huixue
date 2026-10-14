package com.tianji.aigc.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Getter
@RefreshScope  // Nacos 配置变更时自动刷新
@Configuration
public class SystemPromptConfig {

      @Value("${tj.ai.system}")
      private String chatSystemMessage;
  }