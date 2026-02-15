package com.devops.greenkube.watcher;

import com.devops.greenkube.service.LeaderElectionService;
import com.devops.greenkube.service.WasteAnalyzer;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.util.CallGeneratorParams;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Slf4j
@Component
@Data
@RequiredArgsConstructor
public class DeploymentWatcher {
    private final LeaderElectionService leaderService;
    private final ApiClient apiClient;
    private final AppsV1Api appsV1Api;
    private final WasteAnalyzer wasteAnalyzer;

    @PostConstruct
    public void start(){
        log.info("Starting Watcher");
        new Thread(() -> {
            try{
                log.info("connecting to kubernetes api");
                SharedInformerFactory factory = new SharedInformerFactory(apiClient);
                SharedIndexInformer<V1Deployment> informer = factory.sharedIndexInformerFor(
                        (CallGeneratorParams params) -> {
                            return appsV1Api.listDeploymentForAllNamespaces()
                                    .resourceVersion(params.resourceVersion)
                                    .timeoutSeconds(params.timeoutSeconds)
                                    .watch(params.watch)
                                    .buildCall(null);
                        },
                        V1Deployment.class, V1DeploymentList.class, 60 * 1000L);
                informer.addEventHandler(new ResourceEventHandler<V1Deployment>() {
                    @Override
                    public void onAdd(V1Deployment obj) {log.info("Scanning: {}/{}", obj.getMetadata().getNamespace(), obj.getMetadata().getName());
                        if(!leaderService.isCurrentLeader()) return ;
                        wasteAnalyzer.enqueueForAnalysis(obj);}

                    @Override
                    public void onUpdate(V1Deployment oldObj, V1Deployment newObj) {
                        Long oldGen = oldObj.getMetadata().getGeneration();
                        Long newGen = newObj.getMetadata().getGeneration();
                        if(oldGen != null && newGen != null && !oldGen.equals(newGen)){
                            System.out.println("[Watcher} True configuration change detected for: " + newObj.getMetadata().getName());
                        }
                        if(!oldObj.getMetadata().getResourceVersion().equals(newObj.getMetadata().getResourceVersion())){
                            log.info("Changed: {}/{}", newObj.getMetadata().getNamespace(), newObj.getMetadata().getName());
                        }
                        if(!leaderService.isCurrentLeader())return;
                        wasteAnalyzer.enqueueForAnalysis(newObj);
                    }

                    @Override
                    public void onDelete(V1Deployment obj, boolean deletedFinalStateUnknown) {}

                });

                System.out.println("Green Kube Watcher Started");
                factory.startAllRegisteredInformers();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }).start();
    }
}
