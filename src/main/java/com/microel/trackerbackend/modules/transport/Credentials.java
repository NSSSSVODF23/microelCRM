package com.microel.trackerbackend.modules.transport;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Credentials {
    private String login;
    private String password;

    public Boolean isCorrect(){
        return login != null && password != null && !login.isBlank() && !password.isBlank();
    }
}
