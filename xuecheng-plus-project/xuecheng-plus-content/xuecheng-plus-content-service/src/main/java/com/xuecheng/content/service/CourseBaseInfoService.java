package com.xuecheng.content.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.cotent.model.dto.AddCourseDto;
import com.xuecheng.cotent.model.dto.CourseBaseInfoDto;
import com.xuecheng.cotent.model.dto.EditCourseDto;
import com.xuecheng.cotent.model.dto.QueryCourseParamsDto;
import com.xuecheng.cotent.model.po.CourseBase;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 课程信息管理接口
 */
public interface CourseBaseInfoService
{
    /**
     * 课程分页查询
     * @param companyId 培训机构id
     * @param pageParams 分页查询参数
     * @param queryCourseParamsDto 查询条件
     * @return 查询结果
     */
    PageResult<CourseBase> queryCourseBaseList(Long companyId, PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto);

    /**
     * 新增课程
     * @param companyID 机构ID
     * @param addCourseDto 课程信息
     * @return 课程详细信息
     */
    CourseBaseInfoDto createCourseBase(Long companyID, AddCourseDto addCourseDto);

    /**
     * 根据id查询课程
     * @param courseId 课程id
     * @return 课程详细信息
     */
    CourseBaseInfoDto getCourseBaseInfo(Long courseId);

    /**
     * 修改课程
     * @param companyID 机构Id
     * @param editCourseDto 修改课程信息
     * @return 课程详细信息
     */
    CourseBaseInfoDto modifyCourseBase(Long companyID, EditCourseDto editCourseDto);
}
