package com.shop.observability;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/** Accepts the caller's correlation id (or creates one) and echoes it back in the response. */
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(CorrelationId.HEADER);
        CorrelationId.set(incoming != null && !incoming.isBlank() ? incoming : CorrelationId.currentOrNew());
        response.setHeader(CorrelationId.HEADER, CorrelationId.current());
        try {
            chain.doFilter(request, response);
        } finally {
            CorrelationId.clear();
        }
    }
}
