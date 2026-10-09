package com.poj.poj.model.dto.questionlist;

import java.io.Serializable;
import lombok.Data;

@Data
public class QuestionListQuestionRequest implements Serializable {
    private Long questionListId;
    private Long questionId;
    private static final long serialVersionUID = 1L;
}
