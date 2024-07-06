package com.microel.trackerbackend.storage.configurations;

import com.microel.trackerbackend.security.filters.TokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
public class Security {

    private final TokenFilter tokenFilter;

    public Security(TokenFilter tokenFilter) {
        this.tokenFilter = tokenFilter;
    }

//    private final JwtTokenFilter jwtTokenFilter;
//
//    public Security(JwtTokenFilter jwtTokenFilter) {
//        this.jwtTokenFilter = jwtTokenFilter;
//    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .httpBasic().disable()
                .csrf().disable().cors(Customizer.withDefaults())
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeHttpRequests(
                        authz -> authz
                                .antMatchers(
                                        "/api/public/**",
                                        "/api/internal/**",
                                        "/api/ws/**",
                                        "/socket",
                                        "api/private/sensor"
                                ).permitAll()
                                .anyRequest().authenticated()
                                .and()
                                .addFilterAfter(tokenFilter, UsernamePasswordAuthenticationFilter.class)
                )
//                .authorizeRequests()
//                .anyRequest().permitAll().and()
//                .antMatchers("/api/public/**", "/api/ws/**").permitAll()
//                .antMatchers("/api/private/**").authenticated().and()
//                .addFilterAfter(new TokenFilter(), UsernamePasswordAuthenticationFilter.class)
//                .anyRequest().authenticated()
//                .and()
//                .addFilterBefore(jwtTokenFilter, BasicAuthenticationFilter.class)
                .build();
    }
}
