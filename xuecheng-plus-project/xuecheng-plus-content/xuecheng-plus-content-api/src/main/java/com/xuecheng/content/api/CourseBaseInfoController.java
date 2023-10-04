package com.xuecheng.content.api;

import com.xuecheng.base.exception.ValidationGroups;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.util.SecurityUtil;
import com.xuecheng.cotent.model.dto.AddCourseDto;
import com.xuecheng.cotent.model.dto.CourseBaseInfoDto;
import com.xuecheng.cotent.model.dto.EditCourseDto;
import com.xuecheng.cotent.model.dto.QueryCourseParamsDto;
import com.xuecheng.cotent.model.po.CourseBase;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Api(value = "课程信息编辑接口",tags = "课程信息编辑接口")
@RestController //相当于controller 和 Responsibility 的合并
public class CourseBaseInfoController
{
    @Resource
    CourseBaseInfoService courseBaseInfoService;

    @ApiOperation("课程查询接口")
    @PreAuthorize("hasAuthority('xc_teachmanager_course_list')") //指定权限标识符, 拥有此权限才可以访问此方法
    @PostMapping("/course/list")
    public PageResult<CourseBase> list(PageParams pageParams, @RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto)
    {
        //当前登录的用户
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        //用户所属的机构id
        Long companyId = null;

        if(StringUtils.isNotEmpty(user.getCompanyId())) companyId = Long.parseLong(user.getCompanyId());

        PageResult<CourseBase> pageResult = courseBaseInfoService.queryCourseBaseList(companyId, pageParams, queryCourseParamsDto);
        return pageResult;
    }

    @ApiOperation("新增课程")
    @PostMapping("/course")  //@Validated用于合法性校验
    public CourseBaseInfoDto createCourseBase(@RequestBody @Validated(ValidationGroups.Insert.class) AddCourseDto addCourseDto)
    {
        //机构id，由于认证系统没有上线暂时硬编码
        Long companyId = 1232141425L;
        return courseBaseInfoService.createCourseBase(companyId, addCourseDto);
    }

    @ApiOperation("根据课程id查询接口")
    @GetMapping("/course/{courseId}")
    public CourseBaseInfoDto getCourseById(@PathVariable Long courseId)
    {
        //获取当前用户的身份
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        System.err.println("当前用户身份为：" + user);

        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }

    @ApiOperation("修改课程")
    @PutMapping("/course")
    public CourseBaseInfoDto modifyCourseBase(@RequestBody @Validated(ValidationGroups.Update.class) EditCourseDto editCourseDto)
    {
        //机构id，由于认证系统没有上线暂时硬编码
        Long companyId = 1232141425L;
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.modifyCourseBase(companyId, editCourseDto);
        return courseBaseInfoDto;
    }
}
