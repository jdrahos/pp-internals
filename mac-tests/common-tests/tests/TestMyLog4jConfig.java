import org.apache.log4j.xml.DOMConfigurator;
import org.junit.Test;

/**
 * TestMyLog4jConfig
 *
 * @author Pavel Moukhataev
 */
public class TestMyLog4jConfig {

    @Test
    public void testConfig() {
        DOMConfigurator.configure("/Public/Projects/PulsePoint/github/ad-serving-configuration/Common/web/native-dsp/adserver_log4j.xml");
    }
}
