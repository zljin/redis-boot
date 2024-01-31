package com.zou.pojo;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author leonard
 * @date 2022/11/12
 * @Description compute cost time
 */
public class TimeLag {

    private Date start;
    private Date end;

    public TimeLag() {
        start = new Date();
    }

    public String cost() {
        end = new Date();
        long c = end.getTime() - start.getTime();

        String s = new StringBuffer().append("cost ").append(c).append(" milliseconds (").append(c / 1000).append(" seconds).").toString();
        return s;
    }

    public static void main(String[] args) throws InterruptedException {
        TimeLag t = new TimeLag();
        TimeUnit.SECONDS.sleep(2);
        System.out.println(t.cost());
    }

}
