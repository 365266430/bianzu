package com.bianzu.bianzu_backend.config;

import com.alibaba.fastjson2.TypeReference;

import java.util.List;

public class StringListConverter extends JsonListConverter<String> {
    @Override
    protected TypeReference<List<String>> getTypeReference() {
        return new TypeReference<>() {
        };
    }
}
