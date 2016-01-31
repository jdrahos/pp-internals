package r;

/**
 * RemoveThisDebugCode
 *
 * @author Pavel Moukhataev
 */
public class RemoveThisDebugCode {

    public static void run(RunnableE code) {
        try {
            code.run();
        } catch (Exception e) {
            rethrow0(e);
        }
    }

    private static <T extends Throwable> void rethrow0(Throwable t) throws T {
        throw (T) t;
    }
}
