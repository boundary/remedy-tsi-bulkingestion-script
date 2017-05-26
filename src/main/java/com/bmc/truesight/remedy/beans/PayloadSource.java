package com.bmc.truesight.remedy.beans;

public class PayloadSource {

    private String name;
    private String type;
    private String ref;

    public PayloadSource(PayloadSource sender) {
        this.setName(sender.getName());
        this.setRef(sender.getRef());
        this.setType(sender.getType());
    }

    public PayloadSource() {
        // default constructor
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }
}
