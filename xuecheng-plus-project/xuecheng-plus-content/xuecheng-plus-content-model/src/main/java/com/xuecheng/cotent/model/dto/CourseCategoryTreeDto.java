package com.xuecheng.cotent.model.dto;

import com.xuecheng.cotent.model.po.CourseCategory;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CourseCategoryTreeDto extends CourseCategory implements Serializable
{
    List<CourseCategoryTreeDto> childrenTreeNodes;
}
