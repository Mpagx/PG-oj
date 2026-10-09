package com.poj.poj.model.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
public class QuestionSolutionVO implements Serializable {
    private Long id;
    private Long questionId;
    private Long userId;
    private String title;
    private String content;
    private String userName;
    private String userAvatar;
    private Date createTime;
    private Date updateTime;
    private Boolean own;
    private Boolean deletable;
    private static final long serialVersionUID = 1L;
}
