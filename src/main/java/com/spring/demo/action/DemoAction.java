package com.spring.demo.action;

import com.spring.demo.service.DemoService;
import com.spring.mvcframework.annotation.DwbAutowired;
import com.spring.mvcframework.annotation.DwbController;
import com.spring.mvcframework.annotation.DwbRequestMapping;
import com.spring.mvcframework.annotation.DwbRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@DwbController
@DwbRequestMapping("/demo")
public class DemoAction {

    //此注解有问题 待核查
    @DwbAutowired
    private DemoService demoService;


    @DwbRequestMapping("/get")
    public void get(HttpServletRequest req, HttpServletResponse res,
                    @DwbRequestParam("name") String name) {
        System.out.println(" 参数：===================="+name);


       // String result = demoService.get(name);

        String result = "hello" + name;
        try {
            res.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @DwbRequestMapping("/add")
    public void add(HttpServletRequest req, HttpServletResponse res,
                    @DwbRequestParam("number1") Integer number1, @DwbRequestParam("number2") Integer number2) {
        try {
            res.getWriter().write(number1 + "+" + number2 + "=" + (number1 + number2));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @DwbRequestMapping("delete")
    public void delete(HttpServletRequest req, HttpServletResponse res,
                       @DwbRequestParam("id") Integer id) {

    }

}
