package org.yuezhikong.javaim.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexController {

    @RequestMapping("/index") // 访问路径
    public ModelAndView toIndex() {
        // 返回templates目录下index.html
        ModelAndView view = new ModelAndView("index");
        return view;
    }
}
