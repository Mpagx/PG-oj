package com.poj.poj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
@TableName("question_list_item")
public class QuestionListItem implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long questionListId;
    private Long questionId;
    private Date createTime;
    private static final long serialVersionUID = 1L;
}
