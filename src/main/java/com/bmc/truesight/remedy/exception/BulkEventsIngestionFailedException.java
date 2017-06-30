package com.bmc.truesight.remedy.exception;

public class BulkEventsIngestionFailedException extends Exception {

    private static final long serialVersionUID = -4457538956003878309L;

    public BulkEventsIngestionFailedException() {
        super();
    }

    public BulkEventsIngestionFailedException(String message) {
        super(message);
    }

}
