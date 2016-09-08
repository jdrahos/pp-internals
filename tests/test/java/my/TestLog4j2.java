package my;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * TestLog4j2
 *
 * @author Pavel Moukhataev
 */
public class TestLog4j2 {



    public static void main(String[] args) {
        Logger logBlame1 = LogManager.getLogger("com.contextweb.adserving.commons.service.CachingService.aaa");
        Logger logPooling1 = LogManager.getLogger("com.contextweb.commons.client.http.batch.httpcorenio2.pooling.bbb");
        Logger log = LogManager.getLogger("com.contextweb.ccc");

        log.info("info");
        logBlame1.info("info");
        logPooling1.info("info");

        log.fatal("fatal");
        logBlame1.fatal("fatal");
        logPooling1.fatal("fatal");

        log.debug("debug");
        logBlame1.debug("debug");
        logPooling1.debug("debug");
    }
}
