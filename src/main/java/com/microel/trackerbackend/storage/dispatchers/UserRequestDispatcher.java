package com.microel.trackerbackend.storage.dispatchers;

import com.microel.tdo.EventType;
import com.microel.tdo.UpdateCarrier;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.entities.users.UserRequest;
import com.microel.trackerbackend.storage.exceptions.AlreadyExists;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.repositories.UserRequestRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional(readOnly = true)
public class UserRequestDispatcher {
    private final UserRequestRepository userRequestRepository;
    private final StompController stompController;

    public UserRequestDispatcher(UserRequestRepository userRequestRepository, StompController stompController) {
        this.userRequestRepository = userRequestRepository;
        this.stompController = stompController;
    }

    @Transactional
    public UserRequest createRequest(String login, @Nullable UserRequest.Type type, String title, String description, String source, @Nullable String chatId, @Nullable String phoneNumber) {
        // Ищем похожие нереализованные запросы
        List<UserRequest> existedRequests = userRequestRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("userLogin"), login),
                cb.equal(root.get("description"), description),
                cb.isNull(root.get("processedBy")),
                cb.isFalse(root.get("deleted"))
        ));
        if (!existedRequests.isEmpty()) {
            throw new AlreadyExists("Данный запрос уже существует");
        }
        UserRequest userRequest = UserRequest.of(login, type, title, description, source, chatId, phoneNumber);
        userRequestRepository.save(userRequest);
        stompController.updateTlgUserRequest(UpdateCarrier.from(EventType.CREATE, userRequest));
        return userRequest;
    }

    @Transactional
    public void cancelRequest(Long requestId) {
        UserRequest userRequest = userRequestRepository.findById(requestId).orElseThrow(() -> new EntryNotFound("Целевой запрос не найден"));
        userRequest.setDeleted(true);
        userRequestRepository.save(userRequest);
        stompController.updateTlgUserRequest(UpdateCarrier.from(EventType.DELETE, userRequest));
    }

    public List<UserRequest> getUnprocessedRequestsByLogin(String login){
        return userRequestRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("userLogin"), login),
                cb.isNull(root.get("processedBy")),
                cb.isFalse(root.get("deleted"))
        ));
    }
}
