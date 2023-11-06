package com.zzh.luntan.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public class CookieUtil {
    //得到通行证
    public static String getCookie(HttpServletRequest request,String key) throws IllegalAccessException {
        if (request == null||key==null) {
            throw new IllegalAccessException("空request,key异常！");
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (key.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
