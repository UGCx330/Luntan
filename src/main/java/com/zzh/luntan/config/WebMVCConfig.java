package com.zzh.luntan.config;

import com.zzh.luntan.controller.interceptor.DataInterceptor;
import com.zzh.luntan.controller.interceptor.LoginRequiredInterceptor;
import com.zzh.luntan.controller.interceptor.LoginTicketInterceptor;
import com.zzh.luntan.controller.interceptor.MessageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// web组件的配置类
@Configuration
public class WebMVCConfig implements WebMvcConfigurer {
    @Autowired
    private LoginTicketInterceptor loginTicketInterceptor;
    @Autowired
    private LoginRequiredInterceptor loginRequiredInterceptor;
    @Autowired
    private MessageInterceptor messageInterceptor;
    @Autowired
    private DataInterceptor dataInterceptor;


    // 添加拦截器方法
    // 拦截器preHandler执行顺序为此处配置顺序
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 除了静态资源，其他所有请求都需要拦截检查通行证，查询user存容器
        registry.addInterceptor(loginTicketInterceptor)
                // 排除静态资源,第一个/表示根目录，**表示所有级文件夹，*.css表示所有css文件
                .excludePathPatterns("/**/*.css", "/**/*.jpg", "/**/*.png", "/**/*.css", "/**/*.jpeg", "/**/*.js");
        // 除了静态资源，其他所有请求都需要拦截
        // 拦截器的preHandle方法是按照注册顺序执行的，所以只要用户登录了，此拦截器生效前本地线程容器中一定有user
        // ps:拦截器另外两个方法执行顺序与注册顺序相反
        registry.addInterceptor(loginRequiredInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.jpg", "/**/*.png", "/**/*.css", "/**/*.jpeg", "/**/*.js");
        registry.addInterceptor(messageInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.jpg", "/**/*.png", "/**/*.css", "/**/*.jpeg", "/**/*.js");
        registry.addInterceptor(dataInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.jpg", "/**/*.png", "/**/*.css", "/**/*.jpeg", "/**/*.js");
    }
}
