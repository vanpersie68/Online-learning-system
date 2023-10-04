package com.xuecheng.content;

import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.cotent.model.dto.TeachplanDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
public class TeachplanMapperTests
{
    @Resource
    private TeachplanMapper teachplanMapper;

    @Test
    public void testSelectTreeNodes()
    {
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(117L);
        System.err.println(teachplanDtos);
    }
}
