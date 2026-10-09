package com.poj.poj.model.dto.questionsolution;

import java.io.Serializable;
import lombok.Data;

@Data
public class QuestionSolutionSaveRequest implements Serializable {
    private Long questionId;
    private String title;
    private String content;
    private static final long serialVersionUID = 1L;
}
