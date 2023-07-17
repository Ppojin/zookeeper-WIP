package com.ppojin.zkworker.zk;

import com.ppojin.zkworker.Job;
import com.ppojin.zkworker.JobService;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class TaskCallback implements AsyncCallback.ChildrenCallback {
    private final JobService jobService;

    public TaskCallback(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public void processResult(int rc, String path, Object ctx, List<String> children) {
        log.info("{}: ({})", path, children);
        log.info("{}: ({})", path, jobService.keys());

        if (Objects.isNull(ctx)) {
            return;
        }
        if (ctx instanceof TaskWatcher && !Objects.isNull(children)) {
            List<String> started = children.stream()
                    .filter(child -> !jobService.keys().contains(child))
                    .map(child -> {
                        try {
                            byte[] data = ((TaskWatcher) ctx).getZk().getData(
                                    ((TaskWatcher) ctx).getWorkerAssignPath() + "/" + child, false, null
                            );
                            Job job = Job.builder()
                                    .id(child)
                                    .message(new String(data, StandardCharsets.UTF_8))
                                    .build();
                            jobService.add(job);
                        } catch (KeeperException | InterruptedException | JobService.jobExistsException e) {
                            e.printStackTrace();
                        }
                        return child;
                    })
                    .toList();
            log.info("started job: {}", started);

            jobService.keys().stream()
                    .filter(jobId -> !children.contains(jobId))
                    .forEach(jobId -> {
                        jobService.delete(jobId);
                        log.info("stopped: {}", jobId);
                    });
        }
    }
}
