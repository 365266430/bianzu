import L from "leaflet";

export const createCircleWithMarker = (
  map: L.Map,
  latlng: L.LatLngExpression,
  radius: number,
  circleOptions: Partial<L.CircleOptions> = {},
  markerOptions: Partial<L.MarkerOptions & { dotOptions?: L.CircleMarkerOptions }> = {}
): { circle: L.Circle; marker: L.CircleMarker } => ({
  // 创建圆形并合并配置
  circle: L.circle(latlng, {
    radius,
    color: '#3388ff',
    fillColor: '#3388ff',
    fillOpacity: 0.2,
    weight: 2,
    ...circleOptions
  }).addTo(map),

  // 创建圆点标记并合并默认配置与用户配置
  marker: L.circleMarker(latlng, {
    radius: 4,
    color: '#ff0000',
    fillColor: '#ff0000',
    fillOpacity: 1,
    weight: 1,
    ...markerOptions.dotOptions
  }).addTo(map)

});



// 定义信息弹窗内容的类型
interface PopupContent {
  title: string;
  [key: string]: string | number | boolean; // 支持任意其他信息字段
}

/**
 * 创建带点击交互的圆形和标记点
 * @param map Leaflet地图实例
 * @param latlng 中心点坐标
 * @param radius 圆形半径（米）
 * @param circleOptions 圆形样式配置
 * @param markerOptions 标记点样式配置
 * @param popupContent 点击时显示的信息内容
 * @param onClick 自定义点击事件处理函数（可选）
 * @returns 包含圆形和标记点实例的对象
 */
export const createZone = (
  map: L.Map,
  latlng: L.LatLngExpression,
  radius: number,
  circleOptions: Partial<L.CircleOptions> = {},
  markerOptions: Partial<L.MarkerOptions & { dotOptions?: L.CircleMarkerOptions }> = {},
  popupContent?: PopupContent
): { circle: L.Circle; marker: L.CircleMarker } => {
  // 创建圆形并合并配置
  const circle = L.circle(latlng, {
    radius,
    color: '#3388ff',
    fillColor: '#3388ff',
    fillOpacity: 0.2,
    weight: 2,
    ...circleOptions
  }).addTo(map);

  // 创建圆点标记并合并默认配置与用户配置
  const marker = L.circleMarker(latlng, {
    radius: 4,
    color: '#3388ff',
    fillColor: '#3388ff',
    fillOpacity: 1,
    weight: 1,
    ...markerOptions.dotOptions
  }).addTo(map);

  // // 如果提供了弹窗内容，则绑定弹窗
  // if (popupContent) {
  //   // 统一处理坐标获取，兼容数组和对象形式
  //   const lat = Array.isArray(latlng) ? latlng[0] : latlng.lat;
  //   const lng = Array.isArray(latlng) ? latlng[1] : latlng.lng;

  //   // 构建弹窗HTML内容
  //   let popupHtml = `<div class="custom-popup"><h3>${popupContent.title}</h3><ul>`;

  //   // 遍历所有信息字段并添加到弹窗
  //   Object.entries(popupContent).forEach(([key, value]) => {
  //     if (key !== 'title') {
  //       popupHtml += `<li><strong>${key}：</strong>${value}</li>`;
  //     }
  //   });

  //   // 添加坐标信息（使用处理后的lat和lng）
  //   popupHtml += `<li><strong>坐标：</strong>${lat.toFixed(6)}, ${lng.toFixed(6)}</li>`;
  //   popupHtml += `</ul></div>`;

  //   // 为标记和圆形绑定弹窗
  //   marker.bindPopup(popupHtml);
  //   circle.bindPopup(popupHtml);
  // }

  // 不再绑定任何点击事件

  return { circle, marker };
};


/**
 * 创建蓝色敌方节点标记并添加点击事件
 * @param map Leaflet地图实例
 * @param latlng 节点坐标（数组或L.LatLng对象）
 * @param info 敌方节点信息
 * @returns 标记实例
 */
export const createEnemy = (
  map: L.Map,
  latlng: L.LatLngExpression,
  dotOptions: Partial<L.CircleMarkerOptions> = {}
): L.CircleMarker => {

  // 创建蓝色圆形节点，合并自定义dotOptions
  const marker = L.circleMarker(latlng, {
    radius: 8,
    color: '#d71920',
    fillColor: '#d71920',
    fillOpacity: 0.8,
    weight: 2,
    ...dotOptions
  }).addTo(map);

  return marker;
};


/**
 * 在两个节点之间添加虚线
 * @param map Leaflet地图实例
 * @param latlng1 起点坐标 [纬度, 经度]
 * @param latlng2 终点坐标 [纬度, 经度]
 * @param options 线条样式配置（可选）
 * @returns L.Polyline 实例
 */
export function createDashedLine(
  map: L.Map,
  latlng1: L.LatLngExpression,
  latlng2: L.LatLngExpression,
  options: L.PolylineOptions = {}
): L.Polyline {
  const polyline = L.polyline([latlng1, latlng2], {
    color: '#2986f2',
    weight: 2,
    dashArray: '8, 8', // 虚线样式
    ...options
  }).addTo(map);
  return polyline;
}
