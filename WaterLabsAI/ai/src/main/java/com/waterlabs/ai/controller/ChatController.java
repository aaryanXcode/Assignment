package com.waterlabs.ai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.waterlabs.ai.service.ChatService;

import reactor.core.publisher.Flux;

@Controller
public class ChatController {

	private static final Logger log = LoggerFactory.getLogger(ChatController.class);

	private final ChatService chatService;

	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	@GetMapping("/chat")
	ResponseEntity<ChatResponse> getChatResponse(
			@RequestParam(value = "query", required = true) String query,
			@RequestParam(value = "additionalPrompt", required = false) String additionalPrompt) {

		log.info("Chat request — query='{}' additionalPrompt='{}'", query, additionalPrompt);
		ChatResponse response = chatService.chat(query, additionalPrompt);
		log.info("Chat response received");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@GetMapping(value = "/streaming/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	Flux<String> getStreamingChatResponse(
			@RequestParam(value = "query", required = true) String query,
			@RequestParam(value = "additionalPrompt", required = false) String additionalPrompt){
		return chatService.streamingChat(query, additionalPrompt);
	}
}

