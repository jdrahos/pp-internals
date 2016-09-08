package org.mpn;

import fiftyone.mobile.detection.Match;
import fiftyone.mobile.detection.Provider;
import fiftyone.mobile.detection.TrieProvider;
import fiftyone.mobile.detection.entities.Property;
import fiftyone.mobile.detection.entities.Values;
import fiftyone.mobile.detection.factories.StreamFactory;
import fiftyone.mobile.detection.factories.TrieFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Arrays;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App {

    private static final String DATA_DIR = "/opt/projects/pulsepoint/ad-serving-and-commons/ad-serving/ad-serving-commons/src/test/resources/com/contextweb/adserving/commons/useragent/regression";
    private static final String UA_FILE = "/opt/projects/pulsepoint/ad-serving-and-commons/ad-serving/ad-serving-commons/src/test/resources/com/contextweb/adserving/commons/useragent/regression/ua-strings4_uniq.txt";
    private static final int mb = 1024*1024;

    int warmupCycles = 102;

    // Device detection provider which takes User-Agents and returns matches.
    protected final Provider provider;

    // User-Agent string of a iPhone mobile device.
    protected final String mobileUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 7_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D167 Safari/9537.53";


    private int a = 0;
    private int mobileErrors = 0;
    private Property browserName;
    private Property browserVersion;
    private Property platformName;
    private Property platformVersion;
    private Property isMobile;
    // Not accessible in lite
    private Property hardwareFamily;
    private Property hardwareModel;
    private Property hardwareName;
    private Property hardwareVendor;
    private Property deviceType;


    public App() throws Exception {
        printUsedMemory("Initial");

        this.provider = new Provider(StreamFactory.create(DATA_DIR + "/51Degrees-PremiumV3_2.dat", false), 20020);
        browserName = provider.dataSet.get("BrowserName");
        browserVersion = provider.dataSet.get("BrowserVersion");
        platformName = provider.dataSet.get("PlatformName");
        platformVersion = provider.dataSet.get("PlatformVersion");
        isMobile = provider.dataSet.get("IsMobile");
        // Not accessible in lite
        hardwareFamily = provider.dataSet.get("HardwareFamily");
        hardwareModel = provider.dataSet.get("HardwareModel");
        hardwareName = provider.dataSet.get("HardwareName");
        hardwareVendor = provider.dataSet.get("HardwareVendor");
        deviceType = provider.dataSet.get("DeviceType");

        detect(mobileUserAgent);
        testPerformance();
        printUsedMemory("After test regexp");
        provider.dataSet.close();
        cleanupMemory();


        testTriePerformance();

        testSame();

        printUsedMemory("Finally");
    }

    private void cleanupMemory() throws InterruptedException {
        System.gc();
        Thread.sleep(1000);
        System.gc();
        Thread.sleep(1000);
        printUsedMemory("After memory cleanup");
    }

    private void printUsedMemory(String action) {
        Runtime runtime = Runtime.getRuntime();
        System.out.println(action + ":" + runtime.totalMemory()/mb + " Free: " + runtime.freeMemory() / mb);
    }

    private void testTriePerformance() throws Exception {
        System.out.println("-------------------------------------------------- Test trie");
        mobileErrors = 0;
        a = 0;
        TrieProvider trieProvider = TrieFactory.create(DATA_DIR + "/51Degrees-PremiumV3_2.trie");
        List<String> propertyNames = trieProvider.propertyNames();
        int browserName = propertyNames.indexOf("BrowserName");
        int browserVersion = propertyNames.indexOf("BrowserVersion");
        int platformName = propertyNames.indexOf("PlatformName");
        int platformVersion = propertyNames.indexOf("PlatformVersion");
        int isMobile = propertyNames.indexOf("IsMobile");
        // Not accessible in lite
        int hardwareFamily = propertyNames.indexOf("HardwareFamily");
        int hardwareModel = propertyNames.indexOf("HardwareModel");
        int hardwareName = propertyNames.indexOf("HardwareName");
        int hardwareVendor = propertyNames.indexOf("HardwareVendor");
        int deviceType = propertyNames.indexOf("DeviceType");
        int[] arr = new int[]{browserName, browserVersion, platformName, platformVersion, isMobile, hardwareFamily, hardwareModel, hardwareName, hardwareVendor, deviceType};

        {
            int deviceIndex = trieProvider.getDeviceIndex(mobileUserAgent);
            System.out.println("BrowserName = " + trieProvider.getPropertyValue(deviceIndex, browserName));
            System.out.println("BrowserVersion = " + trieProvider.getPropertyValue(deviceIndex, browserVersion));
            System.out.println("PlatformName = " + trieProvider.getPropertyValue(deviceIndex, platformName));
            System.out.println("PlatformVersion = " + trieProvider.getPropertyValue(deviceIndex, platformVersion));
            System.out.println("IsMobile = " + trieProvider.getPropertyValue(deviceIndex, isMobile));
            System.out.println("HardwareFamily = " + trieProvider.getPropertyValue(deviceIndex, hardwareFamily));
            System.out.println("HardwareModel = " + trieProvider.getPropertyValue(deviceIndex, hardwareModel));
            System.out.println("HardwareName = " + trieProvider.getPropertyValue(deviceIndex, hardwareName));
            System.out.println("HardwareVendor = " + trieProvider.getPropertyValue(deviceIndex, hardwareVendor));
            System.out.println("DeviceType = " + trieProvider.getPropertyValue(deviceIndex, deviceType));
        }


        System.out.println("-------------------------------------------------- Test trie performance");

        LineNumberReader in  = new LineNumberReader(new FileReader(UA_FILE));
        long now = System.currentTimeMillis();
        for (int i = 0; i < warmupCycles; i++) {
            processTrieUa(trieProvider, in.readLine(),isMobile, arr);
        }
        long total = System.currentTimeMillis() - now;
        System.out.println("Trie Warmup Microseconds: "  + total * 1000 / warmupCycles);


        now = System.currentTimeMillis();
        int count = 0;
        String inLine;
        while ((inLine = in.readLine()) != null) {
            count++;
            processTrieUa(trieProvider, inLine, isMobile, arr);
        }
        total = System.currentTimeMillis() - now;
        System.out.println("trie regexp Microseconds: "  + total * 1000 / count);
        System.out.println("mobileErrors=" + mobileErrors);
        System.out.println("a=" + a);
        in.close();

        printUsedMemory("After trie performance");

        trieProvider.close();
        cleanupMemory();
    }

    private void processTrieUa(TrieProvider trieProvider, String inLine, int isMobile, int[] arr) throws Exception {
        char inventoryType = inLine.charAt(0);
        String userAgent = inLine.substring(2);

        int deviceIndex = trieProvider.getDeviceIndex(userAgent);
        try {
            for (int i : arr) {
                if (i >= 0 || deviceIndex > 0) {
                    String value = trieProvider.getPropertyValue(deviceIndex, i);
                    if (value != null) {
                        a += value.hashCode();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Err: " + deviceIndex);
        }
        String value = trieProvider.getPropertyValue(deviceIndex, isMobile);
        boolean mob = Boolean.TRUE.equals(Boolean.valueOf(value));
        if (inventoryType == '2' && !mob) {
            mobileErrors++;
        }

    }

    private void testSame() throws IOException {
        System.out.println("-------------------------------------------------- Check dataset");
        LineNumberReader in  = new LineNumberReader(new FileReader(UA_FILE));
        String inLine;
        int[] uaHashes = new int[20005];
        int[] hashes = new int[20005];
        long now = System.currentTimeMillis();
        while ((inLine = in.readLine()) != null) {
            String userAgent = inLine.substring(2);
            uaHashes[in.getLineNumber()] = userAgent.hashCode();
            hashes[in.getLineNumber()] = inLine.hashCode();
        }
        long total = System.currentTimeMillis() - now;
        System.out.println("Read Microseconds: "  + total * 1000 / in.getLineNumber());
        in.close();

        Arrays.sort(hashes);
        Arrays.sort(uaHashes);
        int uaSames = 0;
        int sames = 0;
        for (int i = 0; i < hashes.length - 1; i++) {
            if (hashes[i] == hashes[i+1]) sames++;
            if (uaHashes[i] == uaHashes[i+1]) uaSames++;
        }
        System.out.println("sames = " + sames + ", uasames = " + uaSames);
    }

    private void testPerformance() throws IOException {
        System.out.println("-------------------------------------------------- Warmup regex");
        LineNumberReader in  = new LineNumberReader(new FileReader(UA_FILE));

        long now = System.currentTimeMillis();
        for (int i = 0; i < warmupCycles; i++) {
            processUa(in.readLine());
        }
        long total = System.currentTimeMillis() - now;
        System.out.println("Warmup Microseconds: "  + total * 1000 / warmupCycles);


        System.out.println("-------------------------------------------------- Test regex");
        now = System.currentTimeMillis();
        int count = 0;
        String inLine;
        while ((inLine = in.readLine()) != null) {
            processUa(inLine);
            count++;
        }
        total = System.currentTimeMillis() - now;
        System.out.println("regexp Microseconds: "  + total * 1000 / count);
        System.out.println("mobileErrors=" + mobileErrors);
        System.out.println("a=" + a);
        in.close();

        System.out.println("-------------------------------------------------- Test cache");
        in  = new LineNumberReader(new FileReader(UA_FILE));
        count = 0;
        a = 0;
        now = System.currentTimeMillis();
        while ((inLine = in.readLine()) != null) {
            processUa(inLine);
            count++;
        }
        total = System.currentTimeMillis() - now;
        System.out.println("cache regexp Microseconds: "  + total * 1000 / count);
        System.out.println("mobileErrors=" + mobileErrors);
        System.out.println("a=" + a);
        in.close();

    }

    public void processUa(String inLine) throws IOException {
        char inventoryType = inLine.charAt(0);
        String userAgent = inLine.substring(2);
        Match match = provider.match(userAgent);
        int b1 = getValue(browserName, match);
        int b2 = getValue(browserVersion, match);
        int b3 = getValue(platformName, match);
        int b4 = getValue(platformVersion, match);
        int b10 = getValue(isMobile, match);
        int b5 = getValue(hardwareFamily, match);
        int b6 = getValue(hardwareModel, match);
        int b7 = getValue(hardwareName, match);
        int b8 = getValue(hardwareVendor, match);
        int b9 = getValue(deviceType, match);
        a += b1 + b2 + b3 + b4 + b5 + b6 + b7 + b8 + b9 + b10;
        boolean isMbile = match.getValues(isMobile).get(0).toBool();
        if (inventoryType == '2' && !isMbile) {
            mobileErrors++;
//            System.out.println("Mobile error: " + userAgent);
        }
    }
    private int getValue(Property browserName, Match match) throws IOException {
        Values values = match.getValues(browserName);
        return values == null ? 0 : values.get(0).toString().hashCode();
    }

    public void detect(String userAgent) throws IOException {
        Match match = provider.match(userAgent);
        System.out.println("UA:" + userAgent + ", result:" + match.getResults());
        printProperty(match, "BrowserName");
        printProperty(match, "BrowserVersion");
        printProperty(match, "PlatformName");
        printProperty(match, "PlatformVersion");
        printProperty(match, "IsMobile");
        // Not accessible in lite
        printProperty(match, "HardwareFamily");
        printProperty(match, "HardwareModel");
        printProperty(match, "HardwareName");
        printProperty(match, "HardwareVendor");
        printProperty(match, "DeviceType");
        System.out.println("--------------------------------------------------");
    }

    private void printProperty(Match match, String property) throws IOException {
        System.out.println(property + ": " + match.getValues(property));
    }

    public static void main(String[] args) throws Exception {
        new App();

    }
}
