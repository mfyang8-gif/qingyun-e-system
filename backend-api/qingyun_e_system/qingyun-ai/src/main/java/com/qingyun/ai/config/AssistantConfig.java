package com.qingyun.ai.config;


import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AssistantConfig {


    private final ToolProvider allToolProvider;


    private final OpenAiChatModel doubaoModel;


    private final QwenChatModel qwenChatModel;


    private final OpenAiStreamingChatModel streamingModel;

    private final ChatMemoryProvider chatMemoryProvider;

    public AssistantConfig(OpenAiChatModel doubaoModel, QwenChatModel qwenChatModel, OpenAiStreamingChatModel streamingModel, ChatMemoryProvider chatMemoryProvider,   @Qualifier("mcpToolProvider")ToolProvider allToolProvider) {
        this.doubaoModel = doubaoModel;
        this.qwenChatModel = qwenChatModel;
        this.streamingModel = streamingModel;
        this.chatMemoryProvider = chatMemoryProvider;
        this.allToolProvider = allToolProvider;
    }



}
