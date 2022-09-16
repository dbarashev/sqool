/*
 * Copyright (C) 2018 BarD Software s.r.o
 */

import {Landing, UserGet} from '../Routes';
import {User} from './Auth';
import * as firebase from 'firebase/app';
import 'firebase/auth';

declare global {
    interface Window {
        firebaseConfig: any;
    }
}

function firebaseId(id: string): string {
    return `firebase:::${id}`;
}

class FirebaseUser implements User {
    constructor(private readonly principal: firebase.User) {}
    public id(): string {
        return firebaseId(this.principal.uid);
    }
    public idToken(): Promise<string> {
        return this.principal.getIdToken();
    }

    public displayName(): string {
        return this.principal.displayName || this.principal.email || this.principal.uid;
    }

    public email(): string | null {
        return this.principal.email;
    }

    public isEmailVerified(): boolean {
        return this.principal.emailVerified;
    }

    public sendEmailVerification(redirectUrl: string) {
        this.principal.sendEmailVerification({
            url: redirectUrl,
        });
    }
}

export function signOut(redirect = true): Promise<void> {
    return firebase.auth().signOut()
        .then(() => {
            if (redirect) {
                Landing.redirect();
            }
        })
        .catch((e) => console.error(e));
}

export function getUser(): User | undefined {
    const user = firebase.auth().currentUser;
    if (user === undefined) {
        return undefined;
    }

    return new FirebaseUser(user!);
}

let app: firebase.app.App | undefined;

export function initFirebase() {
    if (!app) {
        // Initialize Firebase
        app = firebase.initializeApp(window.firebaseConfig);
    }
}

export function init(): Promise<User> {
    initFirebase();
    return new Promise<User>((resolve, reject) => {
        firebase.auth().onAuthStateChanged((user) => {
            if (user === null || user === undefined) {
                reject();
                return;
            }
            // Update idToken when it expires
            firebase.auth().onIdTokenChanged((newUser) => {
                if (newUser !== null) {
                    console.debug('idToken has changed');
                    newUser.getIdToken().then((newToken) => {
                        UserGet.invoke({
                            id: firebaseId(user.uid),
                            forceCreate: true,
                            email: newUser.email || '',
                            displayName: newUser.displayName,
                        }).then( () => resolve(new FirebaseUser(newUser))).catch( (err) => {
                            console.error(err);
                            reject('Failed to get or create user');
                        });

                    });
                }
            });
        });
    });
}

