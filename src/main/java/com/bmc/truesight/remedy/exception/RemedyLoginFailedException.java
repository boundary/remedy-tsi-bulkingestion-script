package com.bmc.truesight.remedy.exception;

public class RemedyLoginFailedException extends Exception {

    private static final long serialVersionUID = -4739634227509447336L;

    public RemedyLoginFailedException() {
        super();
    }

    public RemedyLoginFailedException(String message) {
        super(message);
    }

}
