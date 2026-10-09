package com.poj.poj.model.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ProblemImportResultVO implements Serializable {
    private Long questionId;
    private String title;
    private String packageType;
    private Integer caseCount;
    private Integer generatedCaseCount;
    private String status;
    private List<String> checks = new ArrayList<>();
    private static final long serialVersionUID = 1L;
}
