package com.microel.trackerbackend.security;

import com.microel.trackerbackend.security.exceptions.JwsTokenParseError;
import com.microel.trackerbackend.storage.dispatchers.EmployeeDispatcher;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class AuthorizationProvider {
    public final static long JWS_EXPIRATION = 10_577_000_000L;
//    public final static long JWS_EXPIRATION = 3_600_000L;
//    public final static long JWS_EXPIRATION = 30_000L;
    public final static long JWS_REFRESH_EXPIRATION = 2_592_000_000L;
    private static final SecretKey secretKey = Keys.hmacShaKeyFor("kw><r%GIi72f_qY!D=.._Vl`.tz,k&2UQb\"M%Y.X\"cPYE4umIzXAVQUs{(2[<W{".getBytes(StandardCharsets.UTF_8));
    private static final SecretKey secretKeyRefresh = Keys.hmacShaKeyFor("kw><r%GIi72f_qY!D=.._Vl`.tz,k&2UQb\"M%Y.X\"cPYE4umIzXAVQUs{(2[<W{".getBytes(StandardCharsets.UTF_8));
//    private final static Key secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
//    private final static Key secretKeyRefresh = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final EmployeeDispatcher employeeDispatcher;
    private final PasswordService passwordService;

    public AuthorizationProvider(EmployeeDispatcher employeeDispatcher, PasswordService passwordService) {
        this.employeeDispatcher = employeeDispatcher;
        this.passwordService = passwordService;
    }

    public TokenChainWithUserInfo signIn(String login, String password) throws EntryNotFound, IllegalFields {
        Employee employee = employeeDispatcher.getEmployee(login);

        if (employee == null || employee.getDeleted()) throw new EntryNotFound("Пользователь не найден");
        if (Objects.isNull(employee.getPassword()) || employee.getPassword().isBlank()) throw new IllegalFields("Вход не возможен, у данного пользователя нет пароля");

        if (passwordService.decryptPassword(password, employee.getPassword())) {
            return new TokenChainWithUserInfo(TokenChain.builder().token(generateToken(employee)).refreshToken(generateRefreshToken(employee)).build(), employee);
        } else {
            return null;
        }
    }

    private String generateToken(Employee employee) {
        Map<String, Integer> claimsMap = new HashMap<>();
        claimsMap.put("access", employee.getAccess());
        return Jwts.builder()
                .setClaims(claimsMap)
                .setSubject(employee.getLogin())
                .setExpiration(new Date(System.currentTimeMillis() + JWS_EXPIRATION))
                .signWith(secretKey)
                .compact();
    }

    private String generateRefreshToken(Employee employee) {
        return Jwts.builder()
                .setSubject(employee.getLogin())
                .setExpiration(new Date(System.currentTimeMillis() + JWS_REFRESH_EXPIRATION))
                .signWith(secretKeyRefresh)
                .compact();
    }

    private static Claims parseToken(String token) throws JwsTokenParseError {
        try {
            return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
        } catch (Exception e) {
            throw new JwsTokenParseError();
        }
    }

    private static Claims parseRefreshToken(String token) throws JwsTokenParseError {
        try {
            return Jwts.parserBuilder().setSigningKey(secretKeyRefresh).build().parseClaimsJws(token).getBody();
        } catch (Exception e) {
            throw new JwsTokenParseError();
        }
    }

    public TokenChain doRefreshTokenChain(String token) throws JwsTokenParseError {
        Claims refreshClaims = parseRefreshToken(token);
        String login = refreshClaims.getSubject();
        boolean isExpired = Instant.now().isAfter(refreshClaims.getExpiration().toInstant());
        if (isExpired) return null;
        Employee employeeByLogin = null;
        try {
            employeeByLogin = employeeDispatcher.getEmployee(login);
        } catch (EntryNotFound e) {
            return null;
        }
        Boolean isDeleted = employeeByLogin.getDeleted();
        if (isDeleted) return null;
        return TokenChain.builder().token(generateToken(employeeByLogin)).refreshToken(generateRefreshToken(employeeByLogin)).build();
    }

    public String getLoginFromToken(String token){
        Claims claims = null;
        try {
            claims = parseToken(token);
            return claims.getSubject();
        } catch (JwsTokenParseError ignored) {
        }
        return null;
    }

    public Boolean tokenValidate(String token, Boolean withExpired) {
        Claims claims = null;
        try {
            claims = parseToken(token);
        } catch (JwsTokenParseError e) {
            return false;
        }
        String login = claims.getSubject();
        boolean isExpired = Instant.now().isAfter(claims.getExpiration().toInstant());
        if (isExpired && withExpired) return false;
        Employee employeeByLogin = null;
        try {
            employeeByLogin = employeeDispatcher.getEmployee(login);
        } catch (EntryNotFound e) {
            return false;
        }
        boolean isDeleted = employeeByLogin.getDeleted();
        boolean isAccessChanged = !Objects.equals(employeeByLogin.getAccess(), claims.get("access", Integer.class));
        return !isDeleted && !isAccessChanged;
    }

    public Boolean refreshTokenValidate(String refreshToken) {
        Claims claims = null;
        try {
            claims = parseRefreshToken(refreshToken);
        } catch (JwsTokenParseError e) {
            return false;
        }
        String login = claims.getSubject();
        boolean isExpired = Instant.now().isAfter(claims.getExpiration().toInstant());
        if (isExpired) return false;
        Employee employeeByLogin = null;
        try {
            employeeByLogin = employeeDispatcher.getEmployee(login);
        } catch (EntryNotFound e) {
            return false;
        }
        Boolean isDeleted = employeeByLogin.getDeleted();
        return !isDeleted;
    }

    public static TokenChain getTokensFromCookie(List<Cookie> cookies){
        String token = null;
        String refreshToken = null;
        for (Cookie cookie : cookies) {
            switch (cookie.getName()) {
                case "Token":
                    token = cookie.getValue();
                    break;
                case "Refresh-Token":
                    refreshToken = cookie.getValue();
                    break;
            }
        }
        return TokenChain.builder().token(token).refreshToken(refreshToken).build();
    }

    public static String getLoginFromCookie(List<Cookie> cookies){
        TokenChain tokenChain = getTokensFromCookie(cookies);
        try {
            Claims token = parseToken(tokenChain.getToken());
            return token.getSubject();
        } catch (JwsTokenParseError e) {
            return null;
        }
    }

    @Getter
    @Setter
    @Builder
    public static class TokenChain {
        private String token;
        private String refreshToken;

        public Cookie getTokenCookie() {
            Cookie cookie = new Cookie("Token", token);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge((int) (AuthorizationProvider.JWS_EXPIRATION / 1000L));
            return cookie;
        }

        public Cookie getRefreshTokenCookie() {
            Cookie cookie = new Cookie("Refresh-Token", refreshToken);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge((int) (AuthorizationProvider.JWS_REFRESH_EXPIRATION / 1000L));
            return cookie;
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class TokenChainWithUserInfo{
        private TokenChain tokenChain;
        private Employee employee;
    }
}
