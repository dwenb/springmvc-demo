package com.spring.demo.service;

import com.spring.mvcframework.annotation.DwbService;

@DwbService
public class DemoService implements IDemoService{

    public String get(String name) {
        return "hello" + name;
    }


}
