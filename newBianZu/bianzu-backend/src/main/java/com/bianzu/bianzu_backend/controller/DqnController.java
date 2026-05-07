package com.bianzu.bianzu_backend.controller;

import com.bianzu.bianzu_backend.algorithm.dqn.DqnTrainingService;
import com.bianzu.bianzu_backend.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dqn")
public class DqnController {

    @Autowired
    private DqnTrainingService dqnTrainingService;

    @PostMapping("/reset")
    public Result<Map<String, Object>> reset(@RequestParam(defaultValue = "true") boolean deleteSavedModel) {
        dqnTrainingService.resetTrainingState(deleteSavedModel);
        return Result.success(Map.of(
                "reset", true,
                "deleteSavedModel", deleteSavedModel,
                "replaySize", dqnTrainingService.replaySize()
        ));
    }
}
