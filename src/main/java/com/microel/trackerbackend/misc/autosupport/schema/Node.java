package com.microel.trackerbackend.misc.autosupport.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microel.trackerbackend.controllers.telegram.TelegramMessageFactory;
import com.microel.trackerbackend.misc.autosupport.AutoSupportContext;
import com.microel.trackerbackend.misc.autosupport.SendingNodeException;
import com.microel.trackerbackend.misc.autosupport.schema.predicates.IPredicate;
import com.microel.trackerbackend.misc.autosupport.schema.predicates.PredicateType;
import com.microel.trackerbackend.misc.autosupport.schema.predicates.impl.*;
import com.microel.trackerbackend.misc.autosupport.schema.preprocessors.IPreprocessor;
import com.microel.trackerbackend.misc.autosupport.schema.preprocessors.PreprocessorType;
import com.microel.trackerbackend.misc.autosupport.schema.preprocessors.UserInfoPreprocessor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Node {
    private UUID id;
    private String name;
    private Type type;
    @Nullable
    private List<PreprocessorType> preprocessorTypes;
    @Nullable
    private PredicateType predicateType;
    @Nullable
    private Map<String, String> predicateArgumentsToTokensMap;
    @Nullable
    private Map<Integer, UUID> predicateRedirection;
    @Nullable
    private UUID redirectId;
    @Nullable
    private String messageTemplate;
    @Nullable
    private String ticketTitle;
    @Nullable
    private String ticketTemplate;
    @Nullable
    private UUID parent;
    @Nullable
    private List<Node> children;
    @JsonIgnore
    private Boolean hasPhoneTyped = false; // Для обработки ноды Ticket

    public static IPreprocessor createPreprocessor(PreprocessorType type) {
        return switch (type) {
            case USER_INFO -> new UserInfoPreprocessor();
        };
    }

    public static IPredicate createPredicate(PredicateType type) {
        return switch (type) {
            case USER_CREDENTIALS -> new UserCredentialsPredicate();
            case AUTH_USER -> new AuthUserPredicate();
            case POSITIVE_BALANCE -> new PositiveBalancePredicate();
            case DEFERRED_PAYMENT -> new DeferredPaymentPredicate();
            case HAS_AUTH_HARDWARE -> new HasAuthHardwarePredicate();
            case HAS_ONLINE_HARDWARE -> new HasOnlineHardwarePredicate();
            case IS_LARGE_UPTIME -> new IsLargeUptimePredicate();
        };
    }

    public static String prepareMessage(String template, AutoSupportStorage storage) {
        Pattern pattern = Pattern.compile("\\{([A-z:\\d-]+)}");
        Matcher matcher = pattern.matcher(template);

        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            final String token = matcher.group(1);
            final String[] subTokens = token.split(":");
            if (subTokens.length == 2) {
                final UUID inputNodeId = UUID.fromString(subTokens[1]);
                String replacement = storage.getInputResults().get(inputNodeId);
                if (replacement != null) {
                    matcher.appendReplacement(result, replacement);
                }
            } else if (subTokens.length == 3) {
                final UUID preprocessNodeId = UUID.fromString(subTokens[1]);
                final String preprocessNodeKey = subTokens[2];
                String replacement = storage.getPreprocessResults().get(preprocessNodeId).get(preprocessNodeKey);
                if (replacement != null) {
                    matcher.appendReplacement(result, replacement);
                }
            }
        }

        matcher.appendTail(result);

        return result.toString();
    }

    @JsonIgnore
    @Nullable
    public Node getChildrenById(UUID id) {
        if (children == null) return null;
        return children.stream().filter(node -> node.getId().equals(id)).findFirst().orElse(null);
    }

    @JsonIgnore
    private List<IPreprocessor> getProcessorList() {
        if (preprocessorTypes == null) return List.of();
        return preprocessorTypes.stream().map(Node::createPreprocessor).toList();
    }

    @JsonIgnore
    @Nullable
    private IPredicate getPredicate() {
        if (predicateType == null) return null;
        return createPredicate(predicateType);
    }

    public void doPreprocess(AutoSupportContext context, AutoSupportStorage storage, Long userId) {
        Map<String, String> results = getProcessorList().stream()
                .map(p -> p.process(context, userId)).flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        storage.getPreprocessResults().compute(getId(), (k, v) -> results);
    }

    public Boolean doPredicate(AutoSupportContext context, AutoSupportStorage storage, Long userId) {
        IPredicate predicate = getPredicate();
        if (predicate == null || getPredicateArgumentsToTokensMap() == null) return false;
        return predicate.evaluate(context, storage.getPredicateArgsByMap(getPredicateArgumentsToTokensMap()), userId);
    }

    public void doCreateTicket(AutoSupportContext context, AutoSupportStorage storage, Long userId) {
        context.createUserRequest(this, storage, userId);
    }

    public void send(AutoSupportStorage storage, TelegramMessageFactory factory) {
        try {
            factory.autoSupportMessage(this, storage).execute();
        } catch (TelegramApiException e) {
            throw new SendingNodeException(e.getMessage());
        }
    }

    @JsonIgnore
    @Nullable
    public Node getPredicatePassedNode() {
        if (predicateRedirection == null) return null;
        if (children == null || children.isEmpty()) return null;
        if (!predicateRedirection.containsKey(1)) return null;
        return children.stream().filter(node -> Objects.equals(node.getId(), predicateRedirection.get(1))).findFirst().orElse(null);
    }

    @JsonIgnore
    @Nullable
    public Node getPredicateFailedNode() {
        if (predicateRedirection == null) return null;
        if (children == null || children.isEmpty()) return null;
        if (!predicateRedirection.containsKey(0)) return null;
        return children.stream().filter(node -> Objects.equals(node.getId(), predicateRedirection.get(0))).findFirst().orElse(null);
    }

    @Getter
    public enum Type {
        NORMAL("NORMAL"),
        PREDICATE("PREDICATE"),
        INPUT("INPUT"),
        TRUNK("TRUNK"),
        REDIRECT("REDIRECT"),
        TICKET("TICKET");

        private final String type;

        Type(String type) {
            this.type = type;
        }

        public static List<Map<String, String>> getList() {
            return Stream.of(Type.values()).map(value -> Map.of("label", value.getLabel(), "value", value.getType())).toList();
        }

        public String getLabel() {
            return switch (this) {
                case NORMAL -> "Нода сообщения";
                case PREDICATE -> "Нода проверки";
                case INPUT -> "Нода ввода";
                case TRUNK -> "Нода перехода";
                case REDIRECT -> "Нода перенаправления";
                case TICKET -> "Нода заявки";
                default -> "Неизвестный тип уведомления";
            };
        }
    }
}
