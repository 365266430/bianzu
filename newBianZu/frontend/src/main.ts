import { createApp } from 'vue';
import App from './App.vue';
import { createPinia } from 'pinia'
import LeafletPlugin from './plugins/leaflet';

const app = createApp(App);
app.use(LeafletPlugin);
app.use(createPinia());
app.mount('#app');