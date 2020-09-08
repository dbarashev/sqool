import Vue from 'vue';
import App from './App.vue';

Vue.config.productionTip = false;

window.firebaseConfig = {
  apiKey: 'AIzaSyB7jtLPXuc0Oof7-HW21WgPLqHnXBdkFJc',
  authDomain: 'dbms-class-2017.firebaseapp.com',
  databaseURL: 'https://dbms-class-2017.firebaseio.com',
  projectId: 'dbms-class-2017',
  storageBucket: 'dbms-class-2017.appspot.com',
  messagingSenderId: '351383352695',
  appId: '1:351383352695:web:0ad8df850245f79d305d72',
  measurementId: 'G-M5V9DWWKVV',
};

new Vue({
  render: (h) => h(App),
}).$mount('#app');
