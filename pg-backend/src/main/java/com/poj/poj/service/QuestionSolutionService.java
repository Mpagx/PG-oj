package com.poj.poj.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.poj.poj.common.ErrorCode;
import com.poj.poj.exception.BusinessException;
import com.poj.poj.mapper.QuestionSolutionMapper;
import com.poj.poj.model.dto.questionsolution.QuestionSolutionSaveRequest;
import com.poj.poj.model.entity.Question;
import com.poj.poj.model.entity.QuestionSolution;
import com.poj.poj.model.entity.QuestionSubmit;
import com.poj.poj.model.entity.User;
import com.poj.poj.model.vo.QuestionSolutionPageVO;
import com.poj.poj.model.vo.QuestionSolutionVO;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionSolutionService extends ServiceImpl<QuestionSolutionMapper, QuestionSolution> {
    @Resource private QuestionService questionService;
    @Resource private QuestionSubmitService questionSubmitService;
    @Resource private UserService userService;

    public QuestionSolutionPageVO pageForUser(long questionId, long current, long pageSize, User user,
                                               boolean admin) {
        requireVisibleQuestion(questionId, admin);
        if (current < 1 || pageSize < 1 || pageSize > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QuestionSolutionPageVO result = new QuestionSolutionPageVO();
        boolean accepted = admin || hasAccepted(questionId, user.getId());
        boolean manuallyRevealed = !accepted && baseMapper.countReveal(questionId, user.getId()) > 0;
        result.setCanPublish(accepted);
        result.setRevealedWithoutAccepted(manuallyRevealed);
        if (!accepted && !manuallyRevealed) {
            result.setUnlocked(false);
            result.setReason("通过这道题后即可查看题解");
            result.setPage(new Page<>(current, pageSize, 0));
            return result;
        }
        result.setUnlocked(true);
        Page<QuestionSolution> source = page(new Page<>(current, pageSize),
                new QueryWrapper<QuestionSolution>().eq("questionId", questionId)
                        .orderByDesc("updateTime").orderByDesc("id"));
        Set<Long> userIds = source.getRecords().stream().map(QuestionSolution::getUserId)
                .collect(Collectors.toSet());
        Map<Long, User> users = userIds.isEmpty() ? Collections.emptyMap()
                : userService.listByIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));
        List<QuestionSolutionVO> records = source.getRecords().stream()
                .map(item -> toVO(item, users.get(item.getUserId()), user.getId(), admin))
                .collect(Collectors.toList());
        Page<QuestionSolutionVO> output = new Page<>(current, pageSize, source.getTotal());
        output.setRecords(records);
        result.setPage(output);
        QuestionSolution mine = getOne(new QueryWrapper<QuestionSolution>()
                .eq("questionId", questionId).eq("userId", user.getId()));
        if (mine != null) result.setMine(toVO(mine, user, user.getId(), admin));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean reveal(long questionId, User user, boolean admin) {
        if (questionId <= 0) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        requireVisibleQuestion(questionId, admin);
        if (admin || hasAccepted(questionId, user.getId())) return true;
        baseMapper.reveal(questionId, user.getId());
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public long saveMine(QuestionSolutionSaveRequest request, User user, boolean admin) {
        if (request == null || request.getQuestionId() == null || request.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        requireVisibleQuestion(request.getQuestionId(), admin);
        if (!admin && !hasAccepted(request.getQuestionId(), user.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "通过这道题后才能发布题解");
        }
        String title = StringUtils.trim(request.getTitle());
        String content = StringUtils.trim(request.getContent());
        if (StringUtils.isBlank(title) || title.length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题解标题需要 1 到 100 个字符");
        }
        if (StringUtils.isBlank(content) || content.length() > 30000) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题解正文需要 1 到 30000 个字符");
        }
        QuestionSolution solution = getOne(new QueryWrapper<QuestionSolution>()
                .eq("questionId", request.getQuestionId()).eq("userId", user.getId()));
        if (solution == null) {
            QuestionSolution deleted = baseMapper.selectIncludingDeleted(
                    request.getQuestionId(), user.getId());
            if (deleted != null) {
                if (baseMapper.restore(deleted.getId(), title, content) != 1) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "题解恢复失败");
                }
                return deleted.getId();
            }
            solution = new QuestionSolution();
            solution.setQuestionId(request.getQuestionId());
            solution.setUserId(user.getId());
        }
        solution.setTitle(title);
        solution.setContent(content);
        solution.setUpdateTime(new Date());
        boolean saved = solution.getId() == null ? save(solution) : updateById(solution);
        if (!saved) throw new BusinessException(ErrorCode.OPERATION_ERROR, "题解保存失败");
        return solution.getId();
    }

    public boolean deleteSolution(long id, User user, boolean admin) {
        QuestionSolution solution = getById(id);
        if (solution == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        if (!admin && !solution.getUserId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return removeById(id);
    }

    boolean hasAccepted(long questionId, long userId) {
        return questionSubmitService.count(new QueryWrapper<QuestionSubmit>()
                .eq("questionId", questionId).eq("userId", userId).eq("status", 2)
                .like("judgeInfo", "\"message\":\"Accepted\"")) > 0;
    }

    private void requireVisibleQuestion(long questionId, boolean admin) {
        Question question = questionService.getById(questionId);
        if (question == null || (!admin && !"PUBLISHED".equals(question.getStatus()))) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
    }

    private QuestionSolutionVO toVO(QuestionSolution source, User author, long loginUserId, boolean admin) {
        QuestionSolutionVO vo = new QuestionSolutionVO();
        BeanUtils.copyProperties(source, vo);
        if (author != null) {
            vo.setUserName(author.getUserName());
            vo.setUserAvatar(author.getUserAvatar());
        }
        vo.setOwn(source.getUserId().equals(loginUserId));
        vo.setDeletable(Boolean.TRUE.equals(vo.getOwn()) || admin);
        return vo;
    }
}
