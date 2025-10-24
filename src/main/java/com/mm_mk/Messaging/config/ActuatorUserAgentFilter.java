package com.mm_mk.Messaging.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ActuatorUserAgentFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI();
        if (path.startsWith("/actuator")) {
            String ua = req.getHeader("User-Agent");
            if (ua != null && ua.matches(".*(Mozilla|Chrome|Safari|Edge).*")) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.getWriter().write("Browser access is not allowed.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}

