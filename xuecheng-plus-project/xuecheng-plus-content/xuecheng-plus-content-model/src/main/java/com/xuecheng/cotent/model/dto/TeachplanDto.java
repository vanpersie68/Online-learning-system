package com.xuecheng.cotent.model.dto;

import com.xuecheng.cotent.model.po.Teachplan;
import com.xuecheng.cotent.model.po.TeachplanMedia;
import lombok.Data;

import java.util.List;

@Data
public class TeachplanDto extends Teachplan
{
    //课程计划关联的媒资信息
    TeachplanMedia teachplanMedia;
    //小章节List
    List<TeachplanDto> teachPlanTreeNodes;
}
