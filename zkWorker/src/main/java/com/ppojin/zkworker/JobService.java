package com.ppojin.zkworker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class JobService {
    private final Map<String, Worker> jobMap;

    public JobService() {
        this.jobMap = new ConcurrentHashMap<>();
    }

    public Set<String> keys(){
        return jobMap.keySet();
    }

    public void add(Job job) throws jobExistsException {
        if(jobMap.containsKey(job.getId())){
            throw new jobExistsException(job.getId());
        }

        Worker worker = new Worker(job.getMessage());
        worker.start();

        jobMap.put(job.getId(), worker);
        while(!Objects.equals(jobMap.get(job.getId()).getState(), Worker.RUNNING)){
            try {
                log.info(jobMap.get(job.getId()).getState());;
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.error("InterruptedException: ", e);
            }
        }

        log.debug("job: {}", jobMap);
    }

    public void stop(String id){
        jobMap.get(id).stop();
        log.debug("job: {}", jobMap);
    }

    public void start(String id){
        jobMap.get(id).start();
        log.debug("job: {}", jobMap);
    }

    public void delete(String id) {
        jobMap.get(id).stop();
        while(!Objects.equals(jobMap.get(id).getState(), Worker.STOP)){
            try {
                log.info(jobMap.get(id).getState());
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.error("InterruptedException: ", e);
            }
        }
        jobMap.get(id).close();
        jobMap.remove(id);
        log.info("job: {}", jobMap);
    }

    public class jobExistsException extends Exception {
        public jobExistsException(String jobId) {
            super("Job({}) is already running!");
        }
    }
}
