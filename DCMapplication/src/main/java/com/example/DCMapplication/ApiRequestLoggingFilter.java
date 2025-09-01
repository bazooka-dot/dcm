package com.example.DCMapplication;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.*;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class ApiRequestLoggingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(ApiRequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        logger.info("API Request: method={}, uri={}, remoteAddr={}", req.getMethod(), req.getRequestURI(), req.getRemoteAddr());
        chain.doFilter(request, response);
    }
}