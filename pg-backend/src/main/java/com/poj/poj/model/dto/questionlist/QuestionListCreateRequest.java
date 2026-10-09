package com.poj.poj.model.dto.questionlist;

import java.io.Serializable;
import lombok.Data;

@Data
public class QuestionListCreateRequest implements Serializable {
    private String name;
    private static final long serialVersionUID = 1L;
}
