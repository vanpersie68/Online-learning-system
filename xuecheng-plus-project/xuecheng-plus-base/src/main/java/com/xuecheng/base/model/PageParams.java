package com.xuecheng.base.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

/**
 * 分页查询、分页参数
 */
@Data
@ToString
public class PageParams
{
    //当前页码
    @ApiModelProperty("页码")
    private Long pageNo = 1L; //当前页码是1
    //每页记录数默认值
    @ApiModelProperty("每页记录数")
    private Long pageSize =10L; //当前每页显示10条

    public PageParams(){}

    public PageParams(Long pageNo, Long pageSize)
    {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }
}
