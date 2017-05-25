package com.bmc.truesight.remedy.beans.sdk;


public class EventSinkStandardOutput implements EventSink {

    //private EventFormatter formatter;

    public EventSinkStandardOutput () {
      //  formatter = new EventFormatter();
    }

    public void emit(Event event) {
        System.out.println(event.toString());
    }

}
