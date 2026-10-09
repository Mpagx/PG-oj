package com.poj.poj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
@TableName("question_list")
public class QuestionList implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long userId;
    private Date createTime;
    private Date updateTime;
    @TableLogic
    private Integer isDelete;
    private static final long serialVersionUID = 1L;
}
