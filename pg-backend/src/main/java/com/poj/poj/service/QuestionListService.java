package com.poj.poj.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.poj.poj.model.entity.QuestionList;
import com.poj.poj.model.vo.QuestionListVO;
import java.util.List;

public interface QuestionListService extends IService<QuestionList> {
    long createQuestionList(String name, long userId);
    List<QuestionListVO> listMine(long userId, Long questionId);
    boolean addQuestion(long questionListId, long questionId, long userId);
    boolean removeQuestion(long questionListId, long questionId, long userId);
}
