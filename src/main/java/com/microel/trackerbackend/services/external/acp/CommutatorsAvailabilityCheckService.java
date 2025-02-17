package com.microel.trackerbackend.services.external.acp;

import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.services.external.acp.types.Switch;
import com.microel.trackerbackend.services.external.acp.types.SwitchModel;
import com.microel.trackerbackend.storage.dispatchers.AcpCommutatorDispatcher;
import com.microel.trackerbackend.storage.entities.acp.commutator.AcpCommutator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CommutatorsAvailabilityCheckService {
    private final AcpCommutatorDispatcher acpCommutatorDispatcher;
    private final AcpClient acpClient;
    private final StompController stompController;
    private Map<Integer, String> cachedModels = new HashMap<>();
    private List<Switch> cachedCommutators = new ArrayList<>();

    public CommutatorsAvailabilityCheckService(AcpCommutatorDispatcher acpCommutatorDispatcher, AcpClient acpClient, StompController stompController) {
        this.acpCommutatorDispatcher = acpCommutatorDispatcher;
        this.acpClient = acpClient;
        this.stompController = stompController;

        synchronizeBetweenBases();
    }

    @Async
    @Transactional
    @Scheduled(cron = "0 30 * * * *")
    public void synchronizeBetweenBases() {
        try {
            cachedModels = acpClient.getCommutatorModels(null).stream().collect(Collectors.toMap(SwitchModel::getId, SwitchModel::getName));
            cachedCommutators = acpClient.getAllCommutators();
            this.acpCommutatorDispatcher.synchronize(cachedCommutators);
        } catch (ResponseException e) {
            System.out.println("Нет соединения с AcpFlexConnector модулем. " + e.getMessage());
        }
    }

    @Async
    @Transactional
//    @Scheduled(fixedDelay = 40000L, initialDelay = 10000L)
    public void getAllCommutators() {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        for (Switch comm : cachedCommutators) {
            executorService.execute(() -> {
                try {
                    Thread.currentThread().setName("AVAILABILITY CHECK");
                    AcpCommutator updatedCommutator = null;
                    try {
                        boolean isReach = false;
                        for (int i = 0; i < 5; i++) {
                            boolean reachable = InetAddress.getByName(comm.getIpaddr()).isReachable(500);
                            if (reachable) {
                                updatedCommutator = this.acpCommutatorDispatcher.updateStatus(comm.getId(), true);
                                isReach = true;
                                break;
                            }
                        }
                        if (!isReach) {
                            updatedCommutator = this.acpCommutatorDispatcher.updateStatus(comm.getId(), false);
                        }
                    } catch (IOException e) {
                        updatedCommutator = this.acpCommutatorDispatcher.updateStatus(comm.getId(), false);
                    }
                    if (updatedCommutator != null) {
                        comm.setAdditionalInfo(updatedCommutator); // TODO Придумать систему обновления коммутаторов в Фронте
//                        stompController.updateAcpCommutator(updatedCommutator);
//                        stompController.updateBaseCommutator(SwitchBaseInfo.from(comm, cachedModels.get(comm.getSwmodelId().intValue())));
                    }
                } catch (Exception e) {
                    System.out.println("Ошибка в потоке обновления статуса: " + e.getMessage());
                }
            });
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
        }
    }
}
