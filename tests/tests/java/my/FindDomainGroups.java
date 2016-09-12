package my;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * FindDomainGroups
 *
 * @author Pavel Moukhataev
 */
public class FindDomainGroups {

    private Set<String> domainGroups = new HashSet<>();

    private void run() {
        File dir = new File("/Public/Projects/PulsePoint/github/pp-internals/test-data/src/misc/domainGroups_datasets/domainGroups");
    }

    public static void main(String[] args) {
        new FindDomainGroups().run();
    }
}
