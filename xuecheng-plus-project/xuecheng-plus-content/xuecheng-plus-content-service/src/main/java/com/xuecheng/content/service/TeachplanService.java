package com.xuecheng.content.service;

import com.xuecheng.cotent.model.dto.BindTeachplanMediaDto;
import com.xuecheng.cotent.model.dto.SaveTeachplanDto;
import com.xuecheng.cotent.model.dto.TeachplanDto;
import com.xuecheng.cotent.model.po.TeachplanMedia;

import java.util.List;

/**
 * 课程计划管理的相关接口
 */
public interface TeachplanService
{
    /**
     * 根据课程id查询课程计划
     * @param courseId 课程Id
     * @return 课程计划
     */
    List<TeachplanDto> findTeachplanTree(Long courseId);

    /**
     * 添加/修改/保存课程计划
     * @param teachplanDto  课程计划信息
     */
    public void saveTeachplan(SaveTeachplanDto teachplanDto);

    /**
     * @description 教学计划绑定媒资
     * @param bindTeachplanMediaDto
     * @return com.xuecheng.content.model.po.TeachplanMedia
     */
    public TeachplanMedia associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto);


    /**
     * 删除课程绑定的媒资
     * @param teachplanId  课程计划id
     * @param mediaId 媒资id
     */
    void deleteAssociationMedia(Long teachplanId, String mediaId);
}
