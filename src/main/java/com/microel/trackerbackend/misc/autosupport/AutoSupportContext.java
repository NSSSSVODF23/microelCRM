package com.microel.trackerbackend.misc.autosupport;

import com.microel.trackerbackend.misc.autosupport.schema.preprocessors.PreprocessorMetadataProvider;
import com.microel.trackerbackend.services.UserAccountService;
import com.microel.trackerbackend.services.external.billing.ApiBillingController;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class AutoSupportContext {
    private final UserAccountService userAccountService;
    private final ApiBillingController billingController;

    public AutoSupportContext(UserAccountService userAccountService, ApiBillingController billingController) {
        this.userAccountService = userAccountService;
        this.billingController = billingController;
    }

}
