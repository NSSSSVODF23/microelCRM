package com.microel.trackerbackend.services.api.controllers;

import com.microel.trackerbackend.controllers.configuration.entity.BillingConf;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.services.external.billing.ApiBillingController;
import com.microel.trackerbackend.services.external.billing.BillingPayType;
import com.microel.trackerbackend.services.external.billing.directaccess.bases.Base1783;
import com.microel.trackerbackend.services.external.billing.directaccess.bases.Base1785;
import com.microel.trackerbackend.services.external.billing.directaccess.bases.Base781;
import com.microel.trackerbackend.storage.dispatchers.EmployeeDispatcher;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.templating.WireframeFieldType;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.repositories.ModelItemRepository;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@Slf4j
@RequestMapping("api/private/billing")
public class BillingRequestController {

    private final ApiBillingController apiBillingController;
    private final EmployeeDispatcher employeeDispatcher;
    private final ModelItemRepository modelItemRepository;
    private final StompController stompController;

    public BillingRequestController(ApiBillingController apiBillingController, EmployeeDispatcher employeeDispatcher, ModelItemRepository modelItemRepository, StompController stompController) {
        this.apiBillingController = apiBillingController;
        this.employeeDispatcher = employeeDispatcher;
        this.modelItemRepository = modelItemRepository;
        this.stompController = stompController;
    }

    @GetMapping("users/by-login")
    public ResponseEntity<List<ApiBillingController.UserItemData>> getBillingByLogin(@RequestParam String login, @RequestParam Boolean isActive) {
        return ResponseEntity.ok(apiBillingController.getUsersByLogin(login, isActive));
    }

    @GetMapping("users/by-fio")
    public ResponseEntity<List<ApiBillingController.UserItemData>> getBillingByFio(@RequestParam String query, @RequestParam Boolean isActive) {
        return ResponseEntity.ok(apiBillingController.getUsersByFio(query, isActive));
    }

    @GetMapping("users/by-address")
    public ResponseEntity<List<ApiBillingController.UserItemData>> getBillingByAddress(@RequestParam String address, @RequestParam Boolean isActive) {
        return ResponseEntity.ok(apiBillingController.getUsersByAddress(address, isActive));
    }

    @GetMapping("users/search")
    public ResponseEntity<List<ApiBillingController.UserItemData>> searchBillingUsers(@RequestParam String query, @RequestParam Boolean isActive) {
        return ResponseEntity.ok(apiBillingController.searchUsers(query, isActive));
    }

    @GetMapping("user/{login}")
    public ResponseEntity<ApiBillingController.TotalUserInfo> getBillingUserInfo(@PathVariable String login) {
        if(login.equals("test"))
            return ResponseEntity.ok(apiBillingController.testUser());
        return ResponseEntity.ok(apiBillingController.getUserInfo(login));
    }

    @GetMapping("user/{login}/events")
    public ResponseEntity<ApiBillingController.UserEvents> getBillingUserEvents(@PathVariable String login) {
        return ResponseEntity.ok(apiBillingController.getUserEvents(login));
    }

    @PostMapping("user/{login}/update-balance")
    public ResponseEntity<Void> updateBalance(@PathVariable String login, @RequestBody ApiBillingController.UpdateBalanceForm form, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        form.validate();
        if(form.getPayType() == BillingPayType.BANK){
            Base781 base = ApiBillingController.createBase781Session(employee);
            base.login();

            base.makeBankPayment(login, form.getSum(), form.getComment());

            base.logout();

            apiBillingController.getUpdatedUserAndPushUpdate(login);
            return ResponseEntity.ok().build();
        }
        apiBillingController.updateBalance(login, form.getSum(), form.getPayType(), form.getComment());
        return ResponseEntity.ok().build();
    }

    @PostMapping("user/{login}/deferred-payment")
    public ResponseEntity<Void> setDeferredPayment(@PathVariable String login) {
        apiBillingController.deferredPayment(login);
        return ResponseEntity.ok().build();
    }

    @PostMapping("user/{login}/start-service")
    public ResponseEntity<Void> startService(@PathVariable String login) {
        apiBillingController.startUserService(login);
        return ResponseEntity.ok().build();
    }

    @PostMapping("user/{login}/stop-service")
    public ResponseEntity<Void> stopService(@PathVariable String login) {
        apiBillingController.stopUserService(login);
        return ResponseEntity.ok().build();
    }

    @PostMapping("user/create")
    @Transactional
    public ResponseEntity<String> createUser(@RequestBody LoginFieldInfo loginFieldInfo, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);

        Base1785 base = ApiBillingController.createBase1785Session(employee);

        List<ModelItem> modelItems = modelItemRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("modelItemId"), loginFieldInfo.getModelItemId()));
            predicates.add(cb.equal(root.get("wireframeFieldType"), WireframeFieldType.LOGIN));
            return cb.and(predicates.toArray(Predicate[]::new));
        });

        if (modelItems.isEmpty()) throw new ResponseException("Поле логина не найдено в базе данных");

        ModelItem loginField = modelItems.get(0);

        List<ModelItem> taskFields = loginField.getTask().getFields();

        ModelItem addressField = taskFields.stream()
                .filter(
                        modelItem -> modelItem.getWireframeFieldType() == WireframeFieldType.ADDRESS
                                && modelItem.getName().equals("Адрес")
                                && modelItem.getAddressData() != null
                ).findFirst()
                .orElseThrow(() -> new ResponseException("В задаче не найден адрес"));

        ModelItem fioField = taskFields.stream()
                .filter(
                        modelItem -> modelItem.getWireframeFieldType() == WireframeFieldType.SMALL_TEXT &&
                                modelItem.getName().equals("ФИО") &&
                                modelItem.getStringData() != null &&
                                !modelItem.getStringData().isBlank()
                ).findFirst()
                .orElseThrow(() -> new ResponseException("В задаче не найдено ФИО"));

        ModelItem phoneField = taskFields.stream()
                .filter(
                        modelItem -> modelItem.getWireframeFieldType() == WireframeFieldType.PHONE_ARRAY &&
                                modelItem.getPhoneData() != null &&
                                !modelItem.getPhoneData().isEmpty()
                ).findFirst()
                .orElseThrow(() -> new ResponseException("В задаче не найден телефон"));

        ModelItem passwordField = taskFields.stream()
                .filter(
                        modelItem -> modelItem.getWireframeFieldType() == WireframeFieldType.SMALL_TEXT &&
                                modelItem.getName().equals("Пароль")
                ).findFirst().orElse(null);

        Base1785.CreateUserForm createUserForm = Base1785.CreateUserForm.of(
                addressField.getAddressData(),
                fioField.getStringData(),
                phoneField.getPhoneData().values().iterator().next(),
                loginFieldInfo.isOrg ? Base1785.UserType.ORG : Base1785.UserType.PHY
        );

        base.login();

        String createdUserLogin = base.createLogin(createUserForm);

        base.logout();

        loginField.setStringData(createdUserLogin);
        if(passwordField != null){
            passwordField.setStringData(createUserForm.getPreparePhone());
            modelItemRepository.save(passwordField);
        }

        modelItemRepository.save(loginField);

        stompController.updateTask(loginField.getTask());

        return ResponseEntity.ok(createdUserLogin);
    }

    @GetMapping("user/{login}/tariffs")
    public ResponseEntity<List<Base1785.UserTariff>> getUserTariffs(@PathVariable String login, HttpServletRequest request){
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);

        Base1785 base = ApiBillingController.createBase1785Session(employee);
        base.login();
        List<Base1785.UserTariff> tariffList = base.getTariffList(login);
        base.logout();

        return ResponseEntity.ok(tariffList);
    }

    @GetMapping("user/{login}/services")
    public ResponseEntity<List<Base1785.UserTariff>> getUserServices(@PathVariable String login, HttpServletRequest request){
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);

        Base1785 base = ApiBillingController.createBase1785Session(employee);
        base.login();
        List<Base1785.UserTariff> serviceList = base.getServiceList(login);
        base.logout();

        return ResponseEntity.ok(serviceList);
    }

    @PatchMapping("user/{login}/service/{id}/append")
    public ResponseEntity<Void> appendService(@PathVariable String login, @PathVariable Integer id, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);

        Base1785 base = ApiBillingController.createBase1785Session(employee);
        base.login();

        base.appendService(login, id);

        base.logout();

        apiBillingController.getUpdatedUserAndPushUpdate(login);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("user/{login}/service/{name}/remove")
    public ResponseEntity<Void> removeService(@PathVariable String login, @PathVariable String name, HttpServletRequest request){
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);

        Base1785 base = ApiBillingController.createBase1785Session(employee);
        base.login();

        base.removeService(login, name);

        base.logout();

        apiBillingController.getUpdatedUserAndPushUpdate(login);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("user/{login}/tariff/{id}")
    public ResponseEntity<Void> changeTariff(@PathVariable String login, @PathVariable Integer id, HttpServletRequest request){
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);

        Base1785 base = ApiBillingController.createBase1785Session(employee);
        base.login();

        base.changeTariff(login, id);

        base.logout();

        apiBillingController.getUpdatedUserAndPushUpdate(login);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("user/{login}/balance-reset")
    public ResponseEntity<Void> balanceReset(@PathVariable String login, @RequestBody String comment, HttpServletRequest request){
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);

        Base1785 base = ApiBillingController.createBase1785Session(employee);
        base.login();

        base.balanceReset(login, comment);

        base.logout();

        apiBillingController.getUpdatedUserAndPushUpdate(login);

        return ResponseEntity.ok().build();
    }

    @GetMapping("user/{login}/is-enable")
    public ResponseEntity<Boolean> isLoginEnable(@PathVariable String login, HttpServletRequest request){
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);

        Base1785 base = ApiBillingController.createBase1785Session(employee);
        base.login();

        Boolean isEnable = base.isLoginEnable(login);

        base.logout();

        return ResponseEntity.ok(isEnable);
    }

    @PatchMapping("user/{login}/enable-login")
    public ResponseEntity<Void> enableLogin(@PathVariable String login, HttpServletRequest request){
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);

        Base1785 base = ApiBillingController.createBase1785Session(employee);
        base.login();

        base.unfreeze(login);
        base.enableLogin(login);

        base.logout();

        apiBillingController.getUpdatedUserAndPushUpdate(login);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("user/{login}/change/address")
    public ResponseEntity<Void> changeAddress(@PathVariable String login, @RequestBody String address, HttpServletRequest request){
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);

        Base1783 base = ApiBillingController.createBase1783Session(employee);
        base.login();

        base.changeAddress(login, address);

        base.logout();

        apiBillingController.getUpdatedUserAndPushUpdate(login);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("user/{login}/change/full-name")
    public ResponseEntity<Void> changeFullName(@PathVariable String login, @RequestBody String fullName, HttpServletRequest request){
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);

        Base1783 base = ApiBillingController.createBase1783Session(employee);
        base.login();

        base.changeFullName(login, fullName);

        base.logout();

        apiBillingController.getUpdatedUserAndPushUpdate(login);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("user/{login}/change/phone")
    public ResponseEntity<Void> changePhone(@PathVariable String login, @RequestBody String phone, HttpServletRequest request){
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);

        Base1783 base = ApiBillingController.createBase1783Session(employee);
        base.login();

        base.changePhone(login, phone);

        base.logout();

        apiBillingController.getUpdatedUserAndPushUpdate(login);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("user/{login}/change/comment")
    public ResponseEntity<Void> changeComment(@PathVariable String login, @RequestBody String comment, HttpServletRequest request){
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);

        Base1783 base = ApiBillingController.createBase1783Session(employee);
        base.login();

        base.changeComment(login, comment);

        base.logout();

        apiBillingController.getUpdatedUserAndPushUpdate(login);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("user/{login}/edit")
    public ResponseEntity<Void> editUser(@PathVariable String login, @RequestBody EditUserForm form, HttpServletRequest request){
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);

        Base1783 base = ApiBillingController.createBase1783Session(employee);
        base.login();

        base.userEdit(login, form);

        base.logout();

        apiBillingController.getUpdatedUserAndPushUpdate(login);

        return ResponseEntity.ok().build();
    }

    @PostMapping("user/{login}/make-payment")
    public ResponseEntity<Void> makePayment(@PathVariable String login, @RequestBody Base781.PaymentForm form, HttpServletRequest request){
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);

        Base781 base = ApiBillingController.createBase781Session(employee);
        base.login();

        base.makePayment(login, form);

        base.logout();

        apiBillingController.getUpdatedUserAndPushUpdate(login);

        return ResponseEntity.ok().build();
    }

    @PostMapping("user/{login}/make-recalculation")
    public ResponseEntity<Void> makeRecalculation(@PathVariable String login, @RequestBody Base781.RecalculationForm form, HttpServletRequest request){
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);

        Base781 base = ApiBillingController.createBase781Session(employee);
        base.login();

        base.makeRecalculation(login, form);

        base.logout();

        apiBillingController.getUpdatedUserAndPushUpdate(login);

        return ResponseEntity.ok().build();
    }

    @GetMapping("suggestions/user")
    public ResponseEntity<List<ApiBillingController.UserItemData>> getUserSuggestions(@RequestParam String query) {
        return ResponseEntity.ok(apiBillingController.getUserSuggestions(query));
    }

    @PostMapping("counting-lives")
    public ResponseEntity<Map<String, String>> getCountingLives(@RequestBody ApiBillingController.CountingLivesForm form) {
        return ResponseEntity.ok(Map.of("result", apiBillingController.getCalculateCountingLives(form)));
    }

    @GetMapping("configuration")
    public ResponseEntity<BillingConf> getBillingConfiguration() {
        return ResponseEntity.ok(apiBillingController.getConfiguration());
    }

    @PostMapping("configuration")
    public ResponseEntity<Void> updateBillingConfiguration(@RequestBody BillingConf conf) {
        try {
            apiBillingController.setConfiguration(conf);
            return ResponseEntity.ok().build();
        } catch (MalformedURLException e) {
            throw new IllegalFields("Не верный Url адрес");
        }
    }

    @Getter
    @Setter
    public static class LoginFieldInfo {
        private Long modelItemId;
        private Boolean isOrg;
    }

    @Data
    public static class EditUserForm {
        private String address;
        private String fullName;
        private String phone;
        private String comment;
    }
}
