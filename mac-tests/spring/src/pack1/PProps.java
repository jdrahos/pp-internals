package pack1;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * PProps
 *
 * @author Pavel Moukhataev
 */
@Component
public class PProps extends InstantiationAwareBeanPostProcessorAdapter
        implements BeanFactoryPostProcessor, ApplicationContextAware, BeanFactoryAware
{
    private static final Log log = LogFactory.getLog(PProps.class);

    private List<BeanProperty> beanPropertyList = new ArrayList<>();

    private ApplicationContext context;
    private BeanFactory beanFactory;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.debug("Autowire context: " + applicationContext);
        context = applicationContext;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        log.debug("Post process beans");
        String[] beanNames = beanFactory.getBeanDefinitionNames();
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        for (String beanName : beanNames) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            if (beanDefinition.getBeanClassName().equals(BeanProperty.class.getName())) {
                log.debug("Property bean: " + beanDefinition);
                ConstructorArgumentValues constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
                ConstructorArgumentValues.ValueHolder confBeanName = constructorArgumentValues.getIndexedArgumentValue(0, String.class);
                ConstructorArgumentValues.ValueHolder property = constructorArgumentValues.getIndexedArgumentValue(1, String.class);
                ConstructorArgumentValues.ValueHolder value = constructorArgumentValues.getIndexedArgumentValue(2, MyString.class);
                MyString value1 = (MyString) value.getValue();
                BeanProperty e = new BeanProperty(confBeanName.getValue().toString(), property.getValue().toString(), value1.getString());
                e.setType(BeanProperty.Type.property);
                beanPropertyList.add(e);
                registry.removeBeanDefinition(beanName);
            }
        }
    }

    public void onPropertiesUpdate(Properties properties) throws Exception {
        PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("${", "}", ":", true);
        for (BeanProperty beanProperty : beanPropertyList) {
            Object bean = context.getBean(beanProperty.getBeanName());
            BeanWrapper wrapper = new BeanWrapperImpl(bean);
            String value = helper.replacePlaceholders(beanProperty.getPropertyValue(), properties);
            log.debug("Set [" + beanProperty.getPropertyName() + "] = " + value);
            if (beanProperty.getType() == BeanProperty.Type.property) {
                PropertyDescriptor propertyDescriptor = wrapper.getPropertyDescriptor(beanProperty.getPropertyName());
                Method writeMethod = propertyDescriptor.getWriteMethod();
                writeMethod.invoke(bean, value);
            } else {
                Field field = bean.getClass().getDeclaredField(beanProperty.getPropertyName());
                field.setAccessible(true);
                ReflectionUtils.makeAccessible(field);

                /*
                StandardEvaluationContext context = new StandardEvaluationContext();
                ExpressionParser parser = new SpelExpressionParser();
                context.setBeanResolver(new BeanFactoryResolver(this.beanFactory));
                Expression expression = parser.parseExpression(value);
// or "@someOtherBean.data"
                final String value = expression.getValue(context, String.class);
                */
                field.set(bean, value);
            }
        }
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        log.debug("Post process " + beanName);
        ReflectionUtils.doWithFields(bean.getClass(), field -> {
            ReloadableProperty annotation = field.getAnnotation(ReloadableProperty.class);
            Value valueAnnotation = field.getAnnotation(Value.class);
            if (annotation != null && valueAnnotation != null) {
                String value = valueAnnotation.value();
                BeanProperty e = new BeanProperty(beanName, field.getName(), value);
                e.setType(BeanProperty.Type.field);
                beanPropertyList.add(e);
            }
        });
        return true;
    }
}
