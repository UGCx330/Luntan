package com.zzh.luntan.controller;

import com.zzh.luntan.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
public class DataController {
    @Autowired
    private DataService dataService;

    @RequestMapping(path = "/admin/data", method = {RequestMethod.GET, RequestMethod.POST})
    public String getAdmin() {
        return "/site/admin/data";
    }

    @PostMapping("/admin/uv")
    public String getUV(Model model, @DateTimeFormat(pattern = "yyy-MM-dd") Date start, @DateTimeFormat(pattern = "yyyy-MM-dd") Date end) {
        long calculatedUV = dataService.calculateUV(start, end);
        model.addAttribute("calculatedUV", calculatedUV);
        model.addAttribute("UVStart", start);
        model.addAttribute("UVEnd", end);
        return "/site/admin/data";
    }

    @PostMapping("/admin/dau")
    public String getDAU(Model model, @DateTimeFormat(pattern = "yyy-MM-dd") Date start, @DateTimeFormat(pattern = "yyyy-MM-dd") Date end) {
        long calculatedDAU = dataService.calculateDAU(start, end);
        model.addAttribute("calculatedDAU", calculatedDAU);
        model.addAttribute("DAUStart", start);
        model.addAttribute("DAUEnd", end);
        // 内部转发，此时算是一个请求，且model中数据可以携带
        // 转发方式跟本Controller接收方式一样都是post方式
        return "forward:/admin/data";
    }

}
