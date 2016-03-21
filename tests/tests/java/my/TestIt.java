package my;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.POJONode;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 */
public class TestIt {
    @Test
    public void testStringUn() {
        String aaa = "{\"assets\":[{\"id\":1,\"title\":{\"len\":70},\"required\":1},{\"id\":2,\"data\":{\"len\":300,\"type\":2},\"required\":1},{\"id\":3,\"img\":{\"w\":300,\"mimes\":[\"image/png\"],\"type\":3,\"h\":250},\"required\":1},{\"id\":4,\"data\":{\"len\":30,\"type\":1},\"required\":0},{\"id\":5,\"data\":{\"len\":300,\"type\":11},\"required\":1}],\"layout\":6,\"adunit\":2}";
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
}

