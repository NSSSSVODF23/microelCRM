package com.microel.trackerbackend.services.api;

import com.microel.trackerbackend.controllers.telegram.TelegramController;
import com.microel.trackerbackend.controllers.telegram.UserTelegramController;
import com.microel.trackerbackend.misc.DhcpIpRequestNotificationBody;
import com.microel.trackerbackend.modules.transport.Credentials;
import com.microel.trackerbackend.parsers.commutator.ra.DES28RemoteAccess;
import com.microel.trackerbackend.security.AuthorizationProvider;
import com.microel.trackerbackend.security.exceptions.JwsTokenParseError;
import com.microel.trackerbackend.services.UserAccountService;
import com.microel.trackerbackend.services.external.acp.AcpClient;
import com.microel.trackerbackend.services.external.acp.types.DhcpBinding;
import com.microel.trackerbackend.services.external.billing.ApiBillingController;
import com.microel.trackerbackend.storage.entities.acp.commutator.PortInfo;
import com.microel.trackerbackend.storage.entities.acp.commutator.SystemInfo;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("api/public")
public class PublicRequestController {

    private final AuthorizationProvider authorizationProvider;
    private final TelegramController telegramController;
    private final AcpClient acpClient;
    private final StompController stompController;
    private final UserTelegramController userTelegramController;
    private final UserAccountService userAccountService;
    private final ApiBillingController apiBillingController;

    public PublicRequestController(AuthorizationProvider authorizationProvider, TelegramController telegramController, AcpClient acpClient, StompController stompController, UserTelegramController userTelegramController, UserAccountService userAccountService, ApiBillingController apiBillingController) {
        this.authorizationProvider = authorizationProvider;
        this.telegramController = telegramController;
        this.acpClient = acpClient;
        this.stompController = stompController;
        this.userTelegramController = userTelegramController;
        this.userAccountService = userAccountService;
        this.apiBillingController = apiBillingController;
    }

    @PostMapping("sign-in")
    public ResponseEntity<AuthorizationProvider.TokenChainWithUserInfo> signIn(@RequestBody Credentials body, HttpServletResponse response) {
        if (!body.isCorrect()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {

            AuthorizationProvider.TokenChainWithUserInfo tokenChain = authorizationProvider.signIn(body.getLogin(), body.getPassword());
            if (tokenChain == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            response.addCookie(tokenChain.getTokenChain().getTokenCookie());
            response.addCookie(tokenChain.getTokenChain().getRefreshTokenCookie());

            return ResponseEntity.ok(tokenChain);

        } catch (EntryNotFound | IllegalFields e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//            throw new ResponseException(e.getMessage());
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
        if (request.getCookies() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        AuthorizationProvider.TokenChain requestTokens = AuthorizationProvider.getTokensFromCookie(List.of(request.getCookies()));
        if (requestTokens.getToken() != null && requestTokens.getRefreshToken() != null) {
            if (authorizationProvider.tokenValidate(requestTokens.getToken(), true)) {
                return ResponseEntity.ok().build();
            } else if (authorizationProvider.refreshTokenValidate(requestTokens.getRefreshToken())) {
                AuthorizationProvider.TokenChain tokenChain = null;
                try {
                    tokenChain = authorizationProvider.doRefreshTokenChain(requestTokens.getRefreshToken());
                } catch (JwsTokenParseError e) {
                    throw new ResponseException("Не удалось расшифровать токен");
                }
                response.addCookie(tokenChain.getTokenCookie());
                response.addCookie(tokenChain.getRefreshTokenCookie());
                return ResponseEntity.ok(tokenChain);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } else if (requestTokens.getRefreshToken() != null) {
            if (authorizationProvider.refreshTokenValidate(requestTokens.getRefreshToken())) {
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

    @PostMapping("out/dhcp/ip-request/notification")
    public ResponseEntity<Void> outDhcpIpRequestNotification(@RequestBody DhcpIpRequestNotificationBody body, HttpServletRequest request, HttpServletResponse response) {
        try {
            telegramController.sendDhcpIpRequestNotification(body);
            return ResponseEntity.ok().build();
        } catch (TelegramApiException e) {
            return ResponseEntity.ok().build();
        }
    }

    @PostMapping("incoming-update/dhcp-binding")
    public ResponseEntity<Void> incomingUpdateDhcpBinding(@RequestBody DhcpBinding body) {
        stompController.updateDhcpBinding(acpClient.prepareBinding(body));
        return ResponseEntity.ok().build();
    }

    @PostMapping("incoming-update/house-page-signal")
    public ResponseEntity<Void> incomingUpdateHousePageSignal(@RequestBody Integer vlan) {
        stompController.updateHousePageSignal(vlan);
        return ResponseEntity.ok().build();
    }

    @GetMapping("test/remote/{ip}/system-info")
    public ResponseEntity<SystemInfo> testRemote(@PathVariable String ip) {
        DES28RemoteAccess remoteAccess = new DES28RemoteAccess(ip);
        remoteAccess.auth();
        SystemInfo systemInfo = remoteAccess.getSystemInfo();
        remoteAccess.close();
        return ResponseEntity.ok(systemInfo);
    }

    @GetMapping("test/remote/{ip}/ports")
    public ResponseEntity<List<PortInfo>> testRemotePorts(@PathVariable String ip) {
        DES28RemoteAccess remoteAccess = new DES28RemoteAccess(ip);
        remoteAccess.auth();
        List<PortInfo> ports = remoteAccess.getPorts();
        remoteAccess.close();
        return ResponseEntity.ok(ports);
    }

    @PostMapping("user/telegram/auth")
    public ResponseEntity<Map<String, String>> userTelegramAuth(@RequestBody UserTelegramController.UserTelegramCredentials credentials) {
        if(!userTelegramController.checkSecret(credentials))
            return ResponseEntity.ok(Map.of("error", "wrong_auth_token"));
        if(!userAccountService.isCredentialsValid(credentials.getLogin(), credentials.getPassword()))
            return ResponseEntity.ok(Map.of("error",  "wrong_credentials"));
        ApiBillingController.TotalUserInfo userInfo = apiBillingController.getUserInfo(credentials.getLogin());
        if(!userTelegramController.doSendAuthConfirmation(credentials.getId(), userInfo))
            return ResponseEntity.ok(Map.of("error",  "wrong_tlg_user"));
        userAccountService.doSaveUserAccount(credentials);
        return ResponseEntity.ok(Map.of("success",  "OK"));
    }
}
