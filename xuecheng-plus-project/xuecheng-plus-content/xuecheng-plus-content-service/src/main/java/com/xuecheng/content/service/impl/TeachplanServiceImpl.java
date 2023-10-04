package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.cotent.model.dto.BindTeachplanMediaDto;
import com.xuecheng.cotent.model.dto.SaveTeachplanDto;
import com.xuecheng.cotent.model.dto.TeachplanDto;
import com.xuecheng.cotent.model.po.Teachplan;
import com.xuecheng.cotent.model.po.TeachplanMedia;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TeachplanServiceImpl implements TeachplanService
{
    @Resource
    private TeachplanMapper teachplanMapper;

    @Resource
    private TeachplanMediaMapper teachplanMediaMapper;

    /**
     * 根据课程id查询课程计划
     * @param courseId 课程Id
     * @return 课程计划
     */
    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId)
    {
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(courseId);
        return teachplanDtos;
    }

    /**
     * 添加/修改/保存课程计划
     * @param teachplanDto 课程计划信息
     */
    @Override
    public void saveTeachplan(SaveTeachplanDto teachplanDto)
    {
        //通过课程计划id判断是新增还是修改
        Long teachplanId = teachplanDto.getId();
        if(teachplanId == null) //新增
        {
            //取出同父同级别的课程计划数量
            int count = getTeachplanCount(teachplanDto.getCourseId(), teachplanDto.getParentid());
            Teachplan teachplanNew = new Teachplan();
            //设置排序号
            teachplanNew.setOrderby(count+1);
            BeanUtils.copyProperties(teachplanDto, teachplanNew);
            teachplanMapper.insert(teachplanNew);
        }
        else //修改
        {
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);
            BeanUtils.copyProperties(teachplanDto,teachplan);
            teachplanMapper.updateById(teachplan);
        }
    }

    /**
     * @description 获取最新的排序号
     * @param courseId  课程id
     * @param parentId  父课程计划id
     * @return int 最新排序号
     */
    private int getTeachplanCount(long courseId,long parentId)
    {
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId,courseId);
        queryWrapper.eq(Teachplan::getParentid,parentId);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        return count;
    }

    /**
     * @param bindTeachplanMediaDto
     * @return com.xuecheng.content.model.po.TeachplanMedia
     * @description 教学计划绑定媒资
     */
    @Override
    @Transactional
    public TeachplanMedia associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto)
    {
        //教学计划id
        Long teachplanId = bindTeachplanMediaDto.getTeachplanId();
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);

        if(teachplan==null)
        {
            XueChengPlusException.cast("教学计划不存在");
        }

        Integer grade = teachplan.getGrade();
        if(grade != 2)
        {
            XueChengPlusException.cast("只允许第二级教学计划绑定媒资文件");
        }

        //课程id
        Long courseId = teachplan.getCourseId();

        //先删除原有绑定的视频, 根据课程计划的id删除它所绑定的媒资
        QueryWrapper<TeachplanMedia> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teachplan_id", bindTeachplanMediaDto.getTeachplanId());
        int deleteResult = teachplanMediaMapper.delete(queryWrapper);

        //添加新的绑定的视频
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        teachplanMedia.setCourseId(courseId);
        teachplanMedia.setTeachplanId(teachplanId);
        teachplanMedia.setMediaFilename(bindTeachplanMediaDto.getFileName());
        teachplanMedia.setMediaId(bindTeachplanMediaDto.getMediaId());
        teachplanMedia.setCreateDate(LocalDateTime.now());
        teachplanMediaMapper.insert(teachplanMedia);

        return teachplanMedia;
    }

    /**
     * 删除课程绑定的媒资
     *
     * @param teachplanId 课程计划id
     * @param mediaId     媒资id
     */
    @Override
    public void deleteAssociationMedia(Long teachplanId, String mediaId)
    {
        QueryWrapper<TeachplanMedia> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teachplan_Id", teachplanId);
        teachplanMediaMapper.delete(queryWrapper);
    }
}
