package com.waterlabs.ai.dto;

//public record PropertyDTO(
//        String title,
//        String location,
//        String price,
//        int floor,
//        int age,
//        String area,
//        String url) {
//}


public record PropertyDTO(
    String propertyId,
    String title,
    String society,
    int rentInINR,
    int floor,
    int ageInYears, // -1 if not available on card
    String area,
    String url
) {}