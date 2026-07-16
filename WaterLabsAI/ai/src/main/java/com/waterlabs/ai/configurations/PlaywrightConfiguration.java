package com.waterlabs.ai.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.microsoft.playwright.Playwright;

@Configuration
public class PlaywrightConfiguration {
	
	@Bean(destroyMethod = "close")
	Playwright playWright() {
		Playwright playwright = Playwright.create();
		return playwright;
	}
}
