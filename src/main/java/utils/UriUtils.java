package utils;

import io.netty.handler.codec.http.HttpRequest;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * URI 工具类
 * @author chenhaijian
 * @date 2020-05-21 16:43
 */
public class UriUtils {

    /**
     * 获取HttpRequest中的uri封装成URI对象
     * @param httpRequest
     * @return
     * @throws URISyntaxException
     */
    public static URI getURI(HttpRequest httpRequest) throws URISyntaxException {
        return new URI(httpRequest.uri());
    }

    /**
     * 获取URI对象中的参数
     * @param uri
     * @return
     */
    public static Map<String, String> getQueryParams(URI uri) {
        Map<String, String> params = new HashMap<>();
        String query = uri.getQuery();
        if (StringUtils.isNotBlank(query)) {
            String[] paramsItems = query.split("&");
            for (String item : paramsItems) {
                String[] kv = item.split("=");
                params.put(kv[0], kv[1]);
            }
        }
        return params;
    }

    /**
     * 获取HttpRequest中的 query params
     * @param httpRequest
     * @return
     */
    public static Map<String, String> getQueryParams(HttpRequest httpRequest) throws URISyntaxException {
        URI uri = getURI(httpRequest);
        return getQueryParams(uri);
    }
}
