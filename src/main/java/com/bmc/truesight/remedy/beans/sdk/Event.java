package com.bmc.truesight.remedy.beans.sdk;

import java.util.ArrayList;
import java.util.List;


public class Event {

    @Override
	public String toString() {
		return "Event [title=" + title + ", message=" + message + ", severity=" + severity + ", tags=" + tags
				+ ", host=" + host + ", source=" + source + "]";
	}

	public enum EventSeverity {
        INFO, WARN, ERROR, CRITICAL; 
    }
    
    private String title;
    private String message;
    private EventSeverity severity;  
    private List<String> tags;
    private String host;
    private String source;

    public Event(final String title, final String message) {
       this.severity = EventSeverity.INFO; 
       this.title = title;
       this.message = message;
       this.tags = new ArrayList<String>();
    }

    public Event(final EventSeverity severity, final String title, final String message, final String host, final String source, final List<String> tags) {
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.host = host;
        this.source = source;
        this.tags = tags;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public EventSeverity getSeverity() {
        return severity;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getHost() {
        return host;
    }

    public String getSource() {
        return source;
    }

    public boolean hasTags() {
        return tags != null && tags.size() > 0;
    }
}
