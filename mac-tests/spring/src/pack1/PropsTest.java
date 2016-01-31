package pack1;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.annotation.PostConstruct;
import java.util.Properties;

/**
 * PropsTest
 *
 * @author Pavel Moukhataev
 */
public class PropsTest {
    private static final Log log = LogFactory.getLog(PropsTest.class);

    @Value("${key1}")
    private String p1;
    @Value("${key2}")
    private String p2;
    @Value("${key3}")
    private String p3;
    @Value("${key4}")
    private String p4;

    private String aaa;

    @Value("${bbb}")
    @ReloadableProperty
    private String bbb;

    @Qualifier("p1")
    @Autowired
    private Properties pr1;

    @PostConstruct
    private void init() {
        print();
    }

    private void print() {
        System.out.println("p1 = " + p1);
        System.out.println("p2 = " + p2);
        System.out.println("p3 = " + p3);
        System.out.println("p4 = " + p4);
        System.out.println("pr1 = " + pr1);
        System.out.println("aaa = " + aaa);
        System.out.println("bbb = " + bbb);
    }

    public static void main(String[] args) throws Exception {
        // open/read the application context file
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("spring-context.xml");

        PropsTest bean = ctx.getBean(PropsTest.class);
        bean.print();

        PProps pProps = ctx.getBean(PProps.class);
        Properties properties = new Properties();
        properties.put("aaa", "bbb");
        properties.put("bbb", "new!bbb");
        pProps.onPropertiesUpdate(properties);

        bean.print();
    }

    public void setAaa(String aaa) {
        log.debug("Set aaa = " + aaa);
        this.aaa = aaa;
    }
}
