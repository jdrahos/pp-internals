package example;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * StrTest
 *
 * @author Pavel Moukhataev
 */
public class StrTest {

    @Test
    public void testSplit() {
        String keyStr = "or.apache.http.1__1";
        int dotIndex;
        if ((dotIndex = keyStr.lastIndexOf('.')) != -1) {
            keyStr = keyStr.substring(dotIndex+1);
        }

        System.out.println(keyStr);
    }


    @Test
    public void testEmptyRegexGroup() {
        String str1 = "aaa bbb ccc";
        String str2 = "aaa ccc";

        Pattern pattern = Pattern.compile("aaa\\s+(bbb)?\\s+ccc");
        Matcher matcher = pattern.matcher(str1);
        System.out.println("mtchs: " + matcher.matches());
        System.out.println("cnt: " + matcher.groupCount());
        System.out.println("    " + matcher.group(1));

        matcher = pattern.matcher(str2);
        System.out.println("cnt: " + matcher.groupCount());
        System.out.println("    " + matcher.group(1));
    }
}
