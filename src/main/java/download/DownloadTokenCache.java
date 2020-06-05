package download;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

/**
 * 下载链接的凭证缓存
 * @author chenhaijian
 * @date 2020-05-17 21:10
 */
public class DownloadTokenCache {

    // 通过CacheBuilder构建一个缓存实例
    public static Cache<String, String> cache = CacheBuilder.newBuilder()
            .maximumSize(200) // 设置缓存的最大容量
            .expireAfterWrite(20, TimeUnit.MINUTES) // 设置缓存在写入20分钟后失效
            .concurrencyLevel(10) // 设置并发级别为10
            .recordStats() // 开启缓存统计
            .build();

}
