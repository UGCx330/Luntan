package com.zzh.luntan.controller.interceptor;

import com.zzh.luntan.entity.LoginTicket;
import com.zzh.luntan.entity.User;
import com.zzh.luntan.service.UserService;
import com.zzh.luntan.util.CookieUtil;
import com.zzh.luntan.util.ThreadLocalVector;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor {
    @Autowired
    private UserService userService;
    @Autowired
    private ThreadLocalVector threadLocalVector;
    @Autowired
    private SecurityContextRepository securityContextRepository;

    // 每个controller方法调用之前
    // handler即为Controller中与url相匹配的方法(Springboot启动过程中的HandlerMapping类发挥作用)
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从request中获取cookie中的通行证
        String ticket = CookieUtil.getCookie(request, "ticket");
        // 拥有通行证情况下，与redis的通行证匹配
        if (ticket != null) {
            // redis中获取
            LoginTicket loginTicket = userService.selectByTicket(ticket);
            // 通行证匹配且确保有效
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
                User user = userService.selectUserById(loginTicket.getUserId());
                // 保存到ThreadLocal中供本次线程使用,因为一次请求的处理过程相当于一次线程，线程不结束，user就会在本地线程容器中，所以拦截器在渲染之前可以获取user存入Model中，或者整个请求中甚至service方法中都可以随时使用
                    // 保存user到当前线程容器
                    threadLocalVector.setUser(user);
                    // 因为没有在User中implementsUserDetails，所以要保存用户信息到SecurityContext(线程容器),以供Security认证和鉴权
                    // 用户信息分为三种，user本体，user权限，密码，权限需要我们在userService中获取
                    SecurityContextHolder.setContext(new SecurityContextImpl(new UsernamePasswordAuthenticationToken(user, user.getPassword(), userService.getAuthorities(user.getId()))));
                    // 再将SecurityContext保存到session中，让Security拦截其他请求鉴权时可以取到用户数据
                    securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
            }
        }
        return true;
    }

    // Controller方法之后渲染之前
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = threadLocalVector.getUser();
        // 某些请求可能不涉及渲染，那就不使用modelAndView
        if (user != null && modelAndView != null) {
            // 保存user到Model
            modelAndView.addObject("loginUser", user);
        }
    }

    // 渲染之后
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除user
        threadLocalVector.removeUser();
        SecurityContextHolder.clearContext();
    }
}
