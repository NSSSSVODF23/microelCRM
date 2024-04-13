package com.microel.trackerbackend.parsers.commutator.pon;

import com.microel.tdo.pon.MacTableEntry;
import com.microel.tdo.pon.OpticalLineTerminal;
import com.microel.trackerbackend.services.external.acp.AcpClient;
import com.microel.trackerbackend.services.external.pon.PonextenderClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class OltCollectorService {

    private final PonextenderClient ponextenderClient;
    private final OltCollectorFactory oltCollectorFactory;
    private final AcpClient acpClient;

    public OltCollectorService(PonextenderClient ponextenderClient, OltCollectorFactory oltCollectorFactory, AcpClient acpClient) {
        this.ponextenderClient = ponextenderClient;
        this.oltCollectorFactory = oltCollectorFactory;
        this.acpClient = acpClient;
    }

    @Scheduled(cron = "0 0 */3 * * *")
    public void collectMacTable(){
        Set<MacTableEntry> macTable = ConcurrentHashMap.newKeySet();
        List<OpticalLineTerminal> oltList = ponextenderClient.getOltList();
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (OpticalLineTerminal olt : oltList) {
            executorService.execute(()->{
                try {
                    OltTelnetCollector oltCollector = oltCollectorFactory.getOltCollector(olt.getModel(), olt.getIp());
                    oltCollector.auth();
                    macTable.addAll(oltCollector.getOntMacTable());
                    oltCollector.close();
                }catch (Exception ignore){}
            });
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Map<String, String> loginsByMacs = acpClient.getLoginsByMacs(macTable.stream().map(MacTableEntry::getMac).toList());
        macTable.forEach(macTableEntry -> {
            String login = loginsByMacs.get(macTableEntry.getMac());
            macTableEntry.setUserLogin(login);
        });
        ponextenderClient.signLogins(new ArrayList<>(macTable));
    }
}
