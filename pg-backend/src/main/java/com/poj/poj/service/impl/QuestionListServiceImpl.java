package com.poj.poj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.poj.poj.common.ErrorCode;
import com.poj.poj.exception.BusinessException;
import com.poj.poj.mapper.QuestionListItemMapper;
import com.poj.poj.mapper.QuestionListMapper;
import com.poj.poj.model.entity.Question;
import com.poj.poj.model.entity.QuestionList;
import com.poj.poj.model.entity.QuestionListItem;
import com.poj.poj.model.vo.QuestionListVO;
import com.poj.poj.service.QuestionListService;
import com.poj.poj.service.QuestionService;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class QuestionListServiceImpl extends ServiceImpl<QuestionListMapper, QuestionList>
        implements QuestionListService {
    @Resource
    private QuestionListItemMapper itemMapper;
    @Resource
    private QuestionService questionService;

    @Override
    public long createQuestionList(String name, long userId) {
        String normalized = StringUtils.trimToEmpty(name);
        if (normalized.isEmpty() || normalized.length() > 64) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题单名称长度必须为 1 到 64 个字符");
        }
        QuestionList list = new QuestionList();
        list.setName(normalized);
        list.setUserId(userId);
        if (!save(list)) throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建题单失败");
        return list.getId();
    }

    @Override
    public List<QuestionListVO> listMine(long userId, Long questionId) {
        return list(new QueryWrapper<QuestionList>().eq("userId", userId).orderByDesc("updateTime"))
                .stream().map(list -> {
                    QuestionListVO vo = new QuestionListVO();
                    BeanUtils.copyProperties(list, vo);
                    vo.setQuestionCount(itemMapper.selectCount(new QueryWrapper<QuestionListItem>()
                            .eq("questionListId", list.getId())));
                    vo.setContainsQuestion(questionId != null && itemMapper.selectCount(
                            new QueryWrapper<QuestionListItem>().eq("questionListId", list.getId())
                                    .eq("questionId", questionId)) > 0);
                    return vo;
                }).collect(Collectors.toList());
    }

    @Override
    public boolean addQuestion(long questionListId, long questionId, long userId) {
        ownedList(questionListId, userId);
        Question question = questionService.getById(questionId);
        if (question == null || !"PUBLISHED".equals(question.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在或未发布");
        }
        if (itemMapper.selectCount(new QueryWrapper<QuestionListItem>()
                .eq("questionListId", questionListId).eq("questionId", questionId)) > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该题目已经在这个题单中");
        }
        QuestionListItem item = new QuestionListItem();
        item.setQuestionListId(questionListId);
        item.setQuestionId(questionId);
        try {
            boolean added = itemMapper.insert(item) == 1;
            touch(questionListId);
            return added;
        } catch (DuplicateKeyException error) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该题目已经在这个题单中");
        }
    }

    @Override
    public boolean removeQuestion(long questionListId, long questionId, long userId) {
        ownedList(questionListId, userId);
        int deleted = itemMapper.delete(new QueryWrapper<QuestionListItem>()
                .eq("questionListId", questionListId).eq("questionId", questionId));
        if (deleted == 0) throw new BusinessException(ErrorCode.OPERATION_ERROR, "该题目不在这个题单中");
        touch(questionListId);
        return true;
    }

    private QuestionList ownedList(long listId, long userId) {
        QuestionList list = getById(listId);
        if (list == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题单不存在");
        if (!list.getUserId().equals(userId)) throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        return list;
    }

    private void touch(long listId) {
        QuestionList update = new QuestionList();
        update.setId(listId);
        update.setUpdateTime(new java.util.Date());
        updateById(update);
    }
}
