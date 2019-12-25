package com.auto.development.security;

import lombok.AllArgsConstructor;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Luchaoxin
 * @Description: js跨域访问过滤器
 * @date 2019-05-07
 */
@AllArgsConstructor
public class SimpleCorsFilter implements Filter {

    private String accessControlAllowOrigin;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        response.setHeader("Access-Control-Allow-Origin", accessControlAllowOrigin);
        response.setHeader("X-Content-Type-Options", "nosniff");
        //开启XSS保护
        response.setHeader("X-XSS-Protection", "1");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, PATCH, DELETE, PUT");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
        chain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig filterConfig) {

    }


    @Override
    public void destroy() {

    }
}
