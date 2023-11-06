package com.zzh.luntan.config;

import com.zzh.luntan.util.CommunityConstant;
import com.zzh.luntan.util.CommunityUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig implements WebMvcConfigurer, CommunityConstant {

    // 保存SecurityContext到session工具
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // 忽略静态资源
        return (web) -> web.ignoring().requestMatchers("/resource/**");
    }

    // 退出清除session中用户信息相关
    @Bean
    public SecurityContextLogoutHandler securityContextLogoutHandler() {
        return new SecurityContextLogoutHandler();
    }


    // 配置相关
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                // 权限访问路径
                                "/user/getSettingPage",
                                "/user/headerSetting",
                                "/user/passwordSetting",
                                "/post/add",
                                "/comment/addComment/**",
                                "/letter/**",
                                "/notice/**",
                                "/like",
                                "/follow",
                                "/deFollow"
                        ).hasAnyAuthority(
                                AUTHORITY_USER,
                                AUTHORITY_ADMIN,
                                AUTHORITY_MODERATOR
                        )
                        .requestMatchers("/post/setTop",
                                "post/setWonderful")
                        .hasAnyAuthority(AUTHORITY_MODERATOR,AUTHORITY_ADMIN)
                        .requestMatchers("/admin/**","/actuator/**")
                        .hasAnyAuthority(AUTHORITY_ADMIN)
                        // 除了上述路径之外的所有路径一律放行
                        .anyRequest().permitAll()
                )
                .httpBasic(withDefaults());
        // 任何请求关闭csrf
        http.csrf().disable();
        // 异常处理
        http.exceptionHandling()
                // 认证失败处理
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
                        // 查看请求是否是异步请求
                        String xrequestWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xrequestWith)) {
                            // 异步请求
                            response.setContentType("application/plain;charset=utf-8");
                            response.getWriter().write(CommunityUtil.getJSONString(403, "请登录！"));
                        } else {
                            response.sendRedirect(request.getContextPath() + "/login");
                        }
                    }
                })
                // 权限不够处理
                .accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
                        // 查看请求是否是异步请求
                        String xrequestWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xrequestWith)) {
                            // 异步请求
                            response.setContentType("application/plain;charset=utf-8");
                            response.getWriter().write(CommunityUtil.getJSONString(403, "你没有访问此功能的权限！"));
                        } else {
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                });
        // Security默认拦截/logout请求，随便起一个路径名覆盖掉Security中的此请求即可
        // 这样退出就可以使用我们自己的实现了
        http.logout()
                .logoutUrl("/logoutSecurity");


        return http.build();
    }
}
