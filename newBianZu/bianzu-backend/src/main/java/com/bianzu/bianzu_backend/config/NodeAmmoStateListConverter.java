package com.bianzu.bianzu_backend.config;

import com.alibaba.fastjson2.TypeReference;
import com.bianzu.bianzu_backend.model.WeaponNode;

import java.util.List;

public class NodeAmmoStateListConverter extends JsonListConverter<WeaponNode.NodeAmmoState> {
    @Override
    protected TypeReference<List<WeaponNode.NodeAmmoState>> getTypeReference() {
        return new TypeReference<>() {
        };
    }
}
