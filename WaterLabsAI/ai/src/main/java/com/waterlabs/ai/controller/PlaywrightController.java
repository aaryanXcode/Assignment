package com.waterlabs.ai.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.waterlabs.ai.dto.PropertyDTO;
import com.waterlabs.ai.dto.ScrapeFiltersDTO;
import com.waterlabs.ai.service.PlayWrightService;

@Controller
public class PlaywrightController {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightController.class);

    private final PlayWrightService playWrightService;

    public PlaywrightController(PlayWrightService playWrightService) {
        this.playWrightService = playWrightService;
    }

    @GetMapping("/scrape")
    public ResponseEntity<List<PropertyDTO>> scrape(
            @RequestParam(value = "city",         defaultValue = "Bangalore") String city,
            @RequestParam(value = "propertyType", defaultValue = "Apartment")  String propertyType,
            @RequestParam(value = "bhk",          defaultValue = "3")          List<String> bhk,
            @RequestParam(value = "minBudget",    defaultValue = "50000")      int minBudget,
            @RequestParam(value = "maxBudget",    defaultValue = "65000")      int maxBudget,
            @RequestParam(value = "minFloor",     defaultValue = "10")         int minFloor,
            @RequestParam(value = "maxAge",       defaultValue = "5")          int maxAge
    ) {
        ScrapeFiltersDTO filters = new ScrapeFiltersDTO(
                city, propertyType, bhk, minBudget, maxBudget, minFloor, maxAge);

        log.info("Scrape request — city={} type={} bhk={} budget={}-{} minFloor={} maxAge={}",
                city, propertyType, bhk, minBudget, maxBudget, minFloor, maxAge);

        List<PropertyDTO> results = playWrightService.getPropertyDetails(filters);

        log.info("Scrape complete — returned {} properties", results.size());
        return ResponseEntity.ok(results);
    }
}
