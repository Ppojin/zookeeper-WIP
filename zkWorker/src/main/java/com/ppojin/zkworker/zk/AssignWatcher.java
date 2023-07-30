package com.ppojin.zkworker.zk;

import com.ppojin.zkworker.Job;
import com.ppojin.zkworker.JobService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class AssignWatcher implements org.apache.zookeeper.Watcher, AsyncCallback.ChildrenCallback {
    private static ZooKeeper zk;
    private static String workerId;
    private final static CountDownLatch connectedSignal = new CountDownLatch(100);
    private final JobService jobService;

    public AssignWatcher(
            @Value("${zookeeper.host_names}") String hostNames,
            JobService jobService
    ) {
        this.jobService = jobService;
        
        if(zk == null){
            try {
                zk = new ZooKeeper(hostNames, 5000, this);
                log.info("zookeeper session created");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void initializeSession() {
        try {
            if(zk.exists(Consts.APP_PATH, false) == null){
                zk.create(Consts.APP_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }

            if(zk.exists(Consts.ASSIGN_PATH, false) == null){
                zk.create(Consts.ASSIGN_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }

            if(zk.exists(Consts.NODE_PATH, false) == null){
                zk.create(Consts.NODE_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }

            final String worker;
            worker = zk.create(
                    Consts.NODE_PATH + "/" + Consts.NODE_NAME,
                    new byte[0],
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_SEQUENTIAL
            );
            log.info("worknode '{}' created", worker);
            List<String> split = List.of(worker.split("/"));
            workerId = split.get(split.size() - 1);
            zk.create(
                    Consts.ASSIGN_PATH + "/" + workerId,
                    new byte[0],
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT
            );
        } catch (KeeperException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.NodeChildrenChanged) {
            String workerAssignPath = Consts.ASSIGN_PATH + "/" + workerId;
            if (workerAssignPath.equals(event.getPath())) {
                zk.getChildren(workerAssignPath, true, this, "SYNCWORKS");
            }
        }
        
        if (event.getState() == org.apache.zookeeper.Watcher.Event.KeeperState.SyncConnected && workerId == null) {
            connectedSignal.countDown();
            initializeSession();
        }

        log.info("## process: (" +
                "path:" + event.getPath() + ", " +
                "eventType:" + event.getType().name() + ", " +
                "eventState:" + event.getState().name() +
                ")");
    }

    @Override
    public void processResult(int rc, String path, Object ctx, List<String> children) {
        log.info("{}: ({})", path, children);
        log.info("{}: ({})", path, jobService.keys());

        if ("SYNCWORKS".equals(ctx.toString())){
            startJobs(children);
            stopJobs(children);
        }
    }

    private void stopJobs(List<String> children) {
        List<String> stopping = jobService.keys().stream()
                .filter(jobId -> !children.contains(jobId))
                .toList();
        stopping.forEach(jobService::delete);
        log.info("stopped: {}", stopping);
    }

    private void startJobs(List<String> children) {
        List<String> starting = children.stream()
                .filter(child -> !jobService.keys().contains(child))
                .toList();
        starting.forEach(zNode -> {
            try {
                byte[] data = zk.getData(
                        Consts.ASSIGN_PATH + "/" + workerId + "/" + zNode, false, null
                );
                Job job = Job.builder()
                        .id(zNode)
                        .message(new String(data, StandardCharsets.UTF_8))
                        .build();
                jobService.add(job);
            } catch (KeeperException | InterruptedException | JobService.jobExistsException e) {
                e.printStackTrace();
            }
        });
        log.info("started job: {}", starting);
    }
}
