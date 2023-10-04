package com.xuecheng.content;

import com.xuecheng.content.service.CourseCategoryService;
import com.xuecheng.cotent.model.dto.CourseCategoryTreeDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
public class CourseCategoryServiceTests
{
    @Resource
    CourseCategoryService courseCategoryService;

    @Test
    public void testCourseCategoryService()
    {
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryService.queryTreeNodes("1");
        System.err.println(courseCategoryTreeDtos);
    }
}
