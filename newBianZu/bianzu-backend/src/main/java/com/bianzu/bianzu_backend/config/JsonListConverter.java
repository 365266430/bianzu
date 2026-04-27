package com.bianzu.bianzu_backend.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import jakarta.persistence.AttributeConverter;

import java.util.ArrayList;
import java.util.List;

public abstract class JsonListConverter<T> implements AttributeConverter<List<T>, String> {

    @Override
    public String convertToDatabaseColumn(List<T> attribute) {
        if (attribute == null) {
            return "[]";
        }
        return JSON.toJSONString(attribute);
    }

    @Override
    public List<T> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        return JSON.parseObject(dbData, getTypeReference());
    }

    protected abstract TypeReference<List<T>> getTypeReference();
}
