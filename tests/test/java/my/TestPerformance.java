package my;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

/**
 * @author Pavel Moukhataev
 */
public class TestPerformance {
    private static final int warmUpIterations = 10*1000;
    private static final int runIterations = 1*1000*1000;

    private final int parallelism;
    private final int poolSize;

    public TestPerformance(int parallelism) {
        this.parallelism = parallelism;
        this.poolSize = this.parallelism*2;
    }

    public static void main(String[] args) throws Exception {
        IntStream.rangeClosed(2, Runtime.getRuntime().availableProcessors())
                .forEach(parallelism -> {
                    System.out.println("Running with parallelism: "+ parallelism);
                    TestPerformance testPerformance = new TestPerformance(parallelism);

                    testPerformance.test(new SynchronizeMethod());
                    testPerformance.test(new SynchronizeRoot());
                    testPerformance.test(new SynchronizeIndividual());
                    testPerformance.test(new LockRoot());
                    testPerformance.test(new LockIndividual());
                    testPerformance.test(new Concurrent());
                    System.out.println("");
                });
    }

    public void test(Strategy strategy) {
        for (int i = 0; i < poolSize; i++) {
            strategy.release(new NIOPoolEntry(), true);
        }

        measurePerformance(strategy, warmUpIterations);
        RunResult runResult = measurePerformance(strategy, runIterations);
        System.out.println(strategy.getClass().getSimpleName() + "; duration: " + runResult.duration +"ms; exhaustion: "+ runResult.exhaustion);
    }

    private RunResult measurePerformance(Strategy strategy, int runIterations) {
        AtomicLong time = new AtomicLong();
        AtomicInteger exhaustion = new AtomicInteger();
        Thread[] threads = new Thread[parallelism];
        CyclicBarrier barrier = new CyclicBarrier(parallelism);
        int iterationCount = runIterations / parallelism;

        for (int i = 0; i < parallelism; i++) {
            threads[i] = new Thread(() -> {
                try {
                    barrier.await();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                long start = System.currentTimeMillis();

                for (int j = 0; j < iterationCount; j++) {
                    NIOPoolEntry lease = strategy.lease();
                    if (lease != null){
                        strategy.release(lease, true);
                    } else {
                        exhaustion.incrementAndGet();
                    }
                }

                time.addAndGet(System.currentTimeMillis() - start);
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ignore) {}
        }
        return new RunResult(time.get(), exhaustion.get());
    }

    private class RunResult {
        private final long duration;
        private final int exhaustion;

        private RunResult(long duration, int exhaustion) {
            this.duration = duration;
            this.exhaustion = exhaustion;
        }
    }

    public interface Strategy {
        NIOPoolEntry lease();
        boolean release(final NIOPoolEntry leasedEntry, final boolean reusable);
    }

    public static class SynchronizeMethod implements Strategy {
        private final Set<NIOPoolEntry> leased = new HashSet<>();
        private final Queue<NIOPoolEntry> available = new LinkedList<>();
        private final AtomicInteger totalLeased = new AtomicInteger();
        private final AtomicInteger totalAvailable = new AtomicInteger();


        public NIOPoolEntry lease() {
            if (this.available.isEmpty()) {
                return null;
            }

            return getNioPoolEntry();
        }

        private synchronized NIOPoolEntry getNioPoolEntry() {
            NIOPoolEntry poolEntry = this.available.poll();

            if (poolEntry != null){
                this.leased.add(poolEntry);
                this.totalAvailable.decrementAndGet();
                this.totalLeased.incrementAndGet();
            }

            return poolEntry;
        }

        public boolean release(final NIOPoolEntry leasedEntry, final boolean reusable) {
            boolean found;
            found = removeLeased(leasedEntry, reusable);

            if (found){
                this.totalLeased.decrementAndGet();
            }

            return found;
        }

        private synchronized boolean removeLeased(NIOPoolEntry leasedEntry, boolean reusable) {
            boolean found;
            found = this.leased.remove(leasedEntry);

            if (reusable) {
                this.available.offer(leasedEntry);
                this.totalAvailable.incrementAndGet();
            }
            return found;
        }
    }


    public static class SynchronizeRoot implements Strategy {
        private final Object syncRoot = new Object();
        private final Set<NIOPoolEntry> leased = new HashSet<>();
        private final Queue<NIOPoolEntry> available = new LinkedList<>();
        private final AtomicInteger totalLeased = new AtomicInteger();
        private final AtomicInteger totalAvailable = new AtomicInteger();


        public NIOPoolEntry lease() {
            if (this.available.isEmpty()) {
                return null;
            }

            synchronized (syncRoot) {
                NIOPoolEntry poolEntry = this.available.poll();

                if (poolEntry != null){
                    this.leased.add(poolEntry);
                    this.totalAvailable.decrementAndGet();
                    this.totalLeased.incrementAndGet();
                }

                return poolEntry;
            }
        }

        public boolean release(final NIOPoolEntry leasedEntry, final boolean reusable) {
            boolean found;
            synchronized (syncRoot) {
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

    public static class SynchronizeIndividual implements Strategy {
        private final Set<NIOPoolEntry> leased = new HashSet<>();
        private final Queue<NIOPoolEntry> available = new LinkedList<>();
        private final AtomicInteger totalLeased = new AtomicInteger();
        private final AtomicInteger totalAvailable = new AtomicInteger();


        public NIOPoolEntry lease() {
            if (this.available.isEmpty()) {
                return null;
            }

            NIOPoolEntry entry;

            synchronized (available){
                entry = available.poll();
            }

            if (entry != null){
                synchronized (leased){
                    this.leased.add(entry);
                }
                this.totalAvailable.decrementAndGet();
                this.totalLeased.incrementAndGet();
            }

            return entry;
        }

        /**
         * call to release connection pool entry back to the route specific pool
         * @param leasedEntry leased connection pool entry to be returned to the pool
         * @param reusable indicator if the returned entry can be reused
         * @return true if the returned entry was leased from this pool
         */
        public boolean release(final NIOPoolEntry leasedEntry, final boolean reusable) {
            boolean found;
            synchronized (leased){
                found = this.leased.remove(leasedEntry);
            }

            if (found){
                this.totalLeased.decrementAndGet();
            }

            if (reusable) {
                synchronized (available){
                    this.available.offer(leasedEntry);
                }
                this.totalAvailable.incrementAndGet();
            }

            return found;
        }
    }

    public static class LockRoot implements Strategy {
        private final ReentrantLock lock = new ReentrantLock();
        private final Set<NIOPoolEntry> leased = new HashSet<>();
        private final Queue<NIOPoolEntry> available = new LinkedList<>();
        private final AtomicInteger totalLeased = new AtomicInteger();
        private final AtomicInteger totalAvailable = new AtomicInteger();


        public NIOPoolEntry lease() {
            if (this.available.isEmpty()) {
                return null;
            }

            lock.lock();
            try{
                NIOPoolEntry poolEntry = this.available.poll();

                if (poolEntry != null){
                    this.leased.add(poolEntry);
                    this.totalAvailable.decrementAndGet();
                    this.totalLeased.incrementAndGet();
                }

                return poolEntry;
            } finally {
                lock.unlock();
            }
        }

        public boolean release(final NIOPoolEntry leasedEntry, final boolean reusable) {
            boolean found;

            lock.lock();
            try {
                found = this.leased.remove(leasedEntry);

                if (reusable) {
                    this.available.offer(leasedEntry);
                    this.totalAvailable.incrementAndGet();
                }
            } finally {
                lock.unlock();
            }

            if (found){
                this.totalLeased.decrementAndGet();
            }

            return found;
        }
    }

    public static class LockIndividual implements Strategy {
        private final ReentrantLock availableLock = new ReentrantLock();
        private final ReentrantLock leasedLock = new ReentrantLock();
        private final Set<NIOPoolEntry> leased = new HashSet<>();
        private final Queue<NIOPoolEntry> available = new LinkedList<>();
        private final AtomicInteger totalLeased = new AtomicInteger();
        private final AtomicInteger totalAvailable = new AtomicInteger();


        public NIOPoolEntry lease() {
            if (this.available.isEmpty()) {
                return null;
            }

            NIOPoolEntry entry;


            availableLock.lock();
            try {
                entry = available.poll();
            } finally {
                availableLock.unlock();
            }

            if (entry != null){
                leasedLock.lock();
                try{
                    this.leased.add(entry);
                } finally {
                    leasedLock.unlock();
                }
                this.totalAvailable.decrementAndGet();
                this.totalLeased.incrementAndGet();
            }

            return entry;
        }

        /**
         * call to release connection pool entry back to the route specific pool
         * @param leasedEntry leased connection pool entry to be returned to the pool
         * @param reusable indicator if the returned entry can be reused
         * @return true if the returned entry was leased from this pool
         */
        public boolean release(final NIOPoolEntry leasedEntry, final boolean reusable) {
            boolean found;
            leasedLock.lock();
            try{
                found = this.leased.remove(leasedEntry);
            } finally {
                leasedLock.unlock();
            }

            if (found){
                this.totalLeased.decrementAndGet();
            }

            if (reusable) {
                availableLock.lock();
                try{
                    this.available.offer(leasedEntry);
                } finally {
                    availableLock.unlock();
                }
                this.totalAvailable.incrementAndGet();
            }

            return found;
        }
    }

    public static class Concurrent implements Strategy {
        private final Set<NIOPoolEntry> leased = ConcurrentHashMap.newKeySet();
        private final Queue<NIOPoolEntry> available = new ConcurrentLinkedQueue<>();
        private final AtomicInteger totalLeased = new AtomicInteger();
        private final AtomicInteger totalAvailable = new AtomicInteger();

        public NIOPoolEntry lease() {
            if (this.available.isEmpty()) {
                return null;
            }

            NIOPoolEntry entry = this.available.poll();

            if (entry != null){
                this.leased.add(entry);
                this.totalAvailable.decrementAndGet();
                this.totalLeased.incrementAndGet();
            }

            return entry;
        }

        /**
         * call to release connection pool entry back to the route specific pool
         * @param leasedEntry leased connection pool entry to be returned to the pool
         * @param reusable indicator if the returned entry can be reused
         * @return true if the returned entry was leased from this pool
         */
        public boolean release(final NIOPoolEntry leasedEntry, final boolean reusable) {
            boolean found;
            found = this.leased.remove(leasedEntry);

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
