package com.poj.poj.model.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.io.Serializable;
import lombok.Data;

@Data
public class QuestionSolutionPageVO implements Serializable {
    private boolean unlocked;
    private boolean canPublish;
    private boolean revealedWithoutAccepted;
    private String reason;
    private QuestionSolutionVO mine;
    private Page<QuestionSolutionVO> page;
    private static final long serialVersionUID = 1L;
}
