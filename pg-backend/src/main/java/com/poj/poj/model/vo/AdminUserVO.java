package com.poj.poj.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/** Administrator-facing account data; deliberately excludes password and OAuth identifiers. */
@Data
public class AdminUserVO implements Serializable {
    private Long id;
    private String userName;
    private String userEmail;
    private Date emailVerifiedAt;
    private String userRole;
    private Date createTime;
    private Date updateTime;
    private static final long serialVersionUID = 1L;
}
