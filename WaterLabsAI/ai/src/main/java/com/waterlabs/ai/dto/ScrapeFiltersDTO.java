package com.waterlabs.ai.dto;

import java.util.List;

public record ScrapeFiltersDTO(
        String city,
        String propertyType,
        List<String> bhk,
        int minBudget,
        int maxBudget,
        int minFloor,
        int maxAge
) {}
