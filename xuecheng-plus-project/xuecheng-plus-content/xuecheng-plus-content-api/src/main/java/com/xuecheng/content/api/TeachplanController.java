package com.xuecheng.content.api;

import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.cotent.model.dto.BindTeachplanMediaDto;
import com.xuecheng.cotent.model.dto.SaveTeachplanDto;
import com.xuecheng.cotent.model.dto.TeachplanDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 课程计划管理的相关接口
 */
@Api(value = "课程计划编辑接口",tags = "课程计划编辑接口")
@RestController
public class TeachplanController
{
    @Resource
    TeachplanService teachplanService;

    @ApiOperation("查询课程计划树形结构")
    @ApiImplicitParam(value = "courseId",name = "课程Id",required = true,dataType = "Long",paramType = "path")
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    public List<TeachplanDto> getTreeNodes(@PathVariable Long courseId)
    {
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        return teachplanTree;
    }

    @ApiOperation("课程计划创建或修改")
    @PostMapping("/teachplan")
    public void saveTeachplan(@RequestBody SaveTeachplanDto teachplan)
    {
        teachplanService.saveTeachplan(teachplan);
    }

    @ApiOperation(value = "课程计划和媒资信息绑定")
    @PostMapping("/teachplan/association/media")
    public void associationMedia(@RequestBody BindTeachplanMediaDto bindTeachplanMediaDto)
    {
        teachplanService.associationMedia(bindTeachplanMediaDto);
    }

    @ApiOperation(value = "永久删除课程视频")
    @DeleteMapping("/teachplan/association/media/{teachplanId}/{mediaId}")
    public void deleteAssociationMedia(@PathVariable Long teachplanId, @PathVariable String mediaId)
    {
        teachplanService.deleteAssociationMedia(teachplanId, mediaId);
    }
}
