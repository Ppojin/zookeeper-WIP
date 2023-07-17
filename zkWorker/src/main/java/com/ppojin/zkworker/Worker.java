package com.ppojin.zkworker;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Worker implements Closeable {
    private final ExecutorService executor;

    @Setter
    private String message;

    public static final String STOP = "STOP";
    public static final String STOPPING = "STOPPING";
    public static final String RUNNING = "RUNNING";

    @Getter
    private String state;

    public Worker(String message) {
        this.executor = Executors.newSingleThreadExecutor();
        this.message = message;
        this.state = STOP;
    }

    public void start(){
        log.info("isTerminated: {}", executor.isTerminated());
        state = RUNNING;
        CompletableFuture
                .runAsync(this::work, executor)
                .thenRun(()->{
                    log.info("{} stopped", message);
                });
    }

    public void stop(){
        state = STOPPING;
        log.info("isTerminated: {}", executor.isTerminated());
    }

    @Override
    public void close(){
        executor.shutdown();
    }

    private void work() {
        long startTime = System.currentTimeMillis();
        log.info("STARTING: {}", message);
        while (Objects.equals(state, RUNNING)) {
            log.info("RUNNING: {} ({}ms)", message, System.currentTimeMillis() - startTime);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        state = STOP;
        log.info("STOPPED: {} ({}ms)", message, System.currentTimeMillis() - startTime);
    }
}
