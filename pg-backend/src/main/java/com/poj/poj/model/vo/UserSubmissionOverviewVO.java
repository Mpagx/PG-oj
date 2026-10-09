package com.poj.poj.model.vo;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import lombok.Data;

@Data
public class UserSubmissionOverviewVO implements Serializable {
    private Long totalSubmissions = 0L;
    private Long acceptedSubmissions = 0L;
    private Long attemptedQuestions = 0L;
    private Long solvedQuestions = 0L;
    private List<DailySubmissionStatVO> activity = Collections.emptyList();
    private static final long serialVersionUID = 1L;
}
