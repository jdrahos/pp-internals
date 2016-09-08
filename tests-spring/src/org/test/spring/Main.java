package org.test.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Pavel Moukhataev
 */
public class Main {

    public static void main(String[] args) {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("/spring-application-context.xml");

        Bean1 bean1 = applicationContext.getBean(Bean1.class);

        System.out.println(bean1);
    }
}
