package com.bianzu.bianzu_backend.processor;
import com.bianzu.bianzu_backend.model.SimulationContext;

public interface SimulationProcessor {
    /**
     * 执行逻辑
     * @param context 当前帧的上下文数据
     */
    void process(SimulationContext context);
}