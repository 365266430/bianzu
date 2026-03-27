package com.bianzu.bianzu_backend.controller;

import com.bianzu.bianzu_backend.common.Result;
import com.bianzu.bianzu_backend.service.SimulationEngineService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sim/control")
//// 允许跨域 (让前端能调通)
//@CrossOrigin(origins = "*")
public class SimulationControlController {

    /**
     * 控制仿真状态
     * POST /api/sim/control/start  -> 开始
     * POST /api/sim/control/stop   -> 暂停
     */
    @PostMapping("/{action}")
    public Result<String> controlSimulation(@PathVariable String action) {

        if ("start".equalsIgnoreCase(action)) {
            if (SimulationEngineService.isRunning) {
                return Result.failed("仿真启动失败");
            }
            SimulationEngineService.isRunning = true;
            System.out.println(">>> 收到指令: 仿真已启动 [START]");
            return Result.success("success");
        }

        else if ("stop".equalsIgnoreCase(action)) {
            if (!SimulationEngineService.isRunning) {
                return Result.failed("仿真暂停失败");
            }
            SimulationEngineService.isRunning = false;
            System.out.println(">>> 收到指令: 仿真已暂停 [STOP]");
            return Result.success("success");
        }

        return Result.failed("error: unknown action (use 'start' or 'stop')");
    }
}
