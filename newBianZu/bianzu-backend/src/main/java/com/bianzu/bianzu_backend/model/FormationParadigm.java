package com.bianzu.bianzu_backend.model;

/**
 * 编组范式
 * 支持空天地海四域的所有编组组合
 */
public enum FormationParadigm {

    // ==================== 同域编组 ====================
    AIR_AIR("空空编组"),
    GROUND_GROUND("地地编组"),
    SEA_SEA("海海编组"),
    SPACE_SPACE("天天编组"),

    // ==================== 跨域编组 ====================
    AIR_GROUND("空地编组"),
    AIR_SEA("空海编组"),
    AIR_SPACE("空天编组"),
    GROUND_SEA("地海编组"),
    GROUND_SPACE("地天编组"),
    SEA_SPACE("海天编组"),

    // ==================== 三域联合 ====================
    AIR_SEA_GROUND("空海地编组"),
    AIR_SPACE_GROUND("空天地编组"),
    AIR_SEA_SPACE("空海天编组"),
    GROUND_SEA_SPACE("地海天编组"),

    // ==================== 全域编组 ====================
    ALL("全域编组");

    private final String chineseName;

    FormationParadigm(String chineseName) {
        this.chineseName = chineseName;
    }

    public String getChineseName() {
        return chineseName;
    }
}
