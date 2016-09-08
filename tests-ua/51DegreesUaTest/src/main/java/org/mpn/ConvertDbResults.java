package org.mpn;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.LineNumberReader;
import java.io.PrintWriter;

/**
 * Convert dbVisualiser CLI output to CSV
 *
 * @author Pavel Moukhataev
 */
public class ConvertDbResults {

/*
    public static void main(String[] args) throws Exception {
        LineNumberReader in  = new LineNumberReader(new FileReader("/opt/projects/pulsepoint/ad-serving-and-commons/ad-serving/ad-serving-commons/src/test/resources/com/contextweb/adserving/commons/useragent/regression/ua-strings-app-raw.txt"));
        PrintWriter out  = new PrintWriter(new FileWriter("/opt/projects/pulsepoint/ad-serving-and-commons/ad-serving/ad-serving-commons/src/test/resources/com/contextweb/adserving/commons/useragent/regression/ua-strings-app.txt"));
        for (int i = 0; i < 12; i++) {
            in.readLine();
        }
//        out.println("inventoryType\tuserAgentString");

        String inLine;
        while ((inLine = in.readLine()) != null) {
            String inventoryType = inLine.substring(0, 2);
            String userAgent = inLine.substring(14).trim();
            out.println(inventoryType+userAgent);
        }
        in.close();
        out.close();
    }
*/

    public static void main(String[] args) throws Exception {
        LineNumberReader in  = new LineNumberReader(new FileReader("/opt/projects/pulsepoint/ad-serving-and-commons/ad-serving/ad-serving-commons/src/test/resources/com/contextweb/adserving/commons/useragent/regression/ua-strings-app-nonUniq-raw.txt"));
        PrintWriter out  = new PrintWriter(new FileWriter("/opt/projects/pulsepoint/ad-serving-and-commons/ad-serving/ad-serving-commons/src/test/resources/com/contextweb/adserving/commons/useragent/regression/ua-strings-app-nonUniq.txt"));

        String inLine;
        while ((inLine = in.readLine()) != null) {
            inLine = inLine.trim();
            if (!inLine.isEmpty()) {
                out.println(inLine);
            }
        }
        in.close();
        out.close();
    }
}
