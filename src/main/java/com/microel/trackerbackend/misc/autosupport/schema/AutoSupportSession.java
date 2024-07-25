package com.microel.trackerbackend.misc.autosupport.schema;

import com.microel.trackerbackend.controllers.telegram.CallbackData;
import com.microel.trackerbackend.controllers.telegram.TelegramMessageFactory;
import com.microel.trackerbackend.misc.autosupport.AutoSupportContext;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AutoSupportSession {
    private final AutoSupportContext context;
    private final AutoSupportStorage storage;
    private final Node initialNode;
    private final TelegramMessageFactory userChatFactory;
    @Nullable
    private UUID awaitingInput = null;
    @Nullable
    private Node currentNode = null;
    @Nullable
    private Runnable onCloseHandler = null;

    public AutoSupportSession(AutoSupportContext context, AutoSupportStorage storage, Node initialNode, TelegramMessageFactory userChatFactory) {
        this.context = context;
        this.storage = storage;
        this.initialNode = initialNode;
        this.userChatFactory = userChatFactory;
    }

    public boolean isAwaitingInput() {
        return awaitingInput != null;
    }

    public void sendInitialNode(Long userId) {
        currentNode = initialNode;
        currentNode.setHasPhoneTyped(false);
        runNode(currentNode, userId);
    }

    private void handleClose() {
        if (onCloseHandler != null) onCloseHandler.run();
    }

    public void callbackUpdate(Long userId, CallbackData data, Runnable afterRunNode) {
        if (currentNode == null) {
            handleClose();
            return;
        }
        Node children = currentNode.getChildrenById(data.getUUID());
        if (children == null) {
            handleClose();
            return;
        }
        currentNode = children;
        currentNode.setHasPhoneTyped(false);
        runNode(currentNode, userId);
        afterRunNode.run();
    }

    public void phoneNumberRejected(Long userId) {
        if (currentNode == null) {
            handleClose();
            return;
        }
        currentNode.setHasPhoneTyped(true);
        runNode(currentNode, userId);
    }

    public void textUpdate(Update update, Long userId) {
        if (currentNode == null) {
            handleClose();
            return;
        }
        if (isAwaitingInput()) {
            String text = update.getMessage().getText();
            storage.getInputResults().compute(awaitingInput, (k, v) -> text);
            awaitingInput = null;
            if (Objects.equals(currentNode.getType(), Node.Type.INPUT)){
                List<Node> children = currentNode.getChildren();
                if (children == null || children.isEmpty()) {
                    handleClose();
                    return;
                }
                currentNode = children.get(0);
                currentNode.setHasPhoneTyped(false);
                runNode(currentNode, userId);
            } else if (Objects.equals(currentNode.getType(), Node.Type.TICKET)) {
                currentNode.setHasPhoneTyped(true);
                runNode(currentNode, userId);
            }
        } else {
            currentNode.setHasPhoneTyped(false);
            runNode(currentNode, userId);
        }
    }

    private void runNode(Node runningNode, Long userId) {
        if (currentNode == null) return;
        switch (runningNode.getType()) {
            case NORMAL -> runNormalNode(runningNode, userId);
            case REDIRECT -> runRedirectNode(runningNode, userId);
            case TRUNK -> runTrunkNode(runningNode, userId);
            case PREDICATE -> runPredicateNode(runningNode, userId);
            case INPUT -> runInputNode(runningNode, userId);
            case TICKET -> runTicketNode(runningNode, userId);
        }
    }

    private void runNormalNode(Node runningNode, Long userId) {
        try {
            runningNode.doPreprocess(context, storage, userId);
            runningNode.send(storage, userChatFactory);
        } catch (Exception e) {
            handleClose();
        }
        List<Node> children = runningNode.getChildren();
        if (children == null || children.isEmpty()) {
            handleClose();
        }
    }

    private void runRedirectNode(Node runningNode, Long userId) {
        final UUID redirectId = runningNode.getRedirectId();
        if (redirectId == null) {
            handleClose();
            return;
        }
        final Node foundNode = findInNodesById(redirectId, initialNode);
        if (foundNode == null) {
            handleClose();
            return;
        }
        currentNode = foundNode;
        currentNode.setHasPhoneTyped(false);
        runNode(currentNode, userId);
    }

    private void runTrunkNode(Node runningNode, Long userId) {
        List<Node> children = runningNode.getChildren();
        if (children == null || children.isEmpty()) {
            handleClose();
            return;
        }
        currentNode = children.get(0);
        currentNode.setHasPhoneTyped(false);
        runNode(currentNode, userId);
    }

    private void runPredicateNode(Node runningNode, Long userId) {
        try {
            if (runningNode.getPredicateRedirection() == null) return;
            final boolean isPassed = runningNode.doPredicate(context, storage, userId);
            Node nextNode = null;
            if (isPassed)
                nextNode = runningNode.getPredicatePassedNode();
            else
                nextNode = runningNode.getPredicateFailedNode();
            if (nextNode != null) {
                currentNode = nextNode;
                currentNode.setHasPhoneTyped(false);
                runNode(currentNode, userId);
            } else {
                handleClose();
            }
        } catch (Exception e) {
            handleClose();
        }
    }

    private void runInputNode(Node runningNode, Long userId) {
        try {
            runningNode.doPreprocess(context, storage, userId);
            runningNode.send(storage, userChatFactory);
            awaitingInput = runningNode.getId();
        } catch (Exception e) {
            handleClose();
        }
    }

    private void runTicketNode(Node runningNode, Long userId) {
        try {
            if(runningNode.getHasPhoneTyped()){
                runningNode.doCreateTicket(context, storage, userId);
                runningNode.send(storage, userChatFactory);
                handleClose();
            }else{
                runningNode.send(storage, userChatFactory);
                awaitingInput = runningNode.getId();
            }
        } catch (Exception e) {
            handleClose();
        }
    }

    @Nullable
    private Node findInNodesById(@NotNull UUID id, @Nullable Node node) {
        if (node == null) {
            return null;
        }
        if (Objects.equals(node.getId(), id)) return node;
        List<Node> children = node.getChildren();
        if (children == null || children.isEmpty()) return null;
        for (Node child : children) {
            final Node foundNode = findInNodesById(id, child);
            if (foundNode != null) return foundNode;
        }
        return null;
    }

    public void onClose(Runnable handler) {
        onCloseHandler = handler;
    }
}
