/*
 * Copyright (C) 2018 BarD Software s.r.o
 */
import * as Auth from './Auth';
import * as firebase from 'firebase/app';
import 'firebase/auth';
import * as firebaseui from 'firebaseui';
import {Dashboard} from '../Routes';

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

function showUi() {
    const ui = new firebaseui.auth.AuthUI(firebase.auth());
    const uiConfig = {
        callbacks: {
            signInSuccessWithAuthResult(authResult) {
                console.dir(authResult);
                return true;
            },
            uiShown() {
                $('#loader').hide();
            },
        },
        signInFlow: 'redirect',
        signInSuccessUrl: '/me2',
        signInOptions: [
            {
                provider: firebase.auth.GoogleAuthProvider.PROVIDER_ID,
                customParameters: {
                    // Forces account selection even when one account
                    // is available.
                    prompt: 'select_account',
                },
            },
            {
                provider: firebase.auth.GithubAuthProvider.PROVIDER_ID,
            },
        ],
        // tosUrl: AboutTos.absoluteUrl(),
    };
    ui.start('#firebaseui-auth-container', uiConfig);
}

Auth.init().then((user) => {
    console.debug('User: ' + user);
    Dashboard.redirect();
}).catch(() => {
    showUi();
});


