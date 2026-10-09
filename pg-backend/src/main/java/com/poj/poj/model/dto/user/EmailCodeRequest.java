package com.poj.poj.model.dto.user;

import lombok.Data;
import java.io.Serializable;

@Data
public class EmailCodeRequest implements Serializable {
    private String purpose;
    private String email;
    private String captchaAnswer;
    private static final long serialVersionUID = 1L;
}
