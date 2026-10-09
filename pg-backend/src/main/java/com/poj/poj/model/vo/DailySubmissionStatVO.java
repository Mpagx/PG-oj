package com.poj.poj.model.vo;

import java.io.Serializable;
import lombok.Data;

@Data
public class DailySubmissionStatVO implements Serializable {
    private String day;
    private Long submissionCount;
    private Long acceptedCount;
    private static final long serialVersionUID = 1L;
}
