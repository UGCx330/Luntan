package com.zzh.luntan.controller.interceptor;

import com.zzh.luntan.entity.User;
import com.zzh.luntan.service.DataService;
import com.zzh.luntan.util.ThreadLocalVector;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class DataInterceptor implements HandlerInterceptor {
    // 此拦截器会在config中配置在登路拦截器之后
    @Autowired
    private ThreadLocalVector threadLocalVector;
    @Autowired
    private DataService dataService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取ip和本地线程容器的userId，存入redis
        String ip = request.getRemoteHost();
        dataService.setUV(ip);
        User user = threadLocalVector.getUser();
        // 用户登录情况下计入活跃
        if (user != null) {
            dataService.setDAU(user.getId());
        }
        return true;
    }
}
