package com.xuecheng.cotent.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 课程预览模型类
 */
@Data
public class CoursePreviewDto
{
    //课程基本信息、课程营销信息
    CourseBaseInfoDto courseBase;
    //课程计划信息
    List<TeachplanDto> teachplans;
    //课程师资信息...
}
