package com.poj.poj.mapper;

import com.poj.poj.model.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
* @author Legion
* @description 针对表【question(题目)】的数据库操作Mapper
* @createDate 2026-08-11 13:52:02
* @Entity com.poj.poj.model.entity.Question
*/
public interface QuestionMapper extends BaseMapper<Question> {

    @Update("UPDATE question SET submitNum = submitNum + 1 WHERE id = #{questionId} AND isDelete = 0")
    int incrementSubmitNum(@Param("questionId") long questionId);

    @Update("UPDATE question SET acceptedNum = acceptedNum + 1 WHERE id = #{questionId} AND isDelete = 0")
    int incrementAcceptedNum(@Param("questionId") long questionId);

}




