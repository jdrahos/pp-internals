package pack1;

import org.springframework.beans.factory.FactoryBean;

import java.util.Properties;

/**
 * Props1
 *
 * @author Pavel Moukhataev
 */
public class Props2 implements FactoryBean<Properties> {

    @Override
    public Properties getObject() throws Exception {
        Properties properties = new Properties();
        properties.put("key3", "v3");
        properties.put("key4", "v4");
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
