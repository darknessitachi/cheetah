package org.cheetah.fighter.sample;

import org.cheetah.fighter.EventResult;
import org.cheetah.ioc.BeanFactory;
import org.cheetah.ioc.spring.SpringBeanFactoryProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.TimeUnit;

/**
 * Created by Max on 2016/7/29.
 */
public class DomainEventPublisherTest {
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("/META-INF/application.xml");
        SpringBeanFactoryProvider provider = new SpringBeanFactoryProvider(context);
        BeanFactory.setBeanFactoryProvider(provider);

        performance();
//        publish();
    }

    /**
     * 性能测试
     */
    public static void performance() {

            Thread[] threads = new Thread[10];
            for (int i = 0; i < 10; i++) {
                threads[i] = new Thread(() -> {
                    while (true) {
                        System.out.println("put");
                        EventResult result = org.cheetah.fighter.api.DomainEventPublisher.publish(
                                new DomainEventTest("huahng"), true, 1
                        );
                        org.cheetah.fighter.api.DomainEventPublisher.publish(
                                new DomainEventTest2("huahng"), true, 1
                        );
                        //                    System.out.println(atomicLong2.incrementAndGet());
                    }
                });
                threads[i].start();
            }
    }

    /**
     *
     */
    public static void publish() {

        EventResult result = org.cheetah.fighter.api.DomainEventPublisher.publish(
                new DomainEventTest("huahng"), true, 1, TimeUnit.SECONDS
        );
//        EventResult result2 = DomainEventPublisher.publish(
//                new DomainEventTest2("huahng"), true
//        );
        System.out.println(result);
//        System.out.println(result2);
    }
}