package org.cheetah.bootstrap;

import org.cheetah.bootstrap.service.DemoService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by Max on 2016/8/13.
 */
//@ContextConfiguration("classpath:META-INF/applicationContext.xml")
//@RunWith(SpringJUnit4ClassRunner.class)
public class DubboDemoserviceTest {
    @Autowired
    private DemoService demoService;

    @Test
    public void get() {
        while (true) {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        System.out.println(demoService.get());
        }
    }

    @Test
    public void test() {
        String url = "http://www.baidu.com/test/w12?a=b#t";
        System.out.println(URI.create(url).getAuthority());
        System.out.println(URI.create(url).getQuery());
        System.out.println(URI.create(url).getPath());
        System.out.println(URI.create(url).getFragment());
        System.out.println(URI.create(url).getHost());
        System.out.println(URI.create(url).getRawPath());
        System.out.println(URI.create(url).getScheme());
        System.out.println(URI.create(url).getPort());
    }
}
