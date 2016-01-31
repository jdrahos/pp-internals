package pack1;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * MyNamespaceHandler
 *
 * @author Pavel Moukhataev
 */
public class MyNamespaceHandler extends NamespaceHandlerSupport {
    private static final Log log = LogFactory.getLog(MyNamespaceHandler.class);


    @Override
    public void init() {
        log.debug("init()");

        registerBeanDefinitionDecorator("property", new BeanDefinitionDecorator() {
            @Override
            public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
                log.debug("decorate()");
                NamedNodeMap attributes = node.getAttributes();
                String name = attributes.getNamedItem("name").getNodeValue();
                String value = attributes.getNamedItem("value").getNodeValue();

                BeanDefinition beanDefinition = definition.getBeanDefinition();
                beanDefinition.getPropertyValues().add(name, value);
//                beanDefinition.setAttribute(name, value);
                Object source = beanDefinition.getSource();
                log.debug("Source=" + source);
                log.debug("Bean definition [" + definition.getClass().getSimpleName() + "]=" + definition);
                String beanName = definition.getBeanName();
                ConstructorArgumentValues propertyArgs = new ConstructorArgumentValues();
                propertyArgs.addIndexedArgumentValue(0, beanName, String.class.getName());
                propertyArgs.addIndexedArgumentValue(1, name, String.class.getName());
                propertyArgs.addIndexedArgumentValue(2, new MyString(value), MyString.class.getName());
                RootBeanDefinition propertyBean = new RootBeanDefinition(BeanProperty.class, propertyArgs, null);

                String propertyBeanName = "property#" + beanName + "#" + name;
                parserContext.getRegistry().registerBeanDefinition(propertyBeanName, propertyBean);
                /*
                parserContext.getReaderContext().fireComponentRegistered(
                        new BeanComponentDefinition(
                                propertyBean
                                , propertyBeanName));
                                */
                return definition;
            }
        });
    }



}
