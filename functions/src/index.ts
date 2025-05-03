import * as admin from 'firebase-admin';
admin.initializeApp();

import { getLeagues, getSchedule, getMatch } from './lol-esports';

import { monitorMatchStateChanges } from './match-updates';

export {
    getLeagues,
    getSchedule,
    getMatch,
    monitorMatchStateChanges
};