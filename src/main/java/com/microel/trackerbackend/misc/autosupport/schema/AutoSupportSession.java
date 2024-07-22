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
        runNode(currentNode, userId);
        afterRunNode.run();
    }

    public void textUpdate(Update update, Long userId) {
        if (currentNode == null) {
            handleClose();
            return;
        }
        if (isAwaitingInput()) {
            String text = update.getMessage().getText();
            storage.getInputResults().compute(awaitingInput, (k, v) -> text);
            System.out.println("Input storage: " + storage.getInputResults());
            List<Node> children = currentNode.getChildren();
            if (children == null || children.isEmpty()) {
                handleClose();
                return;
            }
            currentNode = children.get(0);
            awaitingInput = null;
            runNode(currentNode, userId);
        } else {
            runNode(currentNode, userId);
        }
    }

    private void runNode(Node runningNode, Long userId) {
        if (currentNode == null) return;
        List<Node> children = runningNode.getChildren();
        switch (runningNode.getType()) {
            case NORMAL -> {
                try {
                    runningNode.doPreprocess(context, storage, userId);
                    runningNode.send(storage, userChatFactory);
                    if (runningNode.getChildren() == null || runningNode.getChildren().isEmpty()) {
                        handleClose();
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    handleClose();
                }
            }
            case REDIRECT -> {
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
                runNode(currentNode, userId);
            }
            case TRUNK -> {
                if (children == null || children.isEmpty()) {
                    handleClose();
                    return;
                }
                currentNode = children.get(0);
                runNode(currentNode, userId);
            }
            case PREDICATE -> {
                try {
                    if (currentNode.getPredicateRedirection() == null) return;
                    final Boolean isPassed = runningNode.doPredicate(context, storage, userId);
                    Node nextNode = null;
                    if (isPassed)
                        nextNode = currentNode.getPredicatePassedNode();
                    else
                        nextNode = currentNode.getPredicateFailedNode();
                    if (nextNode != null) {
                        currentNode = nextNode;
                        runNode(currentNode, userId);
                    } else {
                        handleClose();
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    handleClose();
                }
            }
            case INPUT -> {
                try {
                    runningNode.doPreprocess(context, storage, userId);
                    runningNode.send(storage, userChatFactory);
                    awaitingInput = runningNode.getId();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    handleClose();
                }
            }
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
