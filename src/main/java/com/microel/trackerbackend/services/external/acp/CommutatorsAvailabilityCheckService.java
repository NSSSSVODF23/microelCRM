package com.microel.trackerbackend.services.external.acp;

import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.services.external.acp.types.Switch;
import com.microel.trackerbackend.storage.dispatchers.AcpCommutatorDispatcher;
import com.microel.trackerbackend.storage.entities.acp.AcpCommutator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static java.lang.Thread.sleep;

@Service
public class CommutatorsAvailabilityCheckService {
    private final AcpCommutatorDispatcher acpCommutatorDispatcher;
    private final AcpClient acpClient;
    private final StompController stompController;
    private List<Switch> cachedCommutators = new ArrayList<>();

    public CommutatorsAvailabilityCheckService(AcpCommutatorDispatcher acpCommutatorDispatcher, AcpClient acpClient, StompController stompController) {
        this.acpCommutatorDispatcher = acpCommutatorDispatcher;
        this.acpClient = acpClient;
        this.stompController = stompController;

        synchronizeBetweenBases();
        getAllCommutators();
    }

    @Scheduled(cron = "0 0 */1 * * *")
    public void synchronizeBetweenBases() {
        cachedCommutators = acpClient.getAllCommutators();
        this.acpCommutatorDispatcher.synchronize(cachedCommutators);
    }

    @Scheduled(fixedRate = 10000)
    public void getAllCommutators() {
        ThreadFactory threadFactory = thread -> Executors.defaultThreadFactory().newThread(thread);
        ExecutorService executorService = Executors.newCachedThreadPool(threadFactory);
        for (Switch comm : cachedCommutators) {
            executorService.submit(() -> {
                AcpCommutator updatedCommutator = null;
                try {
                    boolean isReach = false;
                    for (int i = 0; i < 5; i++) {
                        boolean reachable = InetAddress.getByName(comm.getIpaddr()).isReachable(1000);
                        if (reachable) {
                            updatedCommutator = this.acpCommutatorDispatcher.updateStatus(comm.getId(), true);
                            isReach = true;
                            break;
                        }
                    }
                    if (!isReach)
                        updatedCommutator = this.acpCommutatorDispatcher.updateStatus(comm.getId(), false);
                } catch (IOException e) {
                    updatedCommutator = this.acpCommutatorDispatcher.updateStatus(comm.getId(), false);
                }
                if (updatedCommutator != null) {
                    stompController.updateAcpCommutator(updatedCommutator);
                    System.out.println(updatedCommutator);
                }
            });
            try {
                sleep(5);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
