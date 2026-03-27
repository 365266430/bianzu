// plugins/leaflet.ts
import type { App } from 'vue';
import * as L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import markerIconPng from 'leaflet/dist/images/marker-icon.png';
import markerIcon2xPng from 'leaflet/dist/images/marker-icon-2x.png';
import markerShadowPng from 'leaflet/dist/images/marker-shadow.png';

// 解决Leaflet图标加载问题
// 使用类型断言来绕过类型检查
// delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2xPng,
  iconUrl: markerIconPng,
  shadowUrl: markerShadowPng,
});

// 自定义地图初始化函数
export const createMap = (
  container: string | HTMLElement,
  options?: L.MapOptions
): L.Map => {
  return L.map(container, {
    center: [39.90923, 116.397428],
    zoom: 9, // 添加缺失的zoom参数
    layers: [
      // 使用相对路径或绝对URL
      L.tileLayer('/tiles/{z}/{x}/{y}/tile.png', {
        attribution: '本地瓦片地图',
        maxZoom: 9,
        minZoom: 5,
        tileSize: 256
      })
    ],
    ...options
  });
};

// 自定义图标工厂函数
export const createCustomIcon = (options: L.IconOptions): L.Icon => {
  return L.icon({
    iconSize: [25, 41],
    // iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41],
    ...options
  });
};

// 创建带范围圈的标记
export const createMarkerWithRadius = (
  map: L.Map,
  latlng: L.LatLngExpression,
  icon: L.Icon | L.DivIcon,
  radius: number = 1000,
  circleOptions: Partial<L.CircleOptions> = {}
): { marker: L.Marker; circle: L.Circle } => {
  const marker = L.marker(latlng, { icon }).addTo(map);
  const circle = L.circle(latlng, {
    radius,
    color: '#0078FF',
    fillColor: '#0078FF',
    fillOpacity: 0.2,
    weight: 2,
    ...circleOptions,
  }).addTo(map);

  return { marker, circle };
};


// 在地图上绘制圆并标记圆心
// 修改 createCircleWithMarker 函数，添加使用点作为圆心的选项

/**
 * 在地图上绘制圆并标记圆心
 * @param map - Leaflet地图实例
 * @param latlng - 圆心坐标
 * @param radius - 圆的半径(米)
 * @param circleOptions - 圆的样式选项
 * @param markerOptions - 标记的选项，或者传入 useDot: true 使用点标记圆心
 * @returns 包含圆和标记的对象
 */
export const createCircleWithMarker = (
  map: L.Map,
  latlng: L.LatLngExpression,
  radius: number,
  circleOptions: Partial<L.CircleOptions> = {},
  markerOptions: Partial<L.MarkerOptions & { useDot?: boolean; dotOptions?: L.CircleMarkerOptions }> = {}
): { circle: L.Circle; marker: L.Marker | L.CircleMarker } => {
  // 创建圆
  const circle = L.circle(latlng, {
    radius,
    color: '#3388ff',
    fillColor: '#3388ff',
    fillOpacity: 0.2,
    weight: 2,
    ...circleOptions
  }).addTo(map);

  let marker: L.Marker | L.CircleMarker;

  // 判断是否使用点来表示圆心
  if (markerOptions.useDot) {
    const defaultDotOptions: L.CircleMarkerOptions = {
      radius: 4,
      color: '#ff0000',
      fillColor: '#ff0000',
      fillOpacity: 1,
      weight: 1
    };

    const dotOptions = { ...defaultDotOptions, ...markerOptions.dotOptions };
    marker = L.circleMarker(latlng, dotOptions).addTo(map);
  } else {
    // 创建默认标记
    marker = L.marker(latlng, {
      ...markerOptions
    }).addTo(map);
  }

  return { circle, marker };
};





export default {
  install(app: App) {
    app.config.globalProperties.$L = L;
    app.provide('L', L); // 提供注入，便于在setup中使用
  }
};