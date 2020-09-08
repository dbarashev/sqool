/*
 * Copyright (C) 2018 BarD Software s.r.o
 */
import {Landing} from '../Routes';
import * as FirebaseAuth from './FirebaseAuth';

let FirebaselessAuth;

export interface User {
    id(): string;
    idToken(): Promise<string>;
    displayName(): string;
    email(): string | null;
    isEmailVerified(): boolean;
    sendEmailVerification(redirectUrl: string);
}

export function signOut(): Promise<void> {
    if (FirebaselessAuth) {
        FirebaselessAuth.signOut();
    }

    return FirebaseAuth.signOut();
}

function getUserOrUndefined(): User | undefined {
    if (FirebaselessAuth && FirebaselessAuth.getUser()) {
        return FirebaselessAuth.getUser();
    }

    return FirebaseAuth.getUser();
}

export function getUser(): User {
    const user = getUserOrUndefined();

    if (user === undefined) {
        Landing.redirect();
    } else {
        return user;
    }

}

export function init(): Promise<User> {
    return import('./FirebaselessAuth').then((m) => {
        const devUser = m.getUser();
        if (devUser !== undefined) {
            FirebaselessAuth = m;
            // SetIdToken.invoke({
            //     user: devUser.id(),
            // });
            return Promise.resolve(devUser);
        } else {
            throw Error('Firebaseless user not found.');
        }
    }).catch(() => FirebaseAuth.init());
}

$('.btn-logout').on('click', signOut);
