import Vue from 'vue';
import App from './App.vue';
import Submission from './Submission.vue';

Vue.config.productionTip = false;

new Vue({
  render: (h) => h(App),
}).$mount('#app');

new Vue({
  render: (h) => h(Submission),
}).$mount('#submission');
