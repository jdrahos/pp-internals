package pack1;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * ReloadableProperty
 *
 * @author Pavel Moukhataev
 */
//@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReloadableProperty {
}
