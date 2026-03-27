/**
 * 统计所有保护区的火力资源类型（fireType）及对应数量（基于assignedWeapons内FireTypes）
 * @param zones 保护区数组
 * 返回格式：{ [zoneId]: Array<{ type: string, quantity: number }> }
 */
export function countFireResourcesByZone(zones: any[]) {
    const result: Record<string | number, Array<{ type: string, quantity: number }>> = {};
    zones.forEach(zone => {
        const fireTypeMap: Record<string, number> = {};
        if (zone.assignedWeapons && zone.assignedWeapons.length > 0) {
            zone.assignedWeapons.forEach((item: any) => {
                if (item.weapon.FireTypes && item.weapon.FireTypes.length > 0) {
                    item.weapon.FireTypes.forEach((ft: any) => {
                        const type = typeof ft.fireType.Type === 'string' ? ft.fireType.Type : String(ft.fireType.Type);
                        fireTypeMap[type] = (fireTypeMap[type] || 0) + (ft.quantity || 1) * (item.quantity || 1);
                    });
                }
            });
        }
        // 转换为数组形式
        result[zone.ID] = Object.entries(fireTypeMap).map(([type, quantity]) => ({
            type,
            quantity
        }));
    });
    return result;
}


/**
 * 获取单个保护区的所有火力资源类型属性及数量
 * @param zone 保护区对象
 * 返回格式：Array<{ fireUnit: FireUnit, quantity: number }>
 */
export function getZoneFireResource(zone: any) {
    const fireTypeMap: Map<any, number> = new Map();
    if (zone.assignedWeapons && zone.assignedWeapons.length > 0) {
        zone.assignedWeapons.forEach((item: any) => {
            if (item.weapon.FireTypes && item.weapon.FireTypes.length > 0) {
                item.weapon.FireTypes.forEach((ft: any) => {
                    // 以 FireUnit 对象为 key
                    const key = ft.fireType;
                    const addQty = (ft.quantity || 1) * (item.quantity || 1);
                    fireTypeMap.set(key, (fireTypeMap.get(key) || 0) + addQty);
                });
            }
        });
    }
    // 返回数组，包含 fireUnit 对象和数量
    return Array.from(fireTypeMap.entries()).map(([fireUnit, quantity]) => ({
        fireUnit,
        quantity
    }));
}