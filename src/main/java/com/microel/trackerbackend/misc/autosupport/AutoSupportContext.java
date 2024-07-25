package com.microel.trackerbackend.misc.autosupport;

import com.microel.trackerbackend.misc.autosupport.schema.AutoSupportSession;
import com.microel.trackerbackend.misc.autosupport.schema.AutoSupportStorage;
import com.microel.trackerbackend.misc.autosupport.schema.Node;
import com.microel.trackerbackend.misc.autosupport.schema.preprocessors.PreprocessorMetadataProvider;
import com.microel.trackerbackend.services.UserAccountService;
import com.microel.trackerbackend.services.external.acp.AcpClient;
import com.microel.trackerbackend.services.external.acp.AcpXmlRpcController;
import com.microel.trackerbackend.services.external.billing.ApiBillingController;
import com.microel.trackerbackend.storage.dispatchers.UserRequestDispatcher;
import com.microel.trackerbackend.storage.entities.users.TelegramUserAuth;
import com.microel.trackerbackend.storage.entities.users.UserRequest;
import com.microel.trackerbackend.storage.exceptions.AlreadyExists;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class AutoSupportContext {
    private final UserAccountService userAccountService;
    private final ApiBillingController billingController;
    private final AcpClient acpClient;
    private final UserRequestDispatcher userRequestDispatcher;

    public AutoSupportContext(UserAccountService userAccountService, ApiBillingController billingController, AcpClient acpClient, UserRequestDispatcher userRequestDispatcher) {
        this.userAccountService = userAccountService;
        this.billingController = billingController;
        this.acpClient = acpClient;
        this.userRequestDispatcher = userRequestDispatcher;
    }

    public void createUserRequest(Node node, AutoSupportStorage storage, Long chatId){
        final TelegramUserAuth userAccount = userAccountService.getUserAccount(chatId);
        if(userAccount == null) throw new IllegalArgumentException("User not found");
        final String ticketDescription = Node.prepareMessage(node.getTicketTemplate(), storage);
        userRequestDispatcher.createRequest(userAccount.getUserLogin(), null, node.getTicketTitle(), ticketDescription,
                UserRequest.Source.TELEGRAM, chatId.toString(), storage.getInputResults().get(node.getId()));
    }
}
