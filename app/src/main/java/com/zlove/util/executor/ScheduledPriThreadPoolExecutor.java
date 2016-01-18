package com.zlove.util.executor;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by ZLOVE on 2015/3/1.
 */
public class ScheduledPriThreadPoolExecutor extends ThreadPoolExecutor implements ScheduledExecutorService {


    public ScheduledPriThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize, Integer.MAX_VALUE, 0, TimeUnit.NANOSECONDS, new DelayedWorkQueue());
    }

    public ScheduledPriThreadPoolExecutor(int corePoolSize, ThreadFactory t) {
        super(corePoolSize, Integer.MAX_VALUE, 0, TimeUnit.NANOSECONDS, new DelayedWorkQueue(), t);
    }

    static final long initialNanoTime = System.nanoTime();

    static long now() {
        return System.nanoTime() - initialNanoTime;
    }

    private static final AtomicLong sequencer = new AtomicLong(0);

    /**
     * True if ScheduledFutureTask.cancel should remove from queue
     */
    private volatile boolean removeOnCancel = false;

    /**
     * False if should cancel/suppress periodic tasks on shutdown.
     */
    private volatile boolean continueExistingPeriodicTasksAfterShutdown;

    /**
     * False if should cancel non-periodic tasks on shutdown.
     */
    private volatile boolean executeExistingDelayedTasksAfterShutdown = true;

    static class DelayedWorkQueue // extends AbstractQueue<Runnable>
            implements BlockingQueue<Runnable> {

        private PriorityBlockingQueue<Runnable> delayQueue = new PriorityBlockingQueue<Runnable>();

        private PriorityBlockingQueue<Runnable> readyQueue = new PriorityBlockingQueue<Runnable>();

        private final ReentrantLock lock = new ReentrantLock();

        private final Condition available = lock.newCondition();

        @Override
        public boolean add(Runnable o) {
            try {
                return offer(o, 0, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        public boolean addAll(Collection<? extends Runnable> c) {
            lock.lock();
            try {
                boolean ret = false;
                if (c != null) {
                    for (Runnable r : c) {
                        ret = ret || add(r);
                    }
                }
                return ret;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public Runnable remove() {
            lock.lock();
            try {
                checkedDelayedAreReady();
                return readyQueue.remove();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public Runnable element() {
            lock.lock();
            try {
                checkedDelayedAreReady();
                return readyQueue.element();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void clear() {
            lock.lock();
            try {
                readyQueue.clear();
                delayQueue.clear();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean contains(Object object) {
            lock.lock();
            try {
                if (readyQueue.contains(object)) {
                    return true;
                } else {
                    return delayQueue.contains(object);
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            lock.lock();
            try {
                for (Object o : collection) {
                    if (!contains(o)) {
                        return false;
                    }
                }
                return true;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean isEmpty() {
            checkedDelayedAreReady();
            return readyQueue.isEmpty();
        }

        @Override
        public boolean remove(Object object) {
            lock.lock();
            try {
                if (readyQueue.remove(object)) {
                    return true;
                } else {
                    return delayQueue.remove(object);
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            lock.lock();
            try {
                boolean ret = false;
                for (Object o : collection) {
                    ret = ret || remove(o);
                }
                return ret;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            lock.lock();
            try {
                boolean ret;
                ret = delayQueue.retainAll(collection);
                ret = ret || readyQueue.retainAll(collection);
                return ret;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public Object[] toArray() {
            lock.lock();
            try {
                checkedDelayedAreReady();
                return readyQueue.toArray();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public <T> T[] toArray(T[] contents) {
            lock.lock();
            try {
                checkedDelayedAreReady();
                return readyQueue.toArray(contents);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public Runnable poll() {
            checkedDelayedAreReady();
            return readyQueue.poll();
        }

        private void waitReady(long nanoDelay) throws InterruptedException {
            lock.lock();
            try {
                long delay = Math.min(nanoDelay, getDelay());
                if (delay > 0) {
                    available.await(delay, TimeUnit.NANOSECONDS);
                }
                checkedDelayedAreReady();
            } finally {
                lock.unlock();
            }
        }

        private void waitReady() throws InterruptedException {
            lock.lock();
            try {
                long delay = getDelay();
                if (delay == Long.MAX_VALUE) {
                    available.await();
                } else if (delay > 0) {
                    available.await(delay, TimeUnit.NANOSECONDS);
                }
                checkedDelayedAreReady();
            } finally {
                lock.unlock();
            }
        }

        private long getDelay() {
            lock.lock();
            try {
                ScheduledFutureTask<?> job = (ScheduledFutureTask<?>) delayQueue.peek();
                if (job == null) {
                    return Long.MAX_VALUE;
                } else {
                    return job.getDelay(TimeUnit.NANOSECONDS);
                }
            } finally {
                lock.unlock();
            }
        }

        private void checkedDelayedAreReady() {
            lock.lock();
            try {
                ScheduledFutureTask<?> job = null;
                while ((job = (ScheduledFutureTask<?>) delayQueue.peek()) != null) {
                    if (job.getDelay(TimeUnit.NANOSECONDS) <= 0) {
                        delayQueue.poll();
                        lock.unlock();
                        readyQueue.put(job);
                        lock.lock();
                    } else {
                        return;
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        public Runnable peek() {
            Runnable r = null;
            checkedDelayedAreReady();
            r = readyQueue.peek();
            return r;
        }

        @Override
        public boolean offer(Runnable e) {
            try {
                if (e != null) {
                    if (((ScheduledFutureTask<?>) e).isReady()) {
                        return readyQueue.offer((ScheduledFutureTask<?>) e);
                    } else {
                        return delayQueue.offer((ScheduledFutureTask<?>) e);
                    }
                }
                return false;
            } finally {
                available.signalAll();
            }
        }

        @Override
        public void put(Runnable e) throws InterruptedException {
            try {
                if (e != null) {
                    if (((ScheduledFutureTask<?>) e).isReady()) {
                        readyQueue.put((ScheduledFutureTask<?>) e);
                    } else {
                        delayQueue.put((ScheduledFutureTask<?>) e);
                    }
                }
            } finally {
                available.signalAll();
            }
        }

        @Override
        public boolean offer(Runnable e, long timeout, TimeUnit unit) throws InterruptedException {
            try {
                if (e != null) {
                    if (((ScheduledFutureTask<?>) e).isReady()) {
                        return readyQueue.offer((ScheduledFutureTask<?>) e, timeout, unit);
                    } else {
                        return delayQueue.offer((ScheduledFutureTask<?>) e, timeout, unit);
                    }
                } else {
                    return false;
                }
            } finally {
                lock.lock();
                available.signalAll();
                lock.unlock();
            }
        }

        @Override
        public Runnable take() throws InterruptedException {
            Runnable r = null;
            lock.lock();
            try {
                checkedDelayedAreReady();
                while (readyQueue.isEmpty()) {
                    waitReady();
                }
                r = readyQueue.take();
                return r;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
            lock.lock();
            try {
                long start = now();
                long delay = unit.toNanos(timeout);
                long soFar = 0;

                checkedDelayedAreReady();
                soFar = now() - start;
                Runnable job = null;
                while (soFar < delay && (job = readyQueue.poll()) == null) {
                    soFar = now() - start;
                    waitReady(delay - soFar);
                }
                return job;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public int remainingCapacity() {
            checkedDelayedAreReady();
            return readyQueue.remainingCapacity();
        }

        @Override
        public int drainTo(Collection<? super Runnable> c) {
            checkedDelayedAreReady();
            return readyQueue.drainTo(c);
        }

        @Override
        public int drainTo(Collection<? super Runnable> c, int maxElements) {
            checkedDelayedAreReady();
            return readyQueue.drainTo(c, maxElements);
        }

        @Override
        public Iterator<Runnable> iterator() {
            checkedDelayedAreReady();
            return readyQueue.iterator();
        }

        @Override
        public int size() {
            checkedDelayedAreReady();
            return readyQueue.size();
        }
    }

    private class ScheduledFutureTask<V> extends FutureTask<V> implements ScheduledFuture<V>, Priority {

        private Priority childPri = null;

        /** Sequence number to break ties FIFO */
        private final long sequenceNumber;

        /** The time the task is enabled to execute in nanoTime units */
        private long time;

        /**
         * Period in nanoseconds for repeating tasks. A positive value indicates fixed-rate execution. A negative value
         * indicates fixed-delay execution. A value of 0 indicates a non-repeating task.
         */
        private final long period;

        /** The actual task to be re-enqueued by reExecutePeriodic */
        ScheduledFutureTask<V> outerTask = this;

        /**
         * Index into delay queue, to support faster cancellation.
         */
        int heapIndex;

        String toStr = null;

        /**
         * Creates a one-shot action with given nanoTime-based trigger time.
         */
        ScheduledFutureTask(Runnable r, V result, long ns) {
            super(r, result);
            toStr = r.getClass().toString();
            this.time = ns;
            this.period = 0;
            this.sequenceNumber = sequencer.getAndIncrement();
            if (r instanceof Priority) {
                childPri = (Priority) r;
            }
        }

        /**
         * Creates a periodic action with given nano time and period.
         */
        ScheduledFutureTask(Runnable r, V result, long ns, long period) {
            super(r, result);
            toStr = r.getClass().toString();
            this.time = ns;
            this.period = period;
            this.sequenceNumber = sequencer.getAndIncrement();
            if (r instanceof Priority) {
                childPri = (Priority) r;
            }
        }

        /**
         * Creates a one-shot action with given nanoTime-based trigger.
         */
        ScheduledFutureTask(Callable<V> callable, long ns) {
            super(callable);
            toStr = callable.getClass().toString();
            this.time = ns;
            this.period = 0;
            this.sequenceNumber = sequencer.getAndIncrement();
            if (callable instanceof Priority) {
                childPri = (Priority) callable;
            }
        }

        public long getDelay(TimeUnit unit) {
            long d = time - now();
            return d <= 0 ? 0 : unit.convert(d, TimeUnit.NANOSECONDS);
        }

        public boolean isReady() {
            return getDelay(TimeUnit.NANOSECONDS) <= 0;
        }

        public int compareTo(Delayed other) {
            if (other == this) // compare zero ONLY if same object
                return 0;
            if (other instanceof Priority) {
                if (getDelay(TimeUnit.NANOSECONDS) <= 0 && other.getDelay(TimeUnit.NANOSECONDS) <= 0) {
                    if (getPriority() < ((Priority) other).getPriority()) {
                        return -1;
                    } else if (getPriority() > ((Priority) other).getPriority()) {
                        return 1;
                    }
                }
            } else if (getPriority() > Priority.NORMAL && getDelay(TimeUnit.NANOSECONDS) <= 0) {
                return 1;
            }
            if (other instanceof ScheduledFutureTask) {

                ScheduledFutureTask<?> x = (ScheduledFutureTask<?>) other;
                long diff = time - x.time;
                if (diff < 0)
                    return -1;
                else if (diff > 0)
                    return 1;
                else if (sequenceNumber < x.sequenceNumber)
                    return -1;
                else
                    return 1;
            }
            long d = (getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS));
            return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
        }

        /**
         * Returns true if this is a periodic (not a one-shot) action.
         *
         * @return true if periodic
         */
        public boolean isPeriodic() {
            return period != 0;
        }

        /**
         * Sets the next time to run for a periodic task.
         */
        private void setNextRunTime() {
            long p = period;
            if (p > 0)
                time += p;
            else
                time = now() - p;
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean cancelled = super.cancel(mayInterruptIfRunning);
            if (cancelled && removeOnCancel && heapIndex >= 0)
                remove(this);
            return cancelled;
        }

        /**
         * Overrides FutureTask version so as to reset/requeue if periodic.
         */
        public void run() {
            boolean periodic = isPeriodic();
            if (!canRunInCurrentRunState(periodic))
                cancel(false);
            else if (!periodic)
                ScheduledFutureTask.super.run();
            else if (ScheduledFutureTask.super.runAndReset()) {
                setNextRunTime();
                reExecutePeriodic(outerTask);
            }
        }

        @Override
        public int getPriority() {
            return (childPri != null ? childPri.getPriority() : Priority.NORMAL);
        }

        @Override
        public String toString() {
            return toStr;
        }

    }

    /**
     * Returns true if can run a task given current run state and run-after-shutdown parameters.
     *
     * @param periodic true if this task periodic, false if delayed
     */
    protected boolean canRunInCurrentRunState(boolean periodic) {
        return isRunningOrShutdown(periodic ? continueExistingPeriodicTasksAfterShutdown : executeExistingDelayedTasksAfterShutdown);
    }

    /**
     * Requeues a periodic task unless current run state precludes it. Same idea as delayedExecute except drops task
     * rather than rejecting.
     *
     * @param task the task
     */
    void reExecutePeriodic(ScheduledFutureTask<?> task) {
        if (canRunInCurrentRunState(true)) {
            super.getQueue().add(task);
            if (!canRunInCurrentRunState(true) && remove(task))
                task.cancel(false);
            else
                prestartCoreThread();
        }
    }

    private boolean isRunningOrShutdown(boolean continueAfter) {
        return (!isShutdown() ? true : continueAfter);
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        if (command == null || unit == null)
            throw new NullPointerException();
        long triggerTime = nextTriggerTime(delay, unit);
        ScheduledFutureTask<?> t = new ScheduledFutureTask<Void>(command, null, triggerTime);
        delayedExecute(t);
        return t;
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        if (callable == null || unit == null)
            throw new NullPointerException();
        long triggerTime = nextTriggerTime(delay, unit);
        ScheduledFutureTask<V> t = new ScheduledFutureTask<V>(callable, triggerTime);
        delayedExecute(t);
        return t;
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        if (command == null || unit == null)
            throw new NullPointerException();
        if (period <= 0)
            throw new IllegalArgumentException();
        if (initialDelay < 0)
            initialDelay = 0;
        long triggerTime = nextTriggerTime(initialDelay, unit);
        ScheduledFutureTask<Void> sft = new ScheduledFutureTask<Void>(command, null, triggerTime, unit.toNanos(period));
        sft.outerTask = sft;
        delayedExecute(sft);
        return sft;
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        if (command == null || unit == null)
            throw new NullPointerException();
        if (delay <= 0)
            throw new IllegalArgumentException();
        long triggerTime = nextTriggerTime(initialDelay, unit);
        ScheduledFutureTask<Void> sft = new ScheduledFutureTask<Void>(command, null, triggerTime, unit.toNanos(-delay));
        sft.outerTask = sft;
        delayedExecute(sft);

        return sft;
    }

    /**
     * Executes {@code command} with zero required delay. This has effect equivalent to
     * {@link #schedule(Runnable,long,TimeUnit) schedule(command, 0, anyUnit)}. Note that inspections of the queue and
     * of the list returned by {@code shutdownNow} will access the zero-delayed {@link ScheduledFuture}, not the
     * {@code command} itself.
     *
     * <p>
     * A consequence of the use of {@code ScheduledFuture} objects is that {@link ThreadPoolExecutor#afterExecute
     * afterExecute} is always called with a null second {@code Throwable} argument, even if the {@code command}
     * terminated abruptly. Instead, the {@code Throwable} thrown by such a task can be obtained via {@link Future#get}.
     *
     * @throws RejectedExecutionException at discretion of {@code RejectedExecutionHandler}, if the task cannot be
     *         accepted for execution because the executor has been shut down
     * @throws NullPointerException {@inheritDoc}
     */
    public void execute(Runnable command) {
        schedule(command, 0, TimeUnit.NANOSECONDS);
    }

    // Override AbstractExecutorService methods

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public Future<?> submit(Runnable task) {
        return schedule(task, 0, TimeUnit.NANOSECONDS);
    }

    // TODO Teng Check newly added
    public boolean remove(Runnable task) {
        if (task == null)
            throw new NullPointerException();
        long triggerTime = nextTriggerTime(0, TimeUnit.NANOSECONDS);
        ScheduledFutureTask<?> t = new ScheduledFutureTask<Void>(task, null, triggerTime);
        return super.remove(t);
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public <T> Future<T> submit(Runnable task, T result) {
        return schedule(Executors.callable(task, result), 0, TimeUnit.NANOSECONDS);
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public <T> Future<T> submit(Callable<T> task) {
        return schedule(task, 0, TimeUnit.NANOSECONDS);
    }

    /**
     * Returns the trigger time of a delayed action
     */
    private static long nextTriggerTime(long delay, TimeUnit unit) {
        long triggerTime;
        long now = now();
        if (delay <= 0)
            return now; // avoid negative trigger times
        else if ((triggerTime = now + unit.toNanos(delay)) < 0)
            return Long.MAX_VALUE; // avoid numerical overflow
        else
            return triggerTime;
    }

    private void delayedExecute(ScheduledFutureTask<?> task) {
        if (isShutdown())
            reject(task);
        else {
            super.getQueue().add(task);
            if (isShutdown() && !canRunInCurrentRunState(task.isPeriodic()) && remove(task))
                task.cancel(false);
            else
                prestartCoreThread();
        }
    }

    private void reject(ScheduledFutureTask<?> task) {
        // TODO
        // task.setException(new IllegalStateException());
        task.cancel(false);
    }

    public interface Priority {

        public static final int HIGH = -1;

        public static final int NORMAL = 0;

        public static final int LOW = 1;

        public int getPriority();
    }

}
