package com.waterlabs.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import com.waterlabs.ai.exceptions.AiCallFailedException;

import reactor.core.publisher.Flux;

@Service
public class ChatService {

	private static final Logger log = LoggerFactory.getLogger(ChatService.class);
	private static final String CONVERSATION_ID = "default-session";

	private final ChatClient chatClient;
	private final MessageChatMemoryAdvisor memoryAdvisor;

	public ChatService(ChatClient chatClient, ChatMemory chatMemory) {
		this.chatClient = chatClient;
		this.memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
	}

	public ChatResponse chat(String query, String additionalPrompt) {
		String userMessage = (additionalPrompt != null && !additionalPrompt.isBlank())
				? query + "\n\nAdditional instructions: " + additionalPrompt
				: query;

		log.debug("Sending to AI — conversationId={} messageLength={}", CONVERSATION_ID, userMessage.length());

		ChatResponse response;
		try {
			response = chatClient.prompt()
					.user(userMessage)
					.advisors(memoryAdvisor)
					.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, CONVERSATION_ID))
					.call()
					.chatResponse();
		} catch (Exception ex) {
			log.error("AI call failed for conversationId={}: {}", CONVERSATION_ID, ex.getMessage(), ex);
			throw new AiCallFailedException("AI service is unavailable. Please try again later.", ex);
		}

		log.debug("AI response received — tokens={}", response != null && response.getMetadata() != null
				? response.getMetadata().getUsage() : "N/A");

		return response;
	}

	public Flux<String> streamingChat(String query, String additionalPrompt) {
		StringBuilder prompt = new StringBuilder(query);

		if (additionalPrompt != null && !additionalPrompt.isBlank()) {
			prompt.append("\n\nAdditional instructions:\n").append(additionalPrompt);
		}

		log.debug("Streaming to AI — conversationId={} messageLength={}", CONVERSATION_ID, prompt.length());

		try {
			return chatClient.prompt()
					.user(prompt.toString())
					.advisors(memoryAdvisor)
					.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, CONVERSATION_ID))
					.stream()
					.content()
					.doOnError(ex -> log.error("Streaming AI call failed: {}", ex.getMessage(), ex))
					.onErrorMap(ex -> new AiCallFailedException(
							"AI streaming service is unavailable. Please try again later.", ex));
		} catch (Exception ex) {
			log.error("Failed to initiate streaming AI call: {}", ex.getMessage(), ex);
			throw new AiCallFailedException("AI streaming service is unavailable. Please try again later.", ex);
		}
	}
}
