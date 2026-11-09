package org.beurer.bf990;

import java.time.LocalDate;

/** Decide whether a birth-date update is safe using the last verified value. */
final class ProfileDateDecision {
    enum Action { VERIFIED, WRITE, RECOVER_REVISION, MISMATCH }
    static Action decide(LocalDate actual,LocalDate expected,LocalDate desired,boolean dirty,boolean initialRead){
        if(desired==null)return actual.equals(expected)?Action.VERIFIED:Action.MISMATCH;
        if(actual.equals(desired))return dirty?Action.RECOVER_REVISION:Action.VERIFIED;
        return initialRead&&actual.equals(expected)?Action.WRITE:Action.MISMATCH;
    }
}
