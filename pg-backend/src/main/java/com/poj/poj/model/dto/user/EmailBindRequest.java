package com.poj.poj.model.dto.user;

import lombok.Data;
import java.io.Serializable;

@Data
public class EmailBindRequest implements Serializable {
    private String email;
    private String code;
    private static final long serialVersionUID = 1L;
}
