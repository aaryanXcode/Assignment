package com.waterlabs.ai.util;

import org.springframework.stereotype.Component;

@Component
public class PropertyPriceParser {
	
	public PropertyPriceParser() {
		
	}
	
	public int parsePrice(String priceText) {
        if (priceText == null) return -1;
        String digits = priceText.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? -1 : Integer.parseInt(digits);
    }

}
