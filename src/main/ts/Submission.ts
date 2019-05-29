import {Component, Provide, Vue} from 'vue-property-decorator';
import ReviewPage from './components/ReviewPage';

@Component({
    components: {
        ReviewPage,
    },
})
export default class Submission extends Vue {

    @Provide()
    public reviewPage(): ReviewPage {
        return this.$refs.reviewPage as ReviewPage;
    }
}
