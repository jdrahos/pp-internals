package com.example;

import javax.ws.rs.QueryParam;

/**
 * MyPojo
 *
 * @author Pavel Moukhataev
 */
public class MyPojo {
    private String s1;

    @QueryParam("p2")
    private String s2;


    public MyPojo() {
        System.out.println("New Pojo");
        new Throwable().printStackTrace(System.out);
    }

    public String getS1() {
        return s1;
    }

    @QueryParam("p1")
    public void setS1(String s1) {
        System.out.println("Set s1");
        new Throwable().printStackTrace(System.out);
        this.s1 = s1;
    }

    public String getS2() {
        return s2;
    }

    public void setS2(String s2) {
        this.s2 = s2;
    }
}
