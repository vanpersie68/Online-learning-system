package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.service.CourseCategoryService;
import com.xuecheng.cotent.model.dto.CourseCategoryTreeDto;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseCategoryServiceImpl implements CourseCategoryService
{
    @Resource
    CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id)
    {
        //调用mapper递归查询出分类信息
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);
        //找到每个节点的子节点，最终封装成List<CourseCategoryTreeDto>
        //将list转成map，key就是节点的id，value就是CourseCategoryTreeDto对象，目的就是为了方便从map获取节点
        Map<String, CourseCategoryTreeDto> mapTemp = courseCategoryTreeDtos.stream().
                filter(item->!id.equals(item.getId())). //排除根节点
                collect(Collectors.toMap(key -> key.getId(), value -> value, (key1, key2) -> key2));

        //定义一个List，作为最终返回的list
        List<CourseCategoryTreeDto> categoryTreeDtos = new ArrayList<>();
        //从投遍历 List<CourseCategoryTreeDto>， 一边遍历一边找子节点放在父节点的childrenTreeNodes
        courseCategoryTreeDtos.stream().filter(item->!id.equals(item.getId())).forEach(item->
        {
            if(item.getParentid().equals(id))
            {
                //向List中写入元素
                categoryTreeDtos.add(item);
            }

            //找到节点的父节点
            CourseCategoryTreeDto parentID = mapTemp.get(item.getParentid());
            if(parentID != null)
            {
                if (parentID.getChildrenTreeNodes() == null)
                {
                    //如果该父节点的ChildrenTreeNodes属性为空要new一个集合，因为我们要向该集合中放它的子节点
                    parentID.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                }

                //找到每个节点的子节点，把子节点放在父节点的childrenTreeNodes属性中
                parentID.getChildrenTreeNodes().add(item);
            }
        });

        return categoryTreeDtos;

    }
}
