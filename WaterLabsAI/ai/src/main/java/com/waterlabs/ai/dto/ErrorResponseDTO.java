package com.waterlabs.ai.dto;

public record ErrorResponseDTO(String timestamp, String status, String error, String message, String path) {

}
