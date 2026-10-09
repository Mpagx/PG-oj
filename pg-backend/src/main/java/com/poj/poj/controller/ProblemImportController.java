package com.poj.poj.controller;

import com.poj.poj.annotation.AuthCheck;
import com.poj.poj.common.BaseResponse;
import com.poj.poj.common.ResultUtils;
import com.poj.poj.constant.UserConstant;
import com.poj.poj.model.entity.User;
import com.poj.poj.model.vo.ProblemImportResultVO;
import com.poj.poj.service.ProblemPackageImportService;
import com.poj.poj.service.UserService;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/problem-import")
public class ProblemImportController {
    @Resource private ProblemPackageImportService importService;
    @Resource private UserService userService;

    @PostMapping("/zip")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<ProblemImportResultVO> importZip(@RequestPart("file") MultipartFile file,
                                                         HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(importService.importPackage(file, user.getId()));
    }
}
