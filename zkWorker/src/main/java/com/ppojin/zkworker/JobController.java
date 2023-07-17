package com.ppojin.zkworker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/job")
public class JobController {
    private final JobService service;

    public JobController(JobService jobService) {
        this.service = jobService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void add(@RequestBody Job job){
        try {
            service.add(job);
        } catch (JobService.jobExistsException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/{id}/stop")
    @ResponseStatus(HttpStatus.OK)
    public void stop(@PathVariable String id){
        service.stop(id);
    }

    @GetMapping("/{id}/start")
    @ResponseStatus(HttpStatus.OK)
    public void start(@PathVariable String id){
        service.start(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void delete(@PathVariable String id){
        service.delete(id);
    }
}
