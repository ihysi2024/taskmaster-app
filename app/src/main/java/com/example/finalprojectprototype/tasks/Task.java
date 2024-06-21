package com.example.finalprojectprototype.tasks;


import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.PropertyName;

import java.util.Date;

public class Task {
    private String name;
    private String desc;
    private Date dueDate;

    private UrgencyLevel urgency;

    @PropertyName("name")
    public String getName() {
        return name;
    }

    @PropertyName("name")
    public void setName(String name) {
        this.name = name;
    }

    @PropertyName("desc")
    public String getDesc() {
        return desc;
    }

    @PropertyName("desc")
    public void setDesc(String desc) {
        this.desc = desc;
    }

    @PropertyName("dueDate")
    public Date getDueDate() {
        return dueDate;
    }

    @PropertyName("dueDate")
    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    @PropertyName("urgency")
    public UrgencyLevel getUrgency() {
        return urgency;
    }

    @PropertyName("urgency")
    public void setUrgency(UrgencyLevel urgency) {
        this.urgency = urgency;
    }

    public enum UrgencyLevel {
        LEVEL_1(1),
        LEVEL_2(2),
        LEVEL_3(3),
        LEVEL_4(4),
        LEVEL_5(5);

        private final int value;

        UrgencyLevel(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public Task(String name, String desc, Date dueDate, UrgencyLevel urgency) {
        this.name = name;
        this.desc = desc;
        this.dueDate = dueDate;
        this.urgency = urgency;
    }


}
