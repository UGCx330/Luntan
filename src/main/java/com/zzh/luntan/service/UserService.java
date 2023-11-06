package com.zzh.luntan.service;

import com.zzh.luntan.entity.LoginTicket;
import com.zzh.luntan.entity.User;
import com.zzh.luntan.mapper.UserMapper;
import com.zzh.luntan.util.CommunityConstant;
import com.zzh.luntan.util.CommunityUtil;
import com.zzh.luntan.util.MailClient;
import com.zzh.luntan.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;

@Service
public class UserService implements CommunityConstant {
    @Autowired
    private TemplateEngine templateEngine;
    @Value("${zzh.path.domain}")
    private String community;
    @Value("${server.servlet.context-path}")
    private String contextPath;
    @Autowired
    private MailClient mailClient;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    // 直接从redis中获取user
    public User getReidsUser(int userId) {
        return (User) redisTemplate.opsForValue().get(RedisKeyUtil.getUserKey(userId));
    }

    // 从sql获取user存入redis
    public User initRedisUser(int userId) {
        User user = userMapper.selectById(userId);
        redisTemplate.opsForValue().set(RedisKeyUtil.getUserKey(userId), user);
        return user;
    }

    // 从缓存中删除user
    public void delRedisUser(int userId) {
        redisTemplate.delete(RedisKeyUtil.getUserKey(userId));
    }

    public User selectUserById(int userId) {
        User user = getReidsUser(userId);
        if (user == null) {
            user = initRedisUser(userId);
        }
        return user;
    }

    public Map<String, String> register(User user) {
        // 判空处理
        HashMap<String, String> map = new HashMap<>();
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "用户名不能为空");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空");
            return map;
        }

        // 重复处理
        if (userMapper.selectByUsername(user.getUsername()) != null) {
            map.put("usernameMsg", "该用户名已被使用");
            return map;
        }
        if (userMapper.selectByEmail(user.getEmail()) != null) {
            map.put("emailMsg", "该邮箱已被注册");
            return map;
        }

        // 插入注册用户
        String salt = CommunityUtil.generateUUID().substring(0, 5);
        user.setSalt(salt);
        user.setPassword(CommunityUtil.MD5(user.getPassword() + salt));
        user.setType(0);
        user.setStatus(0);
        user.setModerator(0);

        String activationCode = CommunityUtil.generateUUID();
        user.setActivationCode(activationCode);
        user.setHeaderUrl("http://s3oues4tk.hb-bkt.clouddn.com/%E9%BB%98%E8%AE%A4%E5%A4%B4%E5%83%8F.jpg");
        user.setCreateTime(new Date());
        // 插入数据，同时将主键id返回注入
        userMapper.insertUser(user);

        // 渲染html体
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://www.nowcoder.com/activation/abcdefg123456.html
        context.setVariable("target", community + contextPath + "/activation/" + user.getId() + "/" + activationCode);
        String process = templateEngine.process("/mail/activation", context);

        // 发送html邮件
        mailClient.sendMail(user.getEmail(), "激活账号", process);

        return map;
    }

    public int activation(int userId, String activationCode) {
        User user = userMapper.selectById(userId);
        // 激活失败：重复激活，激活码不对失败
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(activationCode)) {
            // 激活
            userMapper.updateStatus(userId, 1);
            delRedisUser(userId);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;
        }
    }

    public Map<String, Object> login(String username, String password, long expiredSeconds) {
        HashMap<String, Object> map = new HashMap<>();
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "用户名不能为空！");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            map.put("usernameMsg", "用户名不存在!");
            return map;
        }
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活!");
            return map;
        }
        if (!user.getPassword().equals(CommunityUtil.MD5(password + user.getSalt()))) {
            map.put("passwordMsg", "密码不正确！");
            return map;
        }
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        loginTicket.setStatus(0);
        String ticket = CommunityUtil.generateUUID();
        loginTicket.setTicket(ticket);
        // loginTicketMapper.insertTicket(loginTicket);
        // 凭证实体插入redis,因为拦截器需要验证实体的状态属性，所以不能光存储一个凭证字符串
        redisTemplate.opsForValue().set(RedisKeyUtil.getTicketKey(ticket), loginTicket);
        map.put("ticket", ticket);
        return map;
    }

    public void logout(String ticket) {
        // loginTicketMapper.updateTicketStatus(ticket, 1);
        // 先取出来，自动转为LoginTicket实体类型
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(RedisKeyUtil.getTicketKey(ticket));
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(RedisKeyUtil.getTicketKey(ticket), loginTicket);
    }

    public void delRedisKey(String key) {
        redisTemplate.delete(key);
    }

    public LoginTicket selectByTicket(String ticket) {
        return (LoginTicket) redisTemplate.opsForValue().get(RedisKeyUtil.getTicketKey(ticket));
    }

    public int updateHeaderUrl(int userId, String headerUrl) {
        int rows = userMapper.updateHeaderUrl(userId, headerUrl);
        delRedisUser(userId);
        return rows;
    }

    public int updatePassword(int userId, String newPassword) {
        int rows = userMapper.updatePassword(userId, newPassword);
        delRedisUser(userId);
        return rows;
    }

    public int updateProfile(int userId, String newUserName, String newContact, String newDescription) {
        int rows = userMapper.updateProfile(userId, newUserName, newContact, newDescription);
        delRedisUser(userId);
        return rows;
    }

    public Map<String, Object> resetPassword(String email, String password) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isBlank(email)) {
            map.put("emailMsg", "邮箱不能为空！");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "新密码不能为空！");
            return map;
        }
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            map.put("emailMsg", "该邮箱尚未注册！");
            return map;
        }
        userMapper.updatePassword(user.getId(), CommunityUtil.MD5(password + user.getSalt()));
        map.put("user", user);
        return map;
    }

    public User selectUserByName(String targetName) {
        return userMapper.selectByName(targetName);
    }

    // 获取用户权限
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.selectUserById(userId);
        ArrayList<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }

    public int updateProfileImg(int userId, String fileNames) {
        int i = userMapper.updateProfileImg(userId, fileNames);
        // 更新缓存中user信息，防止页面刷新相册依然存在。
        User user = userMapper.selectById(userId);
        redisTemplate.opsForValue().set(RedisKeyUtil.getUserKey(userId), user);
        return i;
    }

    public int updateProfileMusic(int userId, String fileName) {
        int i = userMapper.updateProfileMusic(userId, fileName);
        // 更新缓存中user信息，防止页面刷新音乐依然存在。
        User user = userMapper.selectById(userId);
        redisTemplate.opsForValue().set(RedisKeyUtil.getUserKey(userId), user);
        return i;
    }

    public int updateProfileVideo(int userId, String fileName) {
        int i = userMapper.updateProfileVideo(userId, fileName);
        // 更新缓存中user信息，防止页面刷新视频依然存在。
        User user = userMapper.selectById(userId);
        redisTemplate.opsForValue().set(RedisKeyUtil.getUserKey(userId), user);
        return i;
    }

    public int updateProfileBack(int userId, String fileName) {
        int i = userMapper.updateProfileBack(userId, fileName);
        // 更新缓存中user信息，防止页面刷新视频依然存在。
        User user = userMapper.selectById(userId);
        redisTemplate.opsForValue().set(RedisKeyUtil.getUserKey(userId), user);
        return i;
    }
}
