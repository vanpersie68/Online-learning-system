package com.xuecheng.cotent.model.dto;

import lombok.Data;
import lombok.ToString;

/**
 * 课程进行模糊查询时用到的字段
 */

@Data
@ToString
public class QueryCourseParamsDto
{
    //课程名称
    private String courseName;
    //审核状态
    private String auditStatus;
    //发布状态
    private String publishStatus;
}
