package com.pameobeo.kitchen.infrastructure.config;

import java.time.LocalTime;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kitchen")
public record KitchenProperties(int defaultCapacity, LocalTime cutoffTime, String zone) {}
