package pack1;

import org.springframework.beans.factory.FactoryBean;

import java.util.Properties;

/**
 * Props1
 *
 * @author Pavel Moukhataev
 */
public class Props1 implements FactoryBean<Properties> {

    @Override
    public Properties getObject() throws Exception {
        Properties properties = new Properties();
        properties.put("key1", "v1");
        properties.put("key2", "v2");
        properties.put("aaa", "v2-aaa-v2");
        properties.put("bbb", "v1-bbb-v1");
        return properties;
    }

    @Override
    public Class<?> getObjectType() {
        return Properties.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
