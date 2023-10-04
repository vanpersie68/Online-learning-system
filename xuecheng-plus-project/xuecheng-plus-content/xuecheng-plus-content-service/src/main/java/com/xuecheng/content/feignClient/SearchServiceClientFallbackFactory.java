package com.xuecheng.content.feignClient;

import com.xuecheng.search.po.CourseIndex;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SearchServiceClientFallbackFactory implements FallbackFactory<SearchServiceClient>
{
    @Override
    public SearchServiceClient create(Throwable throwable)
    {
        return new SearchServiceClient()
        {
            @Override
            public Boolean add(CourseIndex courseIndex)
            {
                throwable.printStackTrace();
                log.debug("调用搜索发生熔断走降级方法, 索引信息：{}, 熔断异常: {}", courseIndex, throwable.getMessage());
                return false;
            }
        };
    }
}
