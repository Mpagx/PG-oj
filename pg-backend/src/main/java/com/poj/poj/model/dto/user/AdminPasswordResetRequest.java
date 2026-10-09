package com.poj.poj.model.dto.user;

import lombok.Data;
import java.io.Serializable;

@Data
public class AdminPasswordResetRequest implements Serializable {
    private Long userId;
    private static final long serialVersionUID = 1L;
}
