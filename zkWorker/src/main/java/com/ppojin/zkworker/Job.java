package com.ppojin.zkworker;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Job {
    private String id;
    private String message;
}
