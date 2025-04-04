package com.microel.trackerbackend.services.api;

import com.microel.tdo.UpdateCarrier;
import com.microel.tdo.pon.Worker;
import com.microel.tdo.pon.events.OntStatusChangeEvent;
import com.microel.tdo.pon.schema.events.PonSchemeChangeEvent;
import com.microel.tdo.pon.terminal.OpticalNetworkTerminal;
import com.microel.trackerbackend.controllers.configuration.entity.UserTelegramConf;
import com.microel.trackerbackend.misc.*;
import com.microel.trackerbackend.misc.task.counting.*;
import com.microel.trackerbackend.services.RemoteTelnetService;
import com.microel.trackerbackend.services.api.controllers.ProxyRemoteConnectionController;
import com.microel.trackerbackend.services.external.acp.AcpClient;
import com.microel.trackerbackend.services.external.acp.types.SwitchBaseInfo;
import com.microel.trackerbackend.storage.configurations.StompConfig;
import com.microel.trackerbackend.controllers.configuration.entity.AcpConf;
import com.microel.trackerbackend.controllers.configuration.entity.BillingConf;
import com.microel.trackerbackend.controllers.configuration.entity.TelegramConf;
import com.microel.trackerbackend.parsers.oldtracker.OldTracker;
import com.microel.trackerbackend.services.MonitoringService;
import com.microel.trackerbackend.services.external.acp.types.DhcpBinding;
import com.microel.trackerbackend.services.external.acp.types.Switch;
import com.microel.trackerbackend.services.external.billing.ApiBillingController;
import com.microel.trackerbackend.storage.dispatchers.TaskDispatcher;
import com.microel.trackerbackend.storage.dispatchers.TemperatureSensorsDispatcher;
import com.microel.trackerbackend.storage.dto.chat.ChatDto;
import com.microel.trackerbackend.storage.dto.comment.CommentDto;
import com.microel.trackerbackend.storage.dto.team.EmployeeDto;
import com.microel.trackerbackend.storage.entities.acp.commutator.AcpCommutator;
import com.microel.trackerbackend.storage.entities.address.City;
import com.microel.trackerbackend.storage.entities.address.House;
import com.microel.trackerbackend.storage.entities.address.Street;
import com.microel.trackerbackend.storage.entities.chat.Chat;
import com.microel.trackerbackend.storage.entities.chat.SuperMessage;
import com.microel.trackerbackend.storage.entities.comments.events.TaskEvent;
import com.microel.trackerbackend.storage.entities.equipment.ClientEquipment;
import com.microel.trackerbackend.storage.entities.salary.PaidAction;
import com.microel.trackerbackend.storage.entities.salary.PaidWork;
import com.microel.trackerbackend.storage.entities.salary.PaidWorkGroup;
import com.microel.trackerbackend.storage.entities.salary.WorkingDay;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.TypesOfContracts;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.task.utils.TaskTag;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.notification.Notification;
import com.microel.trackerbackend.storage.entities.team.util.Department;
import com.microel.trackerbackend.storage.entities.team.util.Position;
import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import com.microel.trackerbackend.storage.entities.users.Review;
import com.microel.trackerbackend.storage.entities.users.UserRequest;
import com.microel.trackerbackend.storage.entities.users.UserTariff;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class StompController {
    private final SimpMessagingTemplate stompBroker;

    public StompController(SimpMessagingTemplate stompBroker) {
        this.stompBroker = stompBroker;
    }

    private void sendAllAsync(Object payload, String... patch) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            String destination = String.join("/", patch);
            stompBroker.convertAndSend(StompConfig.SIMPLE_BROKER_PREFIX + "/" + destination, payload);
        });
        executorService.shutdown();
    }

    private void sendAll(Object payload, String... patch) {
        String destination = String.join("/", patch);
        stompBroker.convertAndSend(StompConfig.SIMPLE_BROKER_PREFIX + "/" + destination, payload);
    }

    private void sendToUser(String login, Object payload, String... patch) {
        String destination = String.join("/", patch);
        stompBroker.convertAndSendToUser(login, destination, payload);
    }

    private void sendToUserAsync(String login, Object payload, String... patch) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            String destination = String.join("/", patch);
            stompBroker.convertAndSendToUser(login, destination, payload);
        });
        executorService.shutdown();
    }

    public void createTask(Task task) {
        sendAll(task, "task", "create");
        for (Employee employee : task.getAllEmployeesObservers()) {
            sendToUser(employee.getLogin(), task, "task", "create");
        }
    }

    public void updateTask(Task task) {
        sendAll(task, "task", task.getTaskId().toString(), "update");
        sendAll(task, "task", "update");
    }

    public void updateTaskCounter(Long count, AbstractTaskCounterPath path){
        if (path instanceof TaskTagPath tagPath)
            sendAll(count, "task", "counter", tagPath.getSchedulingType(), tagPath.getTaskStatus(),
                    tagPath.getTaskClassId(), tagPath.getTaskTypeId(), "dir", tagPath.getTaskDirectoryId(), tagPath.getTaskTagId());
        else if (path instanceof TaskDirectoryPath directoryPath)
            sendAll(count, "task", "counter", directoryPath.getSchedulingType(), directoryPath.getTaskStatus(),
                    directoryPath.getTaskClassId(), directoryPath.getTaskTypeId(), "dir", directoryPath.getTaskDirectoryId());
        else if(path instanceof TaskScheduleDatePath scheduleDatePath)
            sendAll(count, "task", "counter", scheduleDatePath.getSchedulingType(), scheduleDatePath.getTaskStatus(),
                    scheduleDatePath.getTaskClassId(), scheduleDatePath.getTaskTypeId(), "actual-time", scheduleDatePath.getActualFrom());
        else if(path instanceof TaskTermDatePath termDatePath)
            sendAll(count, "task", "counter", termDatePath.getSchedulingType(), termDatePath.getTaskStatus(),
                    termDatePath.getTaskClassId(), termDatePath.getTaskTypeId(), "term-time", termDatePath.getActualTo());
        else if (path instanceof TaskClosingDatePath closingDatePath)
            sendAll(count, "task", "counter", closingDatePath.getSchedulingType(), closingDatePath.getTaskStatus(),
                    closingDatePath.getTaskClassId(), closingDatePath.getTaskTypeId(), "close-time", closingDatePath.getDateOfClose());
        else if (path instanceof TaskTypePath typePath)
            sendAll(count, "task", "counter", typePath.getSchedulingType(), typePath.getTaskStatus(),
                    typePath.getTaskClassId(), typePath.getTaskTypeId());
        else if (path instanceof TaskClassPath classPath)
            sendAll(count, "task", "counter", classPath.getSchedulingType(), classPath.getTaskStatus(),
                    classPath.getTaskClassId());
        else if (path instanceof TaskStatusPath statusPath)
            sendAll(count, "task", "counter", statusPath.getSchedulingType(), statusPath.getTaskStatus());
    }

    public void updateCachedTaskCounter(Long count, TaskDispatcher.FiltrationConditions conditions){
        sendAll(Map.of(
                "count", count,
                "conditions", conditions
        ), "task", "cached", "counter");
    }

    public void movedTask(){
        sendAll(true, "task", "moved");
    }

    public void createIncomingTask(String login, Task task){
        sendToUser(login, task, "task", "create");
    }

    public void deleteIncomingTask(String login, Task task) {
        sendToUser(login, task, "task", "delete");
    }

    public void createWorkLog(WorkLog workLog) {
        sendAll(workLog, "worklog", "create");
    }

    public void updateWorkLog(WorkLog workLog) {
        sendAll(workLog, "worklog", workLog.getWorkLogId().toString(), "update");
        sendAll(workLog, "worklog", "update");
    }

    public void closeWorkLog(WorkLog workLog) {
        sendAll(workLog, "worklog", "close");
    }

    public void createTaskEvent(Long taskId, TaskEvent taskEvent) {
        sendAll(taskEvent, "task", taskId.toString(), "event", "create");
    }

    public void createComment(CommentDto comment, String parentTaskId) {
        sendAll(comment, "task", parentTaskId, "comment", "create");
    }

    public void updateComment(CommentDto comment, String parentTaskId) {
        sendAll(comment, "task", parentTaskId, "comment", "update");
    }

    public void deleteComment(CommentDto comment, String parentTaskId) {
        sendAll(comment, "task", parentTaskId, "comment", "delete");
    }

    public void createEmployee(Employee employee) {
        sendAll(employee, "employee", "create");
    }

    public void updateEmployee(Employee employee) {
        sendAll(employee, "employee", employee.getLogin(), "update");
        sendAll(employee, "employee", "update");
    }

    public void deleteEmployee(Employee employee) {
        sendAll(employee, "employee", "delete");
    }

    public void createDepartment(Department department) {
        sendAll(department, "department", "create");
    }

    public void updateDepartment(Department department) {
        sendAll(department, "department", department.getDepartmentId().toString(), "update");
        sendAll(department, "department", "update");
    }

    public void deleteDepartment(Department department) {
        sendAll(department, "department", "delete");
    }

    public void createPosition(Position position) {
        sendAll(position, "position", "create");
    }

    public void updatePosition(Position position) {
        sendAll(position, "position", position.getPositionId().toString(), "update");
        sendAll(position, "position", "update");
    }

    public void deletePosition(Position position) {
        sendAll(position, "position", "delete");
    }

    public void createTaskTag(TaskTag taskTag) {
        sendAll(taskTag, "task-tag", "create");
    }

    public void updateTaskTag(TaskTag taskTag) {
        sendAll(taskTag, "task-tag", "update");
    }

    public void deleteTaskTag(TaskTag taskTag) {
        sendAll(taskTag, "task-tag", "delete");
    }

    public void sendNotification(Notification notification) {
        sendToUser(notification.getEmployee().getLogin(), notification, "notification", "create");
    }

    public void updateChat(Chat chat) {
        sendAll(chat, "chat", "update");
    }

    public void closeChat(Chat chat) {
        sendAll(chat, "chat", "close");
    }

    public void createMessage(SuperMessage message, Set<EmployeeDto> chatMembers) {
        chatMembers.forEach(employeeDto -> sendToUser(employeeDto.getLogin(), message, "chat", "message", "create"));
        sendAll(message, "chat", message.getParentChatId().toString(), "message", "create");
        sendAll(message, "chat", "message", "create");
    }

    public void updateMessage(SuperMessage message) {
        sendAll(message, "chat", message.getParentChatId().toString(), "message", "update");
        sendAll(message, "chat", "message", "update");
    }

    public void deleteMessage(SuperMessage deletedMessage) {
        sendAll(deletedMessage, "chat", deletedMessage.getParentChatId().toString(), "message", "delete");
        sendAll(deletedMessage, "chat", "message", "delete");
    }

    public void updateCountUnreadMessage(String login, Long chatId, Long count) {
        sendToUser(login, new Chat.UnreadCounter(chatId, count), "chat", "message", "unread");
    }

    public void updateTrackerParser(OldTracker.DTO tracker) {
        sendAll(tracker, "parser", "tracker", "update");
    }

    public void sendParserMessage(SimpleMessage simpleMessage) {
        sendAll(simpleMessage, "parser", "message");
    }

    public void createChat(ChatDto chat) {
        for (EmployeeDto employee : chat.getMembers()) {
            sendToUser(employee.getLogin(), chat, "chat", "create");
        }
    }

    public void createWireframe(Wireframe wireframe) {
        sendAll(wireframe, "wireframe", "create");
    }

    public void updateWireframe(Wireframe wireframe) {
        sendAll(wireframe, "wireframe", wireframe.getWireframeId().toString(), "update");
        sendAll(wireframe, "wireframe", "update");
    }

    public void deleteWireframe(Wireframe wireframe) {
        sendAll(wireframe, "wireframe", wireframe.getWireframeId().toString(), "delete");
        sendAll(wireframe, "wireframe", "delete");
    }

    public void createPaidAction(PaidAction paidAction) {
        sendAll(paidAction, "paid-action", "create");
    }

    public void updatePaidAction(PaidAction paidAction) {
        sendAll(paidAction, "paid-action", paidAction.getPaidActionId().toString(), "update");
        sendAll(paidAction, "paid-action", "update");
    }

    public void deletePaidAction(PaidAction paidAction) {
        sendAll(paidAction, "paid-action", paidAction.getPaidActionId().toString(), "delete");
        sendAll(paidAction, "paid-action", "delete");
    }

    public void createPaidWorkGroup(PaidWorkGroup paidWorkGroup) {
        sendAll(paidWorkGroup, "paid-work-group", "create");
    }

    public void updatePaidWorkGroup(PaidWorkGroup paidWorkGroup) {
        sendAll(paidWorkGroup, "paid-work-group", "update");
    }

    public void deletePaidWorkGroup(PaidWorkGroup paidWorkGroup) {
        sendAll(paidWorkGroup, "paid-work-group", "delete");
    }

    public void movePaidWorksTreeItem(TreeNode.MoveEvent moveEvent) {
        sendAll(moveEvent, "paid-works", "tree", "move");
    }

    public void updatePaidWorksTreeItem(TreeNode.UpdateEvent updateEvent) {
        sendAll(updateEvent, "paid-works", "tree", "update");
    }

    public void createPaidWorksTreeItem(TreeNode.UpdateEvent updateEvent) {
        sendAll(updateEvent, "paid-works", "tree", "create");
    }

    public void deletePaidWorksTreeItem(TreeNode.UpdateEvent updateEvent) {
        sendAll(updateEvent, "paid-works", "tree", "delete");
    }

    public void worksTreeReposition(List<TreeElementPosition> event) {
        sendAll(event, "paid-works", "tree", "reposition");
    }

    public void updatePaidWork(PaidWork paidWork) {
        sendAll(paidWork, "paid-work", paidWork.getPaidWorkId().toString(), "update");
        sendAll(paidWork, "paid-work", "update");
    }

    public void createWorkingDay(WorkingDay workingDay) {
        sendAll(workingDay, "working-day", "create");
    }

    public void updateWorkingDay(WorkingDay workingDay) {
        sendAll(workingDay, "working-day", workingDay.getWorkingDayId().toString(), "update");
        sendAll(workingDay, "working-day", "update");
    }

    public void createCity(City city) {
        sendAll(city, "city", "create");
    }

    public void updateCity(City city) {
        sendAll(city, "city", city.getCityId().toString(), "update");
        sendAll(city, "city", "update");
    }

    public void deleteCity(City city) {
        sendAll(city, "city", city.getCityId().toString(), "delete");
        sendAll(city, "city", "delete");
    }

    public void createStreet(Street street) {
        sendAll(street, "street", "create");
    }

    public void updateStreet(Street street) {
        sendAll(street, "street", street.getStreetId().toString(), "update");
        sendAll(street, "street", "update");
    }

    public void deleteStreet(Street street) {
        sendAll(street, "street", street.getStreetId().toString(), "delete");
        sendAll(street, "street", "delete");
    }

    public void createHouse(House house) {
        sendAll(house, "house", "create");
    }

    public void updateHouse(House house) {
        sendAll(house, "house", house.getHouseId().toString(), "update");
        sendAll(house, "house", "update");
    }

    public void deleteHouse(House house) {
        sendAll(house, "house", house.getHouseId().toString(), "delete");
        sendAll(house, "house", "delete");
    }

    public void pingMonitoring(MonitoringService.PingMonitoring pingMonitoring) {
        sendAll(pingMonitoring, "monitoring", "ping", pingMonitoring.getIp());
    }

    public void outputTelnetStream(RemoteTelnetService.OutputFrame output, String ip, String sessionId) {
        sendAll(output, "remote", "telnet", ip, sessionId);
    }

    public void telnetConnectionMessage(ProxyRemoteConnectionController.ConnectionCredentials credentials, Employee employee) {
        sendToUser(employee.getLogin(), credentials, "remote", "telnet", "connection-message");
    }

    public void changeBillingConfig(BillingConf billingConf) {
        sendAll(billingConf, "billing-config", "change");
    }

    public void updateBillingUser(ApiBillingController.TotalUserInfo userInfo) {
        sendAll(userInfo, "billing", "user", "update");
        sendAll(userInfo, "billing", "user", userInfo.getUname(), "update");
    }

    public void changeTelegramConfig(TelegramConf telegramConf) {
        sendAll(telegramConf, "telegram-config", "change");
    }

    public void changeUserTelegramConfig(UserTelegramConf telegramConf) {
        sendAll(telegramConf, "user-telegram-config", "change");
    }

    public void changeAcpConfig(AcpConf acpConf) {
        sendAll(acpConf, "acp-config", "change");
    }

    public void updateSalaryTable(SalaryTable salaryTable) {
        sendAll(salaryTable, "salary-table", "update");
    }

    public void createClientEquipment(ClientEquipment equipment) {
        sendAll(equipment, "client-equipment", "create");
    }

    public void updateClientEquipment(ClientEquipment equipment) {
        sendAll(equipment, "client-equipment", equipment.getClientEquipmentId().toString(), "update");
        sendAll(equipment, "client-equipment", "update");
    }

    public void deleteClientEquipment(ClientEquipment equipment) {
        sendAll(equipment, "client-equipment", equipment.getClientEquipmentId().toString(), "delete");
        sendAll(equipment, "client-equipment", "delete");
    }

    public void updateDhcpBinding(DhcpBinding binding) {
        sendAll(binding, "acp", "dhcp-binding", "update");
    }

    public void updateHousePageSignal(Integer vlan) {
        sendAll(Map.of("vlan", vlan), "acp", "dhcp-binding", "house-page", "update");
    }

    public void updateAcpCommutator(AcpCommutator updatedCommutator) {
        sendAll(updatedCommutator, "acp", "commutator", "status", "update");
    }

    public void createCommutator(Switch commutator) {
        sendAll(commutator, "acp", "commutator", "create");
    }

    public void updateCommutator(Switch commutator) {
        sendAll(commutator, "acp", "commutator", "update");
    }

    public void deleteCommutator(Integer id) {
        sendAll(Map.of("id", id), "acp", "commutator", "delete");
    }

    public void updateCommutatorUpdatePool(Set<AcpClient.RemoteUpdatingCommutatorItem> commutators) {
        sendAll(commutators, "acp", "commutator", "remote-update-pool");
    }

    public void createBaseCommutator(SwitchBaseInfo commutator) {
        sendAll(commutator, "acp", "commutator", "base", "create");
    }

    public void updateBaseCommutator(SwitchBaseInfo commutator) {
        sendAll(commutator, "acp", "commutator", "base", "update");
    }

    public void deleteBaseCommutator(Integer id) {
        sendAll(Map.of("id", id), "acp", "commutator", "base", "delete");
    }

    public void updateIncomingTaskCounter(String login, WireframeTaskCounter counter){
        sendToUser(login, counter, "task", "count", "change");
    }

    public void updateTaskCounter(WireframeTaskCounter counter){
        sendAll(counter, "task", "count", "change");
    }

    public void updateTagTaskCounter(Map<Long,Map<Long,Long>> counter){
        sendAll(counter, "task", "tag", "count", "change");
    }

    public void updateIncomingTagTaskCounter(String login, Map<Long,Map<Long,Long>> counter){
        sendToUser(login, counter, "task", "tag", "count", "change");
    }

    public void updateFilesDirectory(Long id){
        sendAll(id, "files", "directory", "update");
    }

    public void afterWorkAppend(WorkLog workLog){
        sendToUser(workLog.getCreator().getLogin(), workLog, "after-work", "append");
    }

    public void afterWorkRemoved(Long id, String employeeLogin){
        sendToUser(employeeLogin, id, "after-work", "remove");
    }

    public void createTypeOfContract(TypesOfContracts typesOfContracts) {
        sendAll(typesOfContracts, "contract", "type", "create");
    }

    public void updateTypeOfContract(TypesOfContracts typesOfContracts) {
        sendAll(typesOfContracts, "contract", "type", "update");
    }

    public void deleteTypeOfContract(TypesOfContracts typesOfContracts) {
        sendAll(typesOfContracts, "contract", "type", "delete");
    }

    public void updatingMarkedContracts() {
        sendAll(true, "contract", "marked", "update");
    }

    public void sendNewOntStatusChangeEvents(List<OntStatusChangeEvent> events) {
        sendAll(events, "pon", "events", "new", "ont", "status-change");
    }

    public void sendNewWorkerInQueue(Worker worker) {
        sendAll(worker, "pon", "worker", "new");
    }

    public void sendSpentWorkerInQueue(Worker worker) {
        sendAll(worker, "pon", "worker", "spent");
    }

    public void sendUpdatedOnt(OpticalNetworkTerminal ont) {
        sendAll(ont, "pon", "ont", "update");
    }

    public void sendUpdateAutoTariff(){
        sendAll(true, "auto-tariff", "update");
    }

    public void sendSchemeChange(PonSchemeChangeEvent event) {
        sendAll(event, "pon", "scheme", "change");
    }

    public void updateSensor(TemperatureSensorsDispatcher.SensorUpdateEvent event) {
        sendAll(event, "sensor", "event");
    }

    public void updateTlgUserTariff(UpdateCarrier<UserTariff> updateCarrier) {
        sendAll(updateCarrier, "telegram-user-tariff");
    }

    public void updateTlgUserRequest(UpdateCarrier<UserRequest> updateCarrier) {
        sendAll(updateCarrier, "telegram-user-request");
    }

    public void updateUserReview(UpdateCarrier<Review> updateCarrier) {
        sendAll(updateCarrier, "user-review");
    }
}
