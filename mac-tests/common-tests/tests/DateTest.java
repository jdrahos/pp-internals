import org.junit.Test;

import java.text.DateFormatSymbols;
import java.util.Locale;
import java.util.TimeZone;

/**
 */
public class DateTest {
    public static void main(String[] args) {
        System.out.println(System.getProperty("java.version"));
        printZonez(Locale.ENGLISH);
        printZonez(Locale.US);
    }

    private static void printZonez(Locale locale) {
        System.out.print("Locale: " + locale + " --> ");
        String[][] zoneStrings = DateFormatSymbols.getInstance(locale).getZoneStrings();
        for (int i = 0; i < zoneStrings.length; i++) {
            String[] zoneString = zoneStrings[i];
            if (zoneString[0].equals("UTC")) {
                System.out.println("i=" + i);
            }
        }
        System.out.println("n=" + zoneStrings[0].length);

/*
        for (String[] zoneString : zoneStrings) {
            System.out.print(zoneString[0] + ",");
        }
*/
        System.out.println();

    }

    @Test
    public void getDefaultTZ() {
        System.out.println("TZ = " + TimeZone.getDefault());
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
