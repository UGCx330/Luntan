package com.zzh.luntan.controller;

import com.google.code.kaptcha.Producer;
import com.zzh.luntan.entity.User;
import com.zzh.luntan.service.UserService;
import com.zzh.luntan.util.CommunityConstant;
import com.zzh.luntan.util.CommunityUtil;
import com.zzh.luntan.util.MailClient;
import com.zzh.luntan.util.RedisKeyUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant {
    @Autowired
    private UserService userService;
    @Autowired
    private Producer kaptchaProducer;
    @Value("${server.servlet.context-path}")
    private String contextPath;
    @Autowired
    private TemplateEngine templateEngine;
    @Autowired
    private MailClient mailClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SecurityContextLogoutHandler securityContextLogoutHandler;

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @GetMapping("/register")
    public String getRegisterPage() {
        return "/site/register";
    }

    // 注册
    @PostMapping("/register")
    public String register(Model model, User user) {
        Map<String, String> map = userService.register(user);
        if (map == null || map.isEmpty()) {
            // 注册成功，发送邮件后跳转到操作界面，再跳转到主界面
            model.addAttribute("msg", "注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活!");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        }
        model.addAttribute("usernameMsg", map.get("usernameMsg"));
        model.addAttribute("passwordMsg", map.get("passwordMsg"));
        model.addAttribute("emailMsg", map.get("emailMsg"));
        // 如果注册失败，自动注入user到Model，回填用户输入的错误的信息
        return "/site/register";
    }

    // 激活
    @GetMapping("/activation/{userId}/{activationCode}")
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("activationCode") String activationCode) {
        int result = userService.activation(userId, activationCode);
        // 激活成功则跳转到登陆界面
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "激活成功,您的账号已经可以正常使用了!");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作,该账号已经激活过了!");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "激活失败,您提供的激活码不正确!");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    @GetMapping("/login")
    public String getLoginPage() {
        return "/site/login";
    }

    @GetMapping("/kaptcha")
    public void getKaptcha(HttpServletResponse response, HttpSession session) {
        String kaptcha = kaptchaProducer.createText();
        //// 生成验证码存到session域
        // session.setAttribute("kaptcha", kaptcha);
        // 验证码存入redis
        String kaptchaUUID = CommunityUtil.generateUUID();
        redisTemplate.opsForValue().set(RedisKeyUtil.getKaptchaKey(kaptchaUUID), kaptcha, 60, TimeUnit.SECONDS);
        Cookie cookie = new Cookie("kaptchaUUID", kaptchaUUID);
        cookie.setMaxAge(120);
        cookie.setPath(contextPath);
        response.addCookie(cookie);

        BufferedImage image = kaptchaProducer.createImage(kaptcha);
        // 使用response输出流输出imag
        response.setContentType("image/png");
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            ImageIO.write(image, "png", outputStream);
        } catch (IOException e) {
            logger.error("获取验证码失败" + e.getMessage());
        }
    }

    @PostMapping("/login")
    public String login(@CookieValue(value = "kaptchaUUID",required = false) String kaptchaUUID, Model model, String username, String password, boolean rememberMe, String kaptcha, HttpServletResponse response) {
        String redisKaptcha = null;
        if (StringUtils.isNotBlank(kaptchaUUID)) {
            redisKaptcha = (String) redisTemplate.opsForValue().get(RedisKeyUtil.getKaptchaKey(kaptchaUUID));
        }
        if (StringUtils.isBlank(redisKaptcha)||StringUtils.isBlank(kaptcha)||!redisKaptcha.equalsIgnoreCase(kaptcha)){
            model.addAttribute("kaptchaMsg","验证码不正确");
            return "/site/login";
        }

        // 过期秒数
        int expiredSeconds = rememberMe ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        // 说明成功登录，并返回ticket,存入cookie
        if (map.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        } else {
            // 登陆不成功，讲错误消息存入model
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }

    @GetMapping("/logout")
    public String logout(@CookieValue("ticket") String ticket, HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        userService.logout(ticket);
        //从session中清理用户数据
        securityContextLogoutHandler.logout(request, response, authentication);
        return "redirect:/login";
    }

    @GetMapping("/getForgetPage")
    public String getForgetPage() {
        return "/site/forget";
    }

    // 存储忘记密码的验证码并发送验证码到邮箱
    @GetMapping("/sendKaptchaToEmail")
    @ResponseBody
    public String postKaptchaToEmail(String email, HttpSession session) {
        if (StringUtils.isBlank(email)) {
            return CommunityUtil.getJSONString(1, "请填写邮箱！");
        }
        String kaptcha = CommunityUtil.generateUUID().substring(0, 4);
        session.setAttribute("kaptcha", email + kaptcha);
        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("kaptcha", kaptcha);
        String process = templateEngine.process("/mail/forget", context);
        mailClient.sendMail(email, "白小恒论坛-忘记密码", process);
        return CommunityUtil.getJSONString(0);
    }

    @PostMapping("/resetPassword")
    public String resetPassword(String email, String kaptcha, String password, Model model, HttpSession session) {
        String email_kaptcha = (String) session.getAttribute("kaptcha");
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(email_kaptcha) || !(email + kaptcha).equals(email_kaptcha)) {
            model.addAttribute("kaptchaMsg", "请检查邮箱或者验证码是否书写错误！");
            return "/site/forget";
        }
        Map<String, Object> map = userService.resetPassword(email, password);
        if (map.containsKey("user")) {
            // 修改成功，跳转登陆界面
            return "redirect:/login";
        } else {
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/forget";
        }
    }


}
