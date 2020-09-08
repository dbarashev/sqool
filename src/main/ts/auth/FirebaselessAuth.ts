/*
 * Copyright (C) 2018 BarD Software s.r.o
 */

import {Landing} from '../Routes';
import {User} from './Auth';

export class FirebaselessUser implements User {
    constructor(private readonly refid: string) {}

    public id(): string {
        return this.refid;
    }

    public idToken(): Promise<string> {
        return Promise.resolve(this.refid);
    }

    public displayName(): string {
        return this.refid;
    }

    public email(): string | null {
        return null;
    }

    public isEmailVerified(): boolean {
        return true;
    }

    public sendEmailVerification(/*redirectUrl: string*/) {
        console.log('Sending email verification');
    }
}

export function signOut() {
    Landing.redirect();
}

export function getUser(): User | undefined {
    const refid = new URL(document.URL).searchParams.get('user');

    if (refid == null || !refid.startsWith('dev:::')) {
        return undefined;
    }

    return new FirebaselessUser(refid);
}
