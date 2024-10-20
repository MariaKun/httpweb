package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Request {
    private String method;
    private String path;
    private String body;
    private List<String> headers;
    private Map<String, String> queryParam0;
    private List<NameValuePair> queryParam;

    public Request(String method, String path, List<String> headers, String body) throws URISyntaxException {
        this.method = method;
        this.headers = headers;
        this.body = body;
        this.queryParam = URLEncodedUtils.parse(new URI(path), Charset.forName("UTF-8"));
        this.path = new URI(path).getPath();
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getBody() {
        return body;
    }

    public List<String> getHeader() {
        return headers;
    }

    public List<NameValuePair> getQueryParams() {
        return queryParam;
    }

    public List<NameValuePair> getQueryParam(String name) {
        return queryParam.stream().filter(x -> x.getName().equals(name)).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "Request{" +
                "\n method='" + method + '\'' +
                "\n path='" + path + '\'' +
                "\n headers=" + headers +
                "\n body='" + body + '\'' +
                '}';
    }
}
