package com.pameobeo.kitchen.interfaces.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Demo-only fault injection for the Circuit Breaker demo: makes {@code POST /api/kitchen/quotes}
 * slow and/or failing. Admin only.
 */
@RestController
@RequestMapping("/api/kitchen/admin/chaos")
class ChaosController {

    private static final Logger log = LoggerFactory.getLogger(ChaosController.class);

    static final AtomicLong DELAY_MS = new AtomicLong();
    static final AtomicInteger FAILURE_PERCENT = new AtomicInteger();

    record ChaosSettings(@Min(0) @Max(60_000) long delayMs, @Min(0) @Max(100) int failurePercent) {}

    @GetMapping
    ChaosSettings get() {
        return new ChaosSettings(DELAY_MS.get(), FAILURE_PERCENT.get());
    }

    @PutMapping
    ChaosSettings set(@Valid @RequestBody ChaosSettings settings) {
        DELAY_MS.set(settings.delayMs());
        FAILURE_PERCENT.set(settings.failurePercent());
        log.warn("CHAOS updated: delay={}ms failure={}%", settings.delayMs(), settings.failurePercent());
        return settings;
    }

    @Component
    static class ChaosFilter extends OncePerRequestFilter {

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            return !("POST".equals(request.getMethod()) && request.getRequestURI().equals("/api/kitchen/quotes"));
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            long delay = DELAY_MS.get();
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (ThreadLocalRandom.current().nextInt(100) < FAILURE_PERCENT.get()) {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "chaos: injected failure");
                return;
            }
            chain.doFilter(request, response);
        }
    }
}
