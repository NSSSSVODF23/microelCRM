package com.microel.trackerbackend.services.api.controllers;

import com.microel.tdo.dynamictable.TablePaging;
import com.microel.trackerbackend.controllers.configuration.entity.AcpConf;
import com.microel.trackerbackend.services.external.acp.AcpClient;
import com.microel.trackerbackend.services.external.acp.AcpXmlRpcController;
import com.microel.trackerbackend.services.external.acp.types.*;
import com.microel.trackerbackend.services.external.billing.ApiBillingController;
import com.microel.trackerbackend.storage.dispatchers.HouseDispatcher;
import com.microel.trackerbackend.storage.entities.acp.AcpHouse;
import com.microel.trackerbackend.storage.entities.acp.commutator.FdbItem;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.address.House;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@Slf4j
@RequestMapping("api/private/acp")
public class AcpRequestController {

    private final AcpClient acpClient;
    private final AcpXmlRpcController  acpXmlRpcController;
    private final ApiBillingController apiBillingController;
    private final HouseDispatcher houseDispatcher;

    public AcpRequestController(AcpClient acpClient, AcpXmlRpcController acpXmlRpcController, ApiBillingController apiBillingController, HouseDispatcher houseDispatcher) {
        this.acpClient = acpClient;
        this.acpXmlRpcController = acpXmlRpcController;
        this.apiBillingController = apiBillingController;
        this.houseDispatcher = houseDispatcher;
    }


    @GetMapping("dhcp/bindings")
    public ResponseEntity<List<DhcpBinding>> getDhcpBindings(@RequestParam String login) {
        return ResponseEntity.ok(acpClient.getBindingsByLogin(login));
    }

    @GetMapping("dhcp/binding/{login}/logs/{page}")
    public ResponseEntity<LogsRequest> getDhcpBindingsLogs(@PathVariable String login, @PathVariable Integer page) {
        return ResponseEntity.ok(acpClient.getLogsByLogin(login, page));
    }

    @GetMapping("vlan/{id}/dhcp/bindings/{page}")
    public ResponseEntity<Page<DhcpBinding>> getDhcpBindingsByVlan(@PathVariable Integer page, @PathVariable Integer id, @RequestParam String excludeLogin) {
        return ResponseEntity.ok(acpClient.getDhcpBindingsByVlan(page, id, excludeLogin));
    }

    @GetMapping("dhcp/bindings/{page}/last")
    public ResponseEntity<Page<DhcpBinding>> getLastBindings(@PathVariable Integer page,
                                                             @RequestParam @Nullable Short state,
                                                             @RequestParam @Nullable String macaddr,
                                                             @RequestParam @Nullable String login,
                                                             @RequestParam @Nullable String ip,
                                                             @RequestParam @Nullable Integer vlan,
                                                             @RequestParam @Nullable Integer buildingId,
                                                             @RequestParam @Nullable Integer commutator,
                                                             @RequestParam @Nullable Integer port) {
        if(commutator == null)
            return ResponseEntity.ok(acpClient.getLastBindings(page, state, macaddr, login, ip, vlan, buildingId, null));

        return ResponseEntity.ok(acpClient.getLastBindings(page, state, macaddr, login, ip, vlan, buildingId, commutator, port));
    }

    @GetMapping("dhcp/binding/{id}/ncl-history")
    public ResponseEntity<AcpClient.NCLHistoryWrapper> getNetworkConnectionLocation(@PathVariable Integer id) {
        return ResponseEntity.ok(acpClient.getNetworkConnectionLocationHistory(id));
    }

    @PostMapping("dhcp/binding/auth")
    public ResponseEntity<Void> authDhcpBinding(@RequestBody DhcpBinding.AuthForm form) {
        apiBillingController.getUserInfo(form.getLogin());
        acpClient.authDhcpBinding(form);
        return ResponseEntity.ok().build();
    }

    @GetMapping("buildings")
    public ResponseEntity<List<AcpHouse>> getBuildings(@Nullable @RequestParam String query) {
        return ResponseEntity.ok(acpClient.getHouses(query));
    }

    @GetMapping("commutators/{page}/page")
    public ResponseEntity<Page<SwitchBaseInfo>> getCommutators(@PathVariable Integer page,
                                                               @RequestParam @Nullable String name,
                                                               @RequestParam @Nullable String ip,
                                                               @RequestParam @Nullable Integer buildingId) {
        return ResponseEntity.ok(acpClient.getCommutators(page, name, ip, buildingId, 15));
    }

    @PostMapping("commutators/table")
    public ResponseEntity<Page<SwitchBaseInfo>> getCommutatorsTable(@RequestBody TablePaging paging){
        return ResponseEntity.ok(acpClient.getCommutatorsTable(paging));
    }

    @GetMapping("commutators/vlan/{vlan}")
    public ResponseEntity<List<Switch>> getCommutatorsByVlan(@PathVariable Integer vlan) {
        return ResponseEntity.ok(acpClient.getCommutatorsByVlan(vlan));
    }

    @GetMapping("commutators/search")
    public ResponseEntity<List<SwitchWithAddress>> searchCommutators(@RequestParam @Nullable String query) {
        return ResponseEntity.ok(acpClient.searchCommutators(query));
    }

    @GetMapping("commutator/{id}")
    public ResponseEntity<SwitchWithAddress> getCommutator(@PathVariable Integer id) {
        return ResponseEntity.ok(acpClient.getCommutator(id));
    }

    @GetMapping("commutator/{id}/editing-preset")
    public ResponseEntity<SwitchEditingPreset> getCommutatorEditingPreset(@PathVariable Integer id) {
        return ResponseEntity.ok(acpClient.getCommutatorEditingPreset(id));
    }

    @PostMapping("commutator/{id}/get-remote-update")
    public ResponseEntity<Void> getCommutatorRemoteUpdate(@PathVariable Integer id) {
        acpClient.getCommutatorRemoteUpdate(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("commutators/vlan/{vlan}/get-remote-update")
    public ResponseEntity<Void> getCommutatorsByVlanRemoteUpdate(@PathVariable Integer vlan) {
        acpClient.getCommutatorsByVlanRemoteUpdate(vlan);
        return ResponseEntity.ok().build();
    }

    @PostMapping("commutators/get-remote-update")
    public ResponseEntity<Void> getCommutatorsRemoteUpdate() {
        acpClient.getAllCommutatorsRemoteUpdate();
        return ResponseEntity.ok().build();
    }

    @GetMapping("commutator/port/{id}/fdb")
    public ResponseEntity<List<FdbItem>> getCommutatorFdb(@PathVariable Long id) {
        return ResponseEntity.ok(acpClient.getFdbByPort(id));
    }

    @PostMapping("commutator")
    public ResponseEntity<Void> createCommutator(@RequestBody Switch.Form form) {
        if (!form.isValid()) throw new IllegalFields("Неверно заполнена форма создания коммутатора");
        Switch createdCommutator = acpClient.createCommutator(form);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("commutator/{id}")
    public ResponseEntity<Void> updateCommutator(@PathVariable Integer id, @RequestBody Switch.Form form) {
        if (!form.isValid()) throw new IllegalFields("Неверно заполнена форма редактирования коммутатора");
        Switch updatedCommutator = acpClient.updateCommutator(id, form);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("commutator/{id}")
    public ResponseEntity<Void> deleteCommutator(@PathVariable Integer id) {
        acpClient.deleteCommutator(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("commutator/check-exist/name")
    public ResponseEntity<Boolean> checkCommutatorNameExist(@RequestParam String name) {
        return ResponseEntity.ok(acpClient.checkCommutatorNameExist(name));
    }

    @GetMapping("commutator/check-exist/ip")
    public ResponseEntity<Boolean> checkCommutatorIpExist(@RequestParam String ip) {
        return ResponseEntity.ok(acpClient.checkCommutatorIpExist(ip));
    }

    @GetMapping("commutator/models")
    public ResponseEntity<List<SwitchModel>> getCommutatorModels(@RequestParam @Nullable String query) {
        return ResponseEntity.ok(acpClient.getCommutatorModels(query));
    }

    @GetMapping("commutator/model/{id}")
    public ResponseEntity<SwitchModel> getCommutatorModel(@PathVariable Integer id) {
        return ResponseEntity.ok(acpClient.getCommutatorModel(id));
    }

    @GetMapping("building/{id}/address")
    public ResponseEntity<Address> getBuildingAddress(@PathVariable Integer id) {
        House houseByBind = houseDispatcher.getByAcpBindId(id);
        if (houseByBind == null) return ResponseEntity.ok(null);
        return ResponseEntity.ok(houseByBind.getAddress());
    }

    @GetMapping("topology")
    public ResponseEntity<List<AcpClient.TopologyStreet>> getTopology() {
        return ResponseEntity.ok(acpClient.getTopology());
    }

    @GetMapping("building/{id}")
    public ResponseEntity<AcpHouse> getBuilding(@PathVariable Integer id) {
        return ResponseEntity.ok(acpClient.getBuilding(id));
    }

    @GetMapping("building/{id}/commutators")
    public ResponseEntity<List<AcpClient.CommutatorListItem>> getCommutators(@PathVariable Integer id) {
        return ResponseEntity.ok(acpClient.getCommutatorsByBuildingId(id));
    }

    @PostMapping("building/{id}/bindings/{page}")
    public ResponseEntity<Page<DhcpBinding>> getBindings(@PathVariable Integer id, @PathVariable Integer page,
                                                         @RequestBody @Nullable AcpClient.BindingFilter filter) {
        return ResponseEntity.ok(acpClient.getBindingsByBuildingId(id, page, filter));
    }

    @PostMapping("user/{login}/bindings/{page}")
    public ResponseEntity<Page<DhcpBinding>> getBindingsByLogin(@PathVariable String login, @PathVariable Integer page,
                                                                @RequestBody @Nullable AcpClient.BindingFilter filter) {
        return ResponseEntity.ok(acpClient.getBindingsByLogin(login, page, filter));
    }

    @PostMapping("vlan/{vid}/bindings/{page}")
    public ResponseEntity<Page<DhcpBinding>> getBindingsByVlan(@PathVariable Integer vid, @PathVariable Integer page,
                                                               @RequestBody @Nullable AcpClient.BindingFilter filter) {
        return ResponseEntity.ok(acpClient.getBindingsByVlan(vid, page, filter));
    }

    @PostMapping("user/{login}/bindings-from-building/{page}")
    public ResponseEntity<Page<DhcpBinding>> getBindingsFromBuildingByLogin(@PathVariable String login, @PathVariable Integer page,
                                                                            @RequestBody @Nullable AcpClient.BindingFilter filter) {
        return ResponseEntity.ok(acpClient.getBindingsFromBuildingByLogin(login, page, filter));
    }

    @PostMapping("commutator/{id}/bindings/{page}")
    public ResponseEntity<Page<DhcpBinding>> getBindingsByCommutator(@PathVariable Long id, @PathVariable Integer page,
                                                                            @RequestBody @Nullable AcpClient.BindingFilter filter) {
        return ResponseEntity.ok(acpClient.getBindingsByCommutator(id, page, filter));
    }

    @PostMapping("bindings/table")
    public ResponseEntity<Page<DhcpBinding>> getBindingsTable(@RequestBody TablePaging paging) {
        return ResponseEntity.ok(acpClient.getBindingsTable(paging));
    }

    @GetMapping("user/{login}/active-binding")
    public ResponseEntity<DhcpBinding> getActiveBinding(@PathVariable String login) {
        return ResponseEntity.ok(acpClient.getActiveBinding(login));
    }

    @GetMapping("vlan/{vid}/building-id")
    public ResponseEntity<Integer> getBuildingIdByVlan(@PathVariable Integer vid) {
        return ResponseEntity.ok(acpClient.getBuildingIdByVlan(vid));
    }

    @GetMapping("user/{login}/brief-info")
    public ResponseEntity<AcpXmlRpcController.AcpUserBrief> getUserBriefInfo(@PathVariable String login) {
        return ResponseEntity.ok(acpXmlRpcController.getUserBrief(login));
    }

    @PostMapping("user/brief-info/bulk")
    public ResponseEntity<Map<String, AcpXmlRpcController.AcpUserBrief>> getBulkUserBriefInfo(@RequestBody Set<String> logins) {
        return ResponseEntity.ok(acpXmlRpcController.getBulkUserBrief(logins));
    }

    @GetMapping("configuration")
    public ResponseEntity<AcpConf> getAcpConfiguration() {
        return ResponseEntity.ok(acpClient.getConfiguration());
    }

    @PatchMapping("configuration")
    public ResponseEntity<Void> updateAcpConfiguration(@RequestBody AcpConf conf) {
        acpClient.setConfiguration(conf);
        return ResponseEntity.ok().build();
    }

}
