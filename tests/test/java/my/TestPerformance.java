package my;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Pavel Moukhataev
 */
public class TestPerformance {

    private static final int iterNumber = 2;
    private static final int poolSize = 20;
    private static final int threadsCount = 3;
    private static final int warmUpIterations = 10000;
    private static final int runIterations = 1000000;


    public void test(PoolMeasure poolMeasure) throws InterruptedException {
        for (int i = 0; i < poolSize; i++) {
            poolMeasure.release(new NIOPoolEntry(), true);
        }

        measurePerformance(poolMeasure, warmUpIterations);
        long l = measurePerformance(poolMeasure, runIterations);
        System.out.println(poolMeasure.getClass().getSimpleName() + " -> " + l);
    }

    private long measurePerformance(PoolMeasure poolMeasure, int iterationCount) throws InterruptedException {
        AtomicLong time = new AtomicLong();
        Thread[] threads = new Thread[threadsCount];
        for (int i = 0; i < threadsCount; i++) {
            threads[i] = new Thread(() -> {
                long start = System.currentTimeMillis();

                for (int j = 0; j < iterationCount; j++) {
                    NIOPoolEntry lease = poolMeasure.lease();
                    poolMeasure.release(lease, true);
                }

                time.addAndGet(System.currentTimeMillis() - start);
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
        return time.get();
    }

    public static void main(String[] args) throws Exception {
        TestPerformance testPerformance = new TestPerformance();
        testPerformance.test(new PoolMeasureNew());
        testPerformance.test(new PoolMeasureOld());
    }


    public interface PoolMeasure {
        NIOPoolEntry lease();
        boolean release(final NIOPoolEntry leasedEntry, final boolean reusable);
    }

    public static class PoolMeasureNew implements PoolMeasure {
        private final Object poolMonitor = new Object();
        private final Set<NIOPoolEntry> leased = new HashSet<>();
        private final Queue<NIOPoolEntry> available = new LinkedList<>();
        private final AtomicInteger totalLeased = new AtomicInteger();
        private final AtomicInteger totalAvailable = new AtomicInteger();


        public NIOPoolEntry lease() {
            synchronized (poolMonitor) {
                if (this.available.isEmpty()) {
                    return null;
                }
                final Iterator<NIOPoolEntry> it = this.available.iterator();
                int count = 0;
                while (it.hasNext()) {
                    final NIOPoolEntry entry = it.next();
                    if (++count >= iterNumber) {
                        this.leased.add(entry);
                        it.remove();
                        this.totalAvailable.decrementAndGet();
                        this.totalLeased.incrementAndGet();
                        return entry;
                    }
                }
            }
            System.out.println("Didn't find new entry");
            return null;
        }

        public boolean release(final NIOPoolEntry leasedEntry, final boolean reusable) {
            boolean found;
            synchronized (poolMonitor) {
                found = this.leased.remove(leasedEntry);

                if (reusable) {
                    this.available.offer(leasedEntry);
                    this.totalAvailable.incrementAndGet();
                }
            }

            if (found){
                this.totalLeased.decrementAndGet();
            }

            return found;
        }
    }


    public static class PoolMeasureOld implements PoolMeasure {
        private final Set<NIOPoolEntry> leased = Collections.synchronizedSet(new HashSet<>());
        private final Queue<NIOPoolEntry> available = new ConcurrentLinkedQueue<>();
        private final AtomicInteger totalLeased = new AtomicInteger();
        private final AtomicInteger totalAvailable = new AtomicInteger();


        public NIOPoolEntry lease() {
            if (this.available.isEmpty()) {
                return null;
            }

            final Iterator<NIOPoolEntry> it = this.available.iterator();

            int count = 0;
            while (it.hasNext()) {
                final NIOPoolEntry entry = it.next();
                if (++count >= iterNumber) {
                    synchronized (this.leased){
                        if (this.leased.contains(entry)) {
                            continue;
                        }
                        this.leased.add(entry);
                    }

                    available.remove(entry);
                    this.totalAvailable.decrementAndGet();
                    this.totalLeased.incrementAndGet();
                    return entry;
                }
            }
            System.out.println("Didn't find old entry : " + available + ", leased: " + leased);
            return null;
        }

        /**
         * call to release connection pool entry back to the route specific pool
         * @param leasedEntry leased connection pool entry to be returned to the pool
         * @param reusable indicator if the returned entry can be reused
         * @return true if the returned entry was leased from this pool
         */
        public boolean release(final NIOPoolEntry leasedEntry, final boolean reusable) {
            final boolean found = this.leased.remove(leasedEntry);

            if (found){
                this.totalLeased.decrementAndGet();
            }

            if (reusable) {
                this.available.offer(leasedEntry);
                this.totalAvailable.incrementAndGet();
            }

            return found;
        }
    }


    private static final class NIOPoolEntry {
        private static int globalNum;
        private int num = globalNum++;

        @Override
        public boolean equals(Object o) {
            return o != null && num == ((NIOPoolEntry) o).num;

        }

        @Override
        public int hashCode() {
            return num;
        }

        @Override
        public String toString() {
            return String.valueOf(num);
        }
    }

}
