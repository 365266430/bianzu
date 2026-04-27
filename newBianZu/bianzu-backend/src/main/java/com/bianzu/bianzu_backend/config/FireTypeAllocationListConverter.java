package com.bianzu.bianzu_backend.config;

import com.alibaba.fastjson2.TypeReference;
import com.bianzu.bianzu_backend.model.WeaponType;

import java.util.List;

public class FireTypeAllocationListConverter extends JsonListConverter<WeaponType.FireTypeAllocation> {
    @Override
    protected TypeReference<List<WeaponType.FireTypeAllocation>> getTypeReference() {
        return new TypeReference<>() {
        };
    }
}
