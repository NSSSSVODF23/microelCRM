package com.microel.trackerbackend.security.filters;

import com.microel.trackerbackend.security.AuthorizationProvider;
import com.microel.trackerbackend.security.JwtAuthentication;
import lombok.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class TokenFilter extends GenericFilterBean {

    private final AuthorizationProvider authorizationProvider;

    public TokenFilter(AuthorizationProvider authorizationProvider) {
        this.authorizationProvider = authorizationProvider;
    }

    public String getJwtFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("Token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String jwt = getJwtFromRequest((HttpServletRequest) servletRequest);
        if(jwt != null){
            Boolean isTokenValid = authorizationProvider.tokenValidate(jwt, true);
            if  (isTokenValid)  {
                String loginFromToken = authorizationProvider.getLoginFromToken(jwt);
                SecurityContextHolder.getContext().setAuthentication(JwtAuthentication.of(loginFromToken, loginFromToken, true));
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
