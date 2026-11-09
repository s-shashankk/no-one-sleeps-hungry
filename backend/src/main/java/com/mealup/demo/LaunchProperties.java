package com.mealup.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The real starting data for a new server (one partner hotel and the plates served before launch), read from the
 * private deploy/application-local.yml. Everything is empty when that file is absent, and then nothing is created.
 */
@ConfigurationProperties(prefix = "mealup.launch")
public record LaunchProperties(String hotelName, String hotelContact, String hotelPhone, String hotelAddress,
                               String hotelMapUrl, String areaName, String areaCity, Integer openingPlates) { }
