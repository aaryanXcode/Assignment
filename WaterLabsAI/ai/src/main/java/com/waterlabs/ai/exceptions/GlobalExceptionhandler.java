package com.waterlabs.ai.exceptions;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.waterlabs.ai.dto.ErrorResponseDTO;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionhandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionhandler.class);

	// ── Scraper failure ───────────────────────────────────────────────────────
	@ExceptionHandler(ScrapperFailedException.class)
	public ResponseEntity<ErrorResponseDTO> handleScrapperFailedException(
	        ScrapperFailedException ex,
	        HttpServletRequest request) {

		log.error("ScrapperFailedException at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
	}

	// ── AI / OpenRouter call failure ──────────────────────────────────────────
	@ExceptionHandler(AiCallFailedException.class)
	public ResponseEntity<ErrorResponseDTO> handleAiCallFailedException(
	        AiCallFailedException ex,
	        HttpServletRequest request) {

		log.error("AiCallFailedException at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
		return buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(), request);
	}

	// ── Missing required request parameter (e.g. ?query= on /chat) ───────────
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponseDTO> handleMissingParam(
	        MissingServletRequestParameterException ex,
	        HttpServletRequest request) {

		log.warn("Missing parameter at {}: {}", request.getRequestURI(), ex.getMessage());

		return buildResponse(HttpStatus.BAD_REQUEST,
		        "Required parameter '" + ex.getParameterName() + "' is missing", request);
	}

	// ── Catch-all for unexpected exceptions ───────────────────────────────────
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponseDTO> handleGenericException(
	        Exception ex,
	        HttpServletRequest request) {

		log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
		        "An unexpected error occurred. Please try again.", request);
	}

	// ── Helper ────────────────────────────────────────────────────────────────
	private ResponseEntity<ErrorResponseDTO> buildResponse(
	        HttpStatus status, String message, HttpServletRequest request) {

		ErrorResponseDTO body = new ErrorResponseDTO(
		        LocalDateTime.now().toString(),
		        String.valueOf(status.value()),
		        status.getReasonPhrase(),
		        message,
		        request.getRequestURI());

		return ResponseEntity.status(status).body(body);
	}
}
