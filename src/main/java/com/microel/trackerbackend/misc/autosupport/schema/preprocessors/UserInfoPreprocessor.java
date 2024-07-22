package com.microel.trackerbackend.misc.autosupport.schema.preprocessors;

import com.microel.trackerbackend.misc.autosupport.AutoSupportContext;
import com.microel.trackerbackend.services.external.billing.ApiBillingController;
import com.microel.trackerbackend.storage.entities.users.TelegramUserAuth;

import java.util.List;
import java.util.Map;

public class UserInfoPreprocessor implements IPreprocessor {
    @Override
    public PreprocessorType type() {
        return PreprocessorType.USER_INFO;
    }

    @Override
    public List<String> getOutputValues() {
        return List.of("username", "user_address");
    }

    @Override
    public Map<String, String> process(AutoSupportContext context, Long userId) {
        TelegramUserAuth userAccount = context.getUserAccountService().getUserAccount(userId);
        ApiBillingController.TotalUserInfo userInfo = context.getBillingController().getUserInfo(userAccount.getUserLogin());
        return Map.of("username", userInfo.getUname(), "user_address", userInfo.getIbase().getAddr());
    }
}
