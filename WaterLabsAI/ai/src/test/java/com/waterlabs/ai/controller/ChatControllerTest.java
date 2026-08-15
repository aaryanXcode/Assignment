package com.waterlabs.ai.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.waterlabs.ai.service.ChatService;

@WebMvcTest(ChatController.class)
public class ChatControllerTest {
	
	@Autowired
	private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;
    
    @Mock
    private ChatResponse chatResponse;
    
    @Test
	void shouldReturnChatResponseWhenRequestCallsChatEndpoint() throws Exception {
    	when(chatService.chat("hello", null))
        .thenReturn(chatResponse);
    	mockMvc.perform(get("/chat").param("query","hello")).andExpect(status().isOk());
    	verify(chatService).chat("hello", null);
    	
    }
    
    @Test
    void shouldReturn400WhenQueryParameterIsMissing() throws Exception {
    	mockMvc.perform(get("/chat")).andExpect(status().isBadRequest());
    	verify(chatService, never()).chat(anyString(), any());
    }
}
