import org.easymock.EasyMock;
import org.junit.Test;

/**
 * TestMocks
 *
 * @author pavelmukhataev
 */
public class TestMocks {

    @Test
    public void testMocks() {
        MyInterface mock = EasyMock.createMock(MyInterface.class);
        mock.voidMethod();
//        EasyMock.expectLastCall();

        EasyMock.replay(mock);

        mock.voidMethod();
        EasyMock.verify(mock);
    }

}

