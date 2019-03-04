package com.spring.demo.service;

import com.spring.mvcframework.annotation.DwbService;

@DwbService
public class DemoService {
    public String get(String name) {
        return "hello" + name;
    }
}
