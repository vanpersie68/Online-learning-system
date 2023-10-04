package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.cotent.model.dto.AddCourseDto;
import com.xuecheng.cotent.model.dto.CourseBaseInfoDto;
import com.xuecheng.cotent.model.dto.EditCourseDto;
import com.xuecheng.cotent.model.dto.QueryCourseParamsDto;
import com.xuecheng.cotent.model.po.CourseBase;
import com.xuecheng.cotent.model.po.CourseCategory;
import com.xuecheng.cotent.model.po.CourseMarket;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService
{
    @Resource
    CourseBaseMapper courseBaseMapper;
    @Resource
    CourseMarketMapper courseMarketMapper;
    @Resource
    CourseCategoryMapper courseCategoryMapper;

    /**
     * 课程分页查询
     * @param companyId 培训机构id
     * @param pageParams 分页查询参数
     * @param queryCourseParamsDto 查询条件
     * @return 查询结果
     */
    @Override
    public PageResult<CourseBase> queryCourseBaseList(Long companyId, PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto)
    {
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //根据名称进行模糊查询
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()), CourseBase::getName, queryCourseParamsDto.getCourseName());
        //根据课程的审核状态
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()), CourseBase::getAuditStatus,queryCourseParamsDto.getAuditStatus());
        //根据课程发布状态查询
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()), CourseBase::getStatus,queryCourseParamsDto.getPublishStatus());

        queryWrapper.eq(CourseBase::getCompanyId, companyId);

        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());

        //分页查询的结果
        Page<CourseBase> result = courseBaseMapper.selectPage(page, queryWrapper);
        //数据列表
        List<CourseBase> items = result.getRecords();
        //总记录数
        long total = result.getTotal();

        //准备返回数据 List<T> items, long counts, long page, long pageSize
        PageResult<CourseBase> pageResult = new PageResult<>(items, total, pageParams.getPageNo(), pageParams.getPageSize());

        return pageResult;
    }

    /**
     * 新增课程
     * @param companyID    机构ID
     * @param addCourseDto 课程信息
     * @return 课程详细信息
     */
    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyID, AddCourseDto addCourseDto)
    {
        //向course_base表写入数据
        CourseBase courseBase = new CourseBase();
        //将传入页面的参数放入到courseBase中
        BeanUtils.copyProperties(addCourseDto, courseBase);
        courseBase.setCompanyId(companyID);
        //审核状态默认为未提交
        courseBase.setAuditStatus("202002");
        //发布状态默认为未发布
        courseBase.setStatus("203001");
        courseBase.setCreateDate(LocalDateTime.now());

        //插入数据库
        int insert = courseBaseMapper.insert(courseBase);
        if(insert <= 0)
            XueChengPlusException.cast("添加课程失败");

        //向course_market 表写入数据
        CourseMarket courseMarketNew = new CourseMarket();
        //将页面输入的数据拷贝到courseMarketNew中
        BeanUtils.copyProperties(addCourseDto, courseMarketNew);
        courseMarketNew.setId(courseBase.getId());
        //保存营销信息
        saveCourseMarket(courseMarketNew);

        //从数据库查出课程的详情信息()
        CourseBaseInfoDto courseBaseInfoDto = getCourseBaseInfo(courseBase.getId());
        return courseBaseInfoDto;
    }

    //单独写一个方法保存course_market中的信息，存在则更新，不存在则添加
    public int saveCourseMarket(CourseMarket courseMarketNew)
    {
        //参数的合法性校验
        String charge = courseMarketNew.getCharge();
        if(StringUtils.isEmpty(charge))
            XueChengPlusException.cast("收费规则为空");

        //如果课程收费，价格没有填写也需要抛异常
        if(charge.equals("201001"))
        {
            if(courseMarketNew.getPrice() == null || courseMarketNew.getPrice().floatValue() <= 0)
            {
                XueChengPlusException.cast("课程的价格不能为空并且必须大于0");
            }
        }

        //从数据库查询营销信息，存在则更新，不存在则添加
        CourseMarket courseMarket = courseMarketMapper.selectById(courseMarketNew.getId());
        if(courseMarket == null)
        {
            //插入数据库
            return courseMarketMapper.insert(courseMarketNew);
        }
        else
        {
            //将courseMarketNew拷贝到courseMarket中
            BeanUtils.copyProperties(courseMarketNew,courseMarket);
            //防止courseMarketNew中id为空导致 更新后的数据id 也为空
            //courseMarket.setId(courseMarketNew.getId());
            //更新数据库
            return courseMarketMapper.updateById(courseMarket);
        }
    }

    /**
     * 根据id查询课程
     * @param courseId 课程id
     * @return 课程详细信息
     */
    @Override
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId)
    {
        //从课程基本信息表查询
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if(courseBase == null)
            return null;
        //从课程营销表查询
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);

        if(courseMarket != null)
        {
            //组装在一起
            CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
            BeanUtils.copyProperties(courseBase, courseBaseInfoDto);
            BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);

            //通过courseCategoryMapper查询课程分类信息，将课程分类名称放在courseBaseInfoDto中
            CourseCategory courseCategoryBySt = courseCategoryMapper.selectById(courseBase.getSt());
            courseBaseInfoDto.setStName(courseCategoryBySt.getName());
            CourseCategory courseCategoryByMt = courseCategoryMapper.selectById(courseBase.getMt());
            courseBaseInfoDto.setMtName(courseCategoryByMt.getName());

            return courseBaseInfoDto;
        }

        return null;
    }

    /**
     * 修改课程
     * @param companyID     机构Id
     * @param editCourseDto 修改课程信息
     * @return 课程详细信息
     */
    @Override
    public CourseBaseInfoDto modifyCourseBase(Long companyID, EditCourseDto editCourseDto)
    {
        Long courseId = editCourseDto.getId();
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);

        if(courseBase == null || courseMarket == null)
            XueChengPlusException.cast("课程不存在");

        //根据具体的业务逻辑去校验（eg：本机构只能修改本机构的课程）
        if(!companyID.equals(courseBase.getCompanyId()))
            XueChengPlusException.cast("本机构只能修改本机构的课程");
        //封装数据
        BeanUtils.copyProperties(editCourseDto, courseBase);
        BeanUtils.copyProperties(editCourseDto, courseMarket);
        //更新修改时间和修改人
        courseBase.setChangeDate(LocalDateTime.now());
        //更新数据库
        int result1 = courseBaseMapper.updateById(courseBase);
        int result2 = courseMarketMapper.updateById(courseMarket);
        if(result1 <= 0 || result2 <= 0)
           XueChengPlusException.cast("修改课程失败");

        //查询课程信息
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }
}
