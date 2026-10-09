package com.poj.poj.model.dto.user;

import lombok.Data;
import java.io.Serializable;

@Data
public class PasswordResetRequest implements Serializable {
    private String email;
    private String code;
    private String newPassword;
    private String checkPassword;
    private static final long serialVersionUID = 1L;
}
