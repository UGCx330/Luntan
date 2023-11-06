package com.zzh.luntan.controller.advice;

import com.zzh.luntan.util.CommunityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.io.PrintWriter;

// 标注此类是异常通知类
@ControllerAdvice(annotations = Controller.class)
public class ExceptionAdvice {
    @Autowired
    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    // 标注处理的异常类型(500),参数自动注入
    @ExceptionHandler({Exception.class})
    public void handleException(Exception e, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        // 异常信息记录到logger中
        // 先打印粗略错误信息
        logger.error("服务器出错了： " + e.getMessage());
        // 记录详细错误信息
        for (StackTraceElement stackTraceElement : e.getStackTrace()) {
            logger.error(stackTraceElement.toString());
        }

        // 判断请求是否是ajax异步请求，如果是，则向前端返回字符串。如果不是，则返回500错误页面
        String requestWith = httpServletRequest.getHeader("x-requested-with");
        if ("XMLHttpRequest".equals(requestWith)) {
            // 是异步请求,一般设置返回类型为普通字符串-plain
            httpServletResponse.setContentType("application/plain;charset=utf-8");
            // response得到输出流输出到前端
            PrintWriter writer = httpServletResponse.getWriter();
            writer.write(CommunityUtil.getJSONString(1, "服务器出现错误"));
        } else {
            // 同步请求，则直接返回html，跳转至500错误页面
            httpServletResponse.sendRedirect(httpServletRequest.getContextPath() + "/500Error");
        }
    }


}
