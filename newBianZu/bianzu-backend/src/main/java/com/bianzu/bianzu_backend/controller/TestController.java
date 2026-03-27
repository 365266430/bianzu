package com.bianzu.bianzu_backend.controller;

import com.bianzu.bianzu_backend.common.Result;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {
    @GetMapping("/connection")
    public Result<TestDate> TestConnection(){
        return Result.success(new TestDate("test_one",1));
    }

    @Data
    @AllArgsConstructor
    private class  TestDate{
        public String testId;
        public int testNum;
    }
}
