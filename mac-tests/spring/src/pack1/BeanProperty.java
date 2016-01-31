package pack1;

/**
 * BeanProperty
 *
 * @author Pavel Moukhataev
 */
public class BeanProperty {
    enum Type {property, field}

    private String beanName;
    private String propertyName;
    private String propertyValue;
    private Type type;

    public BeanProperty(String beanName, String propertyName, String propertyValue) {
        this.beanName = beanName;
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    public String getBeanName() {
        return beanName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
