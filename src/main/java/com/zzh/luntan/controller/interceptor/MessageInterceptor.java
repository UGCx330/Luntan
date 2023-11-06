package com.zzh.luntan.controller.interceptor;

import com.zzh.luntan.entity.User;
import com.zzh.luntan.service.MessageService;
import com.zzh.luntan.util.ThreadLocalVector;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class MessageInterceptor implements HandlerInterceptor {
    @Autowired
    private ThreadLocalVector threadLocalVector;
    @Autowired
    private MessageService messageService;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = threadLocalVector.getUser();
        if (user != null && modelAndView != null) {
            int letterUnreadCount = messageService.unreadCount(user.getId(), null);
            int noticeUnreadCount = messageService.selectUnreadCountOfTopic(user.getId(), null);
            modelAndView.addObject("allUnreadCount", letterUnreadCount + noticeUnreadCount);
        }
    }
}
