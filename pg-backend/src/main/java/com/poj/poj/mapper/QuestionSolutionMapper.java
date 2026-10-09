package com.poj.poj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.poj.poj.model.entity.QuestionSolution;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface QuestionSolutionMapper extends BaseMapper<QuestionSolution> {
    @Select("SELECT COUNT(*) FROM question_solution_reveal WHERE questionId = #{questionId} "
            + "AND userId = #{userId}")
    int countReveal(@Param("questionId") long questionId, @Param("userId") long userId);

    @Insert("INSERT IGNORE INTO question_solution_reveal (questionId, userId) "
            + "VALUES (#{questionId}, #{userId})")
    int reveal(@Param("questionId") long questionId, @Param("userId") long userId);

    @Select("SELECT * FROM question_solution WHERE questionId = #{questionId} "
            + "AND userId = #{userId} ORDER BY id DESC LIMIT 1")
    QuestionSolution selectIncludingDeleted(@Param("questionId") long questionId,
                                             @Param("userId") long userId);

    @Update("UPDATE question_solution SET title = #{title}, content = #{content}, "
            + "isDelete = 0, updateTime = CURRENT_TIMESTAMP WHERE id = #{id}")
    int restore(@Param("id") long id, @Param("title") String title,
                @Param("content") String content);
}
