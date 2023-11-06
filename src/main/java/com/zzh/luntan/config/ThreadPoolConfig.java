package com.zzh.luntan.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableAsync
public class ThreadPoolConfig {
    //有这个配置类，Spring可执行定时任务的线程池才能被正常实例化到容器
}
