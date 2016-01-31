package com.contextweb.utils;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */
@Controller
public class ProxyServlet {

    private static final Logger log = Logger.getLogger(ProxyServlet.class);

/*
    @RequestMapping("/proxy")
    @ResponseBody
    public String doProxyGet(@RequestBody String body, @RequestParam String url, HttpServletResponse response) {
        log.info("Info");
        log.debug("Body: " + body);
        log.debug("URL: " + url);

//        response.setContentType("text/plain");
//        response.setCharacterEncoding("UTF-8");


        return "{\"my\": 4}";
    }
*/

    @RequestMapping(path = "/proxy", method = RequestMethod.POST)
    @ResponseBody
    public String doProxyPost(@RequestBody String body, @RequestParam String url, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return doProxy0("POST", body, url, request, response);
    }

    @RequestMapping(path = "/proxy", method = RequestMethod.GET)
    @ResponseBody
    public String doProxyGet(@RequestParam String url, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return doProxy0("GET", null, url, request, response);
    }

    private String doProxy0(String method, @RequestBody String body, @RequestParam String url, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("Info");
        log.debug("Body: " + body);
        log.debug("URL: " + url);

//        response.setContentType("text/plain");
//        response.setCharacterEncoding("UTF-8");

        log.info("    ---- In headers");
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            String value = request.getHeader(header);
            log.info("        " + header + " = " + value);
            headers.put(header, value);
        }
        log.info("    ---- Ok");


        HttpResponseData responseData = sendPost(method, url, body, headers);
        int responseCode = responseData.getResponseCode();
        log.info("    response code: " + responseCode + "/" + responseData.getResponseMessage() + "/" + responseData.getContentEncoding());
        response.setStatus(responseCode);

        log.info("    ---- Out headers");
        for (Map.Entry<String, List<String>> stringListEntry : responseData.getResponseHeaders().entrySet()) {
            String key = stringListEntry.getKey();
            List<String> value = stringListEntry.getValue();
            log.info("       " + key + " = " + value);
            String s1 = value.get(0);
            if (key != null) {
                response.addHeader(key, s1);
            }
        }
        log.info("    ---- Ok");


        return responseData.getResponseBody();
    }

    // HTTP POST request
    private HttpResponseData sendPost(String method, String url, String data, Map<String, String> headers) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        //add reuqest header
        con.setRequestMethod(method);
//        con.setRequestProperty("User-Agent", USER_AGENT);
//        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        for (Map.Entry<String, String> stringStringEntry : headers.entrySet()) {
            con.setRequestProperty(stringStringEntry.getKey(), stringStringEntry.getValue());
//            log.debug("    request field: " + stringStringEntry.getKey() + "=" + stringStringEntry.getValue());
        }

        if (data != null) {
            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(data);
            wr.flush();
            wr.close();
        }

        int responseCode = con.getResponseCode();
        log.debug("Sending 'POST' request to URL : " + url);
        log.debug("Post parameters : " + data);
        log.debug("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        HttpResponseData responseData = new HttpResponseData();
        responseData.setResponseCode(responseCode);
        responseData.setResponseBody(response.toString());
        responseData.setResponseMessage(con.getResponseMessage());

        responseData.setContentEncoding(con.getContentEncoding());
        responseData.setResponseHeaders(con.getHeaderFields());

        //print result
        log.debug(response.toString());

        return responseData;
    }

    private class HttpResponseData {

        private int responseCode;
        private String responseBody;
        private String responseMessage;
        private String contentEncoding;
        private Map<String, List<String>> responseHeaders;

        public void setResponseCode(int responseCode) {
            this.responseCode = responseCode;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public void setResponseBody(String responseBody) {
            this.responseBody = responseBody;
        }

        public String getResponseBody() {
            return responseBody;
        }


        public void setResponseMessage(String responseMessage) {
            this.responseMessage = responseMessage;
        }

        public String getResponseMessage() {
            return responseMessage;
        }

        public void setContentEncoding(String contentEncoding) {
            this.contentEncoding = contentEncoding;
        }

        public String getContentEncoding() {
            return contentEncoding;
        }

        public void setResponseHeaders(Map<String,List<String>> responseHeaders) {
            this.responseHeaders = responseHeaders;
        }

        public Map<String, List<String>> getResponseHeaders() {
            return responseHeaders;
        }
    }

    @RequestMapping(path = "/props")
    @ResponseBody
    public String getProperties(HttpServletResponse response) throws IOException {
        File userHome = new File(System.getProperty("user.home"));
        File props = new File(userHome, ".config/PulsePoint/my-web.json");
        String s = FileUtils.readFileToString(props);
        response.setContentType("application/json");
        return s;
    }

    private ConcurrentMap<String, AtomicInteger> stats = new ConcurrentHashMap<>();

    @RequestMapping(path = "/stats")
    @ResponseBody
    public String stats() {
        StringBuilder result = new StringBuilder();
        result.append("<pre>");
        for (Map.Entry<String, AtomicInteger> statsEntry : stats.entrySet()) {
            result.append(statsEntry.getKey() + "=" + statsEntry.getValue() + "\n");
        }
        result.append("</pre>");
        return result.toString();
    }

    private String addStats(String key) {
        AtomicInteger value = stats.get(key);
        if (value == null) {
            AtomicInteger newValue = new AtomicInteger();
            AtomicInteger oldValue = stats.putIfAbsent(key, newValue);
            value = oldValue != null ? oldValue : newValue;
        }
        value.incrementAndGet();
        return key;
    }

    @RequestMapping(path = {"/oldApp/**", "/oldApp"}, method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public String oldAppGet(HttpServletRequest request) {
        return addStats("old-" + request.getMethod() + "[" + request.getRequestURI() + "]");
    }

    @RequestMapping(path = {"/newApp/**", "/newApp"}, method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public String newAppGet(HttpServletRequest request) {
        return addStats("new-" + request.getMethod() + "[" + request.getRequestURI() + "]");
    }

}
