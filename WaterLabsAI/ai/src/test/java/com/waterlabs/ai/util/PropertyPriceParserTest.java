package com.waterlabs.ai.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.waterlabs.ai.util.PropertyPriceParser;

public class PropertyPriceParserTest {
	
	private PropertyPriceParser propertyPriceParser;
	
	@BeforeEach
	void init() {
		propertyPriceParser = new PropertyPriceParser();
	}
	
	@Test
	void shouldParseCommaSeparatedPrice() {
		int result = propertyPriceParser.parsePrice("50,000");
		assertEquals(50000, result);
	}
	
	@Test
	void shouldCheckNullValues() {
		int result = propertyPriceParser.parsePrice(null);
		assertEquals(-1, result);
	}
	
	@Test
	void shouldParseHindiPriceValues() {
		int result = propertyPriceParser.parsePrice("₹1,25,000");
		assertEquals(125000, result);
	}
	
	@Test
	void shouldCheckForZeroValue() {
		int result = propertyPriceParser.parsePrice("0");
		assertEquals(0, result);
	}
	
	@ParameterizedTest
	@CsvSource({
	    "'50,000',50000",
	    "'₹1,25,000',125000",
	    "'0',0"
	})
	void shouldParsePrice(String input, int expected) {

	    int result = propertyPriceParser.parsePrice(input);
	    assertEquals(expected, result);
	}

}
