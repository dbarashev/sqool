import Vue from 'vue';
import Submission from './Submission.vue';

Vue.config.productionTip = false;

new Vue({
    render: (h) => h(Submission),
}).$mount('#submission');
