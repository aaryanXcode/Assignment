package com.waterlabs.ai.exceptions;

public class ScrapperFailedException extends RuntimeException {

	private static final long serialVersionUID = 4365150784533770965L;

	public ScrapperFailedException(String message) {
		super(message);
	}

}
