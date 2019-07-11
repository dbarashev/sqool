import Vue from 'vue';
import App from './App.vue';
import BFormSelect from "bootstrap-vue";

Vue.config.productionTip = false;
Vue.use(BFormSelect);

new Vue({
  render: (h) => h(App),
}).$mount('#app');
