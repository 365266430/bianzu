package com.bianzu.bianzu_backend.controller;

import com.bianzu.bianzu_backend.algorithm.dqn.DqnTrainingService;
import com.bianzu.bianzu_backend.algorithm.dqn.DqnModelPersistenceService;
import com.bianzu.bianzu_backend.algorithm.dqn.DqnRuntimeConfigService;
import com.bianzu.bianzu_backend.algorithm.dqn.DqnTrainingLogService;
import com.bianzu.bianzu_backend.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/dqn", "/api/dqn"})
public class DqnController {

    @Autowired
    private DqnTrainingService dqnTrainingService;

    @Autowired
    private DqnModelPersistenceService dqnModelPersistenceService;

    @Autowired
    private DqnRuntimeConfigService dqnRuntimeConfigService;

    @Autowired
    private DqnTrainingLogService dqnTrainingLogService;

    @PostMapping("/reset")
    public Result<Map<String, Object>> reset(@RequestParam(defaultValue = "true") boolean deleteSavedModel) {
        dqnTrainingService.resetTrainingState(deleteSavedModel);
        return Result.success(Map.of(
                "reset", true,
                "deleteSavedModel", deleteSavedModel,
                "replaySize", dqnTrainingService.replaySize()
        ));
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        return Result.success(Map.of(
                "training", dqnTrainingService.trainingStatus(),
                "modelFile", dqnModelPersistenceService.modelFileStatus(),
                "runtime", dqnRuntimeConfigService.status(),
                "trainingLog", dqnTrainingLogService.status()
        ));
    }

    @PostMapping("/mode")
    public Result<Map<String, Object>> setMode(@RequestParam(defaultValue = "TRAIN") String mode) {
        return Result.success(dqnRuntimeConfigService.setMode(mode));
    }

    @PostMapping("/save")
    public Result<Map<String, Object>> saveModel() {
        dqnModelPersistenceService.saveModel();
        return Result.success(Map.of(
                "saved", true,
                "modelFile", dqnModelPersistenceService.modelFileStatus()
        ));
    }

    @PostMapping("/load")
    public Result<Map<String, Object>> loadModel() {
        boolean loaded = dqnModelPersistenceService.loadModel();
        return Result.success(Map.of(
                "loaded", loaded,
                "modelFile", dqnModelPersistenceService.modelFileStatus()
        ));
    }

    @PostMapping("/train-batch")
    public Result<Map<String, Object>> trainBatch(
            @RequestParam(defaultValue = "32") int batchSize,
            @RequestParam(defaultValue = "0.001") double learningRate,
            @RequestParam(defaultValue = "0.95") double gamma,
            @RequestParam(defaultValue = "10") int targetUpdateFreq) {
        double avgAbsError = dqnTrainingService.trainBatch(batchSize, learningRate, gamma, targetUpdateFreq);
        return Result.success(Map.of(
                "avgAbsError", avgAbsError,
                "batchSize", batchSize,
                "learningRate", learningRate,
                "gamma", gamma,
                "targetUpdateFreq", targetUpdateFreq,
                "replaySize", dqnTrainingService.replaySize(),
                "trainStep", dqnTrainingService.trainStep()
        ));
    }
}
