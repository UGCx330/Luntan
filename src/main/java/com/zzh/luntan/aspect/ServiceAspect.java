package com.zzh.luntan.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@Aspect
public class ServiceAspect {
    private static final Logger logger = LoggerFactory.getLogger(ServiceAspect.class);

    // 设置切入点，springboot注解方式只支持以方法为切入点
    // 第一个* ：所有返回值类型
    // service后面的第一个*表示该包下的所有service类
    // service后面的第二个*表示所有service类中的所有方法
    // (..)表示方法的所有参数类型
    @Pointcut("execution(* com.zzh.luntan.service.*.*(..))")
    public void pointcut() {
    }

    // 用户每次调用service中方法时，记录进日志
    @Before("pointcut()")
    public void before(JoinPoint joinPoint) {
        // 记录格式 ：用户[ip地址]，在[时间]，访问了[com.zzh.luntan.service.xxx()].
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes==null){
            //说明此时是消费者service被Spring调用了
            return;
        }
        HttpServletRequest request = requestAttributes.getRequest();
        String ip = request.getRemoteHost();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String route = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
        // 因为不是记录错误信息，所以使用info
        logger.info(String.format("用户[%s],在[%s],访问了[%s].", ip, time, route));
    }
}
