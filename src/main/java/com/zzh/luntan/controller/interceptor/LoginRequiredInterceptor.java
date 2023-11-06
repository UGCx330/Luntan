package com.zzh.luntan.controller.interceptor;

import com.zzh.luntan.controller.annotation.LoginRequired;
import com.zzh.luntan.util.ThreadLocalVector;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {
    @Autowired
    private ThreadLocalVector threadLocalVector;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果该url请求是被HandlerMapping映射的Controller中的某个方法而不是静态资源或者其他的
        ///那么拦截该请求
        if (handler instanceof HandlerMethod) {
            // 强转
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
            // 尝试获取该Controller方法的LoginRequired注解，如果有该注解，则判断当前线程容器中有无user，没有说明当前用户未登录，非法访问该url，拦截重定向到登录页
            if (loginRequired != null && threadLocalVector.getUser() == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return false;
            }
        }
        // 放行
        return true;
    }
}
