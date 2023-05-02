package com.microel.trackerbackend.services.api;

import com.microel.trackerbackend.modules.transport.Credentials;
import com.microel.trackerbackend.security.AuthorizationProvider;
import com.microel.trackerbackend.security.exceptions.JwsTokenParseError;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
@RequestMapping("api/public")
public class PublicRequestController {

    private final AuthorizationProvider authorizationProvider;

    public PublicRequestController(AuthorizationProvider authorizationProvider) {
        this.authorizationProvider = authorizationProvider;
    }

    @PostMapping("sign-in")
    public ResponseEntity<AuthorizationProvider.TokenChain> signIn(@RequestBody Credentials body, HttpServletResponse response) {
        if (!body.isCorrect()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try{

            AuthorizationProvider.TokenChain tokenChain = authorizationProvider.signIn(body.getLogin(), body.getPassword());
            if (tokenChain == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            response.addCookie(tokenChain.getTokenCookie());
            response.addCookie(tokenChain.getRefreshTokenCookie());

            return ResponseEntity.ok(tokenChain);

        } catch (EntryNotFound | IllegalFields e) {
            throw new ResponseException(e.getMessage());
        }
    }

    @PostMapping("sign-out")
    public ResponseEntity<Void> signOut(HttpServletRequest request, HttpServletResponse response) {
        // Устанавливаем пустые куки
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                cookie.setPath("/");
                cookie.setValue("");
                cookie.setHttpOnly(true);
                cookie.setMaxAge(0);
                response.addCookie(cookie);
            }
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("auth-checkout")
    public ResponseEntity<AuthorizationProvider.TokenChain> getAuthCheckout(HttpServletRequest request, HttpServletResponse response) {
        if(request.getCookies() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        AuthorizationProvider.TokenChain requestTokens = AuthorizationProvider.getTokensFromCookie(List.of(request.getCookies()));
        if (requestTokens.getToken() != null && requestTokens.getRefreshToken() != null) {
            if(authorizationProvider.tokenValidate(requestTokens.getToken(), true)){
                return ResponseEntity.ok().build();
            }else if(authorizationProvider.refreshTokenValidate(requestTokens.getRefreshToken())){
                AuthorizationProvider.TokenChain tokenChain = null;
                try {
                    tokenChain = authorizationProvider.doRefreshTokenChain(requestTokens.getRefreshToken());
                } catch (JwsTokenParseError e) {
                    throw new ResponseException("Не удалось расшифровать токен");
                }
                response.addCookie(tokenChain.getTokenCookie());
                response.addCookie(tokenChain.getRefreshTokenCookie());
                return ResponseEntity.ok(tokenChain);
            }else{
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } else if (requestTokens.getRefreshToken() != null) {
            if(authorizationProvider.refreshTokenValidate(requestTokens.getRefreshToken())){
                AuthorizationProvider.TokenChain tokenChain = null;
                try {
                    tokenChain = authorizationProvider.doRefreshTokenChain(requestTokens.getRefreshToken());
                } catch (JwsTokenParseError e) {
                    throw new ResponseException("Не удалось расшифровать токен");
                }
                response.addCookie(tokenChain.getTokenCookie());
                response.addCookie(tokenChain.getRefreshTokenCookie());
                return ResponseEntity.ok(tokenChain);
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
