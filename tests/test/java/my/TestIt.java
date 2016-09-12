package my;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.POJONode;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.Test;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.*;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 */
public class TestIt {
    @Test
    public void testStringUn() {
        String aaa = "";
    }

    @Test
    public void testArr() {
        Integer[] arr = new Integer[]{1,2,3};
        System.out.println(arr instanceof Object[]);
    }

    @Test
    public void testDeserializer() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        POJO1 pojo1 = objectMapper.readValue("{\"aaa\":[1,2,3,{\"a\":1, \"b\":\"bbb\"}]}", POJO1.class);
        System.out.println(pojo1.getAaa().getClass());
        System.out.println(pojo1.getAaa());
        System.out.println(((List<?>)pojo1.getAaa()).get(3).getClass());

        Object object = objectMapper.readValue("{\"aaa\":[1,2,3,{\"a\":1, \"b\":\"bbb\"}]}", Object.class);
        System.out.println(object);
        System.out.println(object.getClass());

        POJO1 pojo2 = objectMapper.treeToValue(new POJONode(object), POJO1.class);
        System.out.println(pojo2.getAaa().getClass());
        System.out.println(pojo2.getAaa());
        System.out.println(((List<?>)pojo2.getAaa()).get(3).getClass());
    }

    public static class POJO1 {
        private Object aaa;

        public Object getAaa() {
            return aaa;
        }

        public void setAaa(Object aaa) {
            this.aaa = aaa;
        }
    }


    //
    // Convert Map to POJO
    //

    public static class POJO2 {
        private String name;
        private Integer version;
        private POJOI internal;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }

        public POJOI getInternal() {
            return internal;
        }

        public void setInternal(POJOI internal) {
            this.internal = internal;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("name", name)
                    .append("version", version)
                    .append("internal", internal)
                    .toString();
        }
    }

    public static class POJOI {
        private String internal;

        public String getInternal() {
            return internal;
        }

        public void setInternal(String internal) {
            this.internal = internal;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("internal", internal)
                    .toString();
        }
    }

    @Test
    public void testConvert() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        {
            Object src = objectMapper.readValue("{\"name\": \"aaa\"}", Object.class);
            POJO2 pojo2 = objectMapper.convertValue(src, POJO2.class);
            System.out.println("pojo2:" + pojo2);
            System.out.println("pojo2:" + objectMapper.convertValue(pojo2, Object.class));
        }

        {
            Object src = objectMapper.readValue("{\"name\": \"aaa\", \"version\":\"1\", \"internal\":{}}", Object.class);
            POJO2 pojo2 = objectMapper.convertValue(src, POJO2.class);
            System.out.println("pojo2:" + pojo2);
            System.out.println("pojo2:" + objectMapper.convertValue(pojo2, Object.class));
        }

        {
            Object src = objectMapper.readValue("{\"name\": \"aaa\", \"version\":\"1\", \"internal\":{\"internal\":\"bbb\"}}", Object.class);
            POJO2 pojo2 = objectMapper.convertValue(src, POJO2.class);
            System.out.println("pojo2:" + pojo2);
            System.out.println("pojo2:" + objectMapper.convertValue(pojo2, Object.class));
        }
    }

    @Test
<<<<<<< HEAD:tests/test/java/my/TestIt.java
    public void testIpProtocolVersion() throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getLoopbackAddress();
        InetAddress localHost = InetAddress.getLocalHost();
        System.out.println(inetAddress);
        System.out.println(localHost);
    }

    @Test
    public void testExec() throws IOException {
        Process exec = new ProcessBuilder("/bin/sh", "-c", "ls /var/log")
                .directory(new File("/var/log"))
                .redirectErrorStream(true)
                .start();
        InputStream inputStream = exec.getInputStream();
        System.out.println(inputStream);

//        Executors.newWorkStealingPool()submit()
    }

    private Thread ioReadThread;

    @Test
    public void testExecutorService() throws Exception {
        Thread execThread = Thread.currentThread();

        Process exec = new ProcessBuilder("/bin/sh", "-c", "grep aaa")
                .directory(new File("/var/log"))
                .redirectErrorStream(true)
                .start();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<Object> submit = executorService.submit(() -> {
            ioReadThread = Thread.currentThread();

            InputStream in1;// = System.in;
            in1 = exec.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(in1));
            String inLine;
            try {
                while ((inLine = in.readLine()) != null) {
                    System.out.println(inLine);
                }
            } catch (Exception e) {
                System.out.println("Interrupted");
                e.printStackTrace(System.out);
            }
            System.out.println("IO Finished");
            return null;
        });

        System.out.println("Wait...");
        try {
            Object o = submit.get(1, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.out.println("Timeout");
        }

        System.out.println("Thread interrupted: " + execThread.isInterrupted());
        System.out.println("Cancel...");
        submit.cancel(true);
        System.out.println("Thread interrupted: " + execThread.isInterrupted());

        Thread.sleep(1000);
        System.out.println("Kill");
        exec.destroy();
        Thread.sleep(1000);

        System.out.println("Kill -9");
        exec.destroyForcibly();
        Thread.sleep(1000);

        System.out.println("Kill -9");
        exec.destroyForcibly();
        Thread.sleep(1000);

        System.out.println("Close input stream");
        exec.getInputStream().close();

        Thread.sleep(100000);
    }

    @Test
    public void testDoubleEquals() {
        int a = 4;
        System.out.println("a = " + a);
    }

    @Test
    public void testStandardDeviation() {
        int n = 1000000;
        double[] in = new double[n];
        double sum = 0, sum2 = 0;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < in.length; i++) {
            in[i] = random.nextGaussian();
            sum += in[i];
            sum2 += in[i] * in[i];
        }
        double std1 = sum2 / n - sum * sum / n / n;
        double avg = sum / n;
        double std2 = 0;
        for (int i = 0; i < n; i++) {
            double diff = in[i] - avg;
            std2 += diff * diff;
        }
        System.out.printf("1 = " + std1 + ", 2 = " + (std2 / n));
    }

    public void testTime() {
        long time = 1460561020160l;
/*
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("EST"));
        calendar.setTime(new Date(time));
        System.out.println("Time: " + calendar);
*/
        DateFormat dateInstance = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.FULL);
        dateInstance.setTimeZone(TimeZone.getTimeZone("EST"));
        System.out.println("Time: " + dateInstance.format(new Date(time)));
    }
}

