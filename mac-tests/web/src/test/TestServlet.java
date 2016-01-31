package test;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

/**
 * TestServlet
 *
 * @author pavelmukhataev
 */
@WebServlet(name = "test", urlPatterns = {"/test1"})
public class TestServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String aaa = req.getParameter("aaa");
        System.out.println(aaa + " / " + req.getParameterMap());
        System.out.println(Arrays.toString(req.getParameterValues("aaa")));

    }
}
