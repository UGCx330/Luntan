package com.zzh.luntan.util;

import com.zzh.luntan.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ThreadLocalVector {
    private ThreadLocal<User> threadLocal = new ThreadLocal<>();

    public User getUser() {
        return threadLocal.get();
    }

    public void setUser(User user) {
        threadLocal.set(user);
    }

    public void removeUser() {
        threadLocal.remove();
    }
}
