package com.waterlabs.ai.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.AdvisorSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;

import com.waterlabs.ai.exceptions.AiCallFailedException;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {
	
	@Mock
	private ChatClient chatClient;
	
	@Mock
	private ChatMemory chatMemory;
	
	@Mock
	ChatClient.ChatClientRequestSpec requestSpec;
	
	@Mock
	private ChatClient.CallResponseSpec callResponseSpec;
	
	@InjectMocks
	private ChatService chatService;
	
	@Mock
	private ChatResponse chatResponse;
	
	@Test
	void shouldReturnChatResponseWhenAiCallSucceeds() {
		when(chatClient.prompt()).thenReturn(requestSpec);
		when(requestSpec.user(anyString())).thenReturn(requestSpec);
		when(requestSpec.advisors(any(Advisor.class))).thenReturn(requestSpec);
		when(requestSpec.advisors((Consumer<AdvisorSpec>) any(Consumer.class))).thenReturn(requestSpec);
		when(requestSpec.call()).thenReturn(callResponseSpec);
		when(callResponseSpec.chatResponse()).thenReturn(chatResponse);
		
		ChatResponse result = chatService.chat("Hello", null);
		assertEquals(chatResponse, result);
		
		verify(chatClient).prompt();
		verify(requestSpec).user(anyString());
		verify(requestSpec).advisors(any(Advisor.class));
		verify(requestSpec).advisors(any(Consumer.class));
		verify(requestSpec).call();
		verify(callResponseSpec).chatResponse();
	}
	
	@Test
	void shouldThrowAiCallFailedExceptionWhenAiCallFails() {

	    when(chatClient.prompt())
	            .thenThrow(new RuntimeException("OpenAI down"));

	    AiCallFailedException ex = assertThrows(
	            AiCallFailedException.class,
	            () -> chatService.chat("Hello", null)
	    );

	    assertEquals(
	            "AI service is unavailable. Please try again later.",
	            ex.getMessage()
	    );

	    assertTrue(ex.getCause() instanceof RuntimeException);
	}
	
}
