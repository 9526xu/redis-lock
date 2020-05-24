package com.example.lock;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author andyXu xu9529@gmail.com
 * @date 2020/5/23
 */
public class EasyTest {

    @SneakyThrows
    @Test
    public void test() {
        Object o1 = true ? new Integer(1) : new Double(2.0);

        Object o2;

        if (true)
            o2 = new Integer(1);
        else
            o2 = new Double(2.0);
        System.out.println(o1);
        System.out.println(o2);

        TimeUnit.HOURS.sleep(1L);

        Map map = new HashMap();
        Boolean b = Boolean.valueOf(map == null ? false : ((Boolean)map.get("test")).booleanValue());
        System.out.println(b);

    }
}
