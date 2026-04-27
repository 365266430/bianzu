package com.bianzu.bianzu_backend.config;

import com.alibaba.fastjson2.TypeReference;

import java.util.List;

public class DoubleListConverter extends JsonListConverter<Double> {
    @Override
    protected TypeReference<List<Double>> getTypeReference() {
        return new TypeReference<>() {
        };
    }
}
