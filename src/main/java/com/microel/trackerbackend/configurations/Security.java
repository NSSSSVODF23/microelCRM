package com.microel.trackerbackend.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class Security {

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
                .and().authorizeRequests()
                .anyRequest().permitAll().and()
//                .antMatchers(
//                        "/api/public/**", "/api/ws/**"
//                ).permitAll()
//                .antMatchers(
//                        "/api/private/operator",
//                        "/api/private/operators",
//                        "/api/private/config"
//                ).hasRole(OperatorGroup.ADMIN.toString())
//                .anyRequest().authenticated()
//                .and()
//                .addFilterBefore(jwtTokenFilter, BasicAuthenticationFilter.class)
                .build();
    }
}
