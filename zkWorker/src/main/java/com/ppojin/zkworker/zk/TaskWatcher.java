package com.ppojin.zkworker.zk;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Component
public class TaskWatcher implements Watcher{
    @Getter
    private final ZooKeeper zk;
    private final String workerId;

    final static CountDownLatch connectedSignal = new CountDownLatch(100);

    @Getter
    private final String workerAssignPath;

    private final TaskCallback taskCallback;

    public TaskWatcher(
            @Value("${zookeeper.host_names}") String hostNames,
            TaskCallback taskCallback
    ) {
        this.taskCallback = taskCallback;

        try {
            this.zk = new ZooKeeper(hostNames, 5000, this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final String worker;
        try {
            worker = zk.create(Consts.NODE_PATH + "/" + Consts.NODE_NAME, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            log.info("worknode '{}' created", worker);
        } catch (KeeperException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<String> split = List.of(worker.split("/"));
        this.workerId = split.get(split.size() - 1);
        this.workerAssignPath = Consts.ASSIGN_PATH + "/" + this.workerId;

        try {
            zk.create(workerAssignPath, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getState() == Watcher.Event.KeeperState.SyncConnected){
            if (Objects.isNull(this.workerId)) {
                connectedSignal.countDown();
                try {
                    zk.getChildren(Consts.ASSIGN_PATH, true);
                } catch (KeeperException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else if (event.getType() == Event.EventType.NodeChildrenChanged) {
                zk.getChildren(workerAssignPath, true, taskCallback, this);
            }
        }

        String path = event.getPath();
        String eventType = event.getType().name();
        String eventState = event.getState().name();
        System.out.println("## process: (" +
                "path:" + path + ", " +
                "eventType:" + eventType + ", " +
                "eventState:" + eventState +
        ")");
    }
}
