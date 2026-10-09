package com.poj.poj.mapper;

import com.poj.poj.model.entity.QuestionSubmit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.poj.poj.model.vo.DailySubmissionStatVO;
import com.poj.poj.model.vo.UserSubmissionOverviewVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
* @author Legion
* @description 针对表【question_submit(题目提交)】的数据库操作Mapper
* @createDate 2026-08-11 13:55:43
* @Entity com.poj.poj.model.entity.QuestionSubmit
*/
public interface QuestionSubmitMapper extends BaseMapper<QuestionSubmit> {
    @Select("SELECT COUNT(*) AS totalSubmissions, " +
            "COALESCE(SUM(CASE WHEN judgeInfo LIKE '%\"message\":\"Accepted\"%' THEN 1 ELSE 0 END), 0) AS acceptedSubmissions, " +
            "COUNT(DISTINCT questionId) AS attemptedQuestions, " +
            "COUNT(DISTINCT CASE WHEN judgeInfo LIKE '%\"message\":\"Accepted\"%' THEN questionId END) AS solvedQuestions " +
            "FROM question_submit WHERE userId = #{userId} AND isDelete = 0")
    UserSubmissionOverviewVO selectUserOverview(@Param("userId") long userId);

    @Select("SELECT DATE_FORMAT(createTime, '%Y-%m-%d') AS day, COUNT(*) AS submissionCount, " +
            "SUM(CASE WHEN judgeInfo LIKE '%\"message\":\"Accepted\"%' THEN 1 ELSE 0 END) AS acceptedCount " +
            "FROM question_submit WHERE userId = #{userId} AND isDelete = 0 " +
            "AND createTime >= DATE_SUB(CURRENT_DATE, INTERVAL 83 DAY) " +
            "GROUP BY DATE_FORMAT(createTime, '%Y-%m-%d') " +
            "ORDER BY DATE_FORMAT(createTime, '%Y-%m-%d')")
    List<DailySubmissionStatVO> selectRecentActivity(@Param("userId") long userId);
}




