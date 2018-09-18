/*
 * Copyright (c) 2018 NemoSW
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.nemosw.commons.task;

import java.util.Arrays;
import java.util.function.LongSupplier;

import static com.nemosw.commons.task.WheelTask.*;

/**
 * A timer that supports circular tasks.<br>
 * * This class is not thread-safe
 *
 * @author Nemo
 * @see WheelTask
 */
public class WheelTimer implements Runnable
{
    private final WheelQueue[] wheel;

    private final LongSupplier movement;

    private long currentTicks;

    /**
     * Constructs an empty timer with the specified initial capacity
     *
     * @param maxTick timer in support ticks
     * @see WheelTimer#WheelTimer(int, LongSupplier)
     * @see WheelTimer#WheelTimer(int, LongSupplier, long)
     */
    public WheelTimer(int maxTick)
    {
        this(maxTick, null);
    }

    /**
     * Constructs an empty timer with the specified initial capacity and time based movement
     *
     * @param maxTick      timer in support ticks
     * @param timeSupplier time supplier {@code System::nanoTime() or System::currentTimeMillis()}
     * @param interval     tick interval time
     * @see WheelTimer#WheelTimer(int)
     * @see WheelTimer#WheelTimer(int, LongSupplier)
     */
    public WheelTimer(int maxTick, final LongSupplier timeSupplier, final long interval)
    {
        this(maxTick, () -> timeSupplier.getAsLong() / interval);
    }

    /**
     * onstructs an empty timer with the specified initial capacity and movement
     *
     * @param maxTick timer in support ticks
     * @param movement timer tick movement
     */
    public WheelTimer(int maxTick, LongSupplier movement)
    {
        if (maxTick <= 0)
            throw new IllegalArgumentException("Illegal maxTick " + maxTick);

        this.wheel = new WheelQueue[maxTick + 1];
        this.movement = movement;

        if (movement != null)
            this.currentTicks = movement.getAsLong();
    }

    /**
     * Register a one-time task.
     *
     * @param task Task to run in the future
     */
    public void schedule(WheelTask task)
    {
        registerTask(task, 0, 0);
    }

    /**
     * Register a one-time task with delay.
     *
     * @param task  Task to run in the future
     * @param delay Delay ticks
     */
    public void schedule(WheelTask task, int delay)
    {
        registerTask(task, Math.max(0, delay), 0);
    }

    /**
     * Register a repeating task.
     *
     * @param task   Task to run in the future
     * @param delay  Delay ticks
     * @param period Repeat delay ticks
     */
    public void schedule(WheelTask task, int delay, int period)
    {
        registerTask(task, Math.max(0, delay), Math.max(1, period));
    }

    private void registerTask(WheelTask task, int delay, int period)
    {
        if (task.state == SCHEDULED)
            throw new IllegalArgumentException("Already scheduled task " + task.toString());

        WheelQueue[] wheel = this.wheel;
        int length = wheel.length;

        if (delay >= length)
            throw new IllegalArgumentException("Illegal delay " + delay);
        if (period >= length)
            throw new IllegalArgumentException("Illegal period " + period);

        if (task.state == RUNNING)
            task.queue.unlinkFirst(task);

        long currentTicks = this.currentTicks;

        task.state = SCHEDULED;
        task.period = period;
        long nextRun = currentTicks + delay;
        task.nextRun = nextRun;
        int index = (int) (nextRun % length);

        WheelQueue queue = wheel[index];

        if (queue == null)
            wheel[index] = queue = new WheelQueue();

        queue.linkLast(task);
    }

    /**
     * Executes registered tasks in a tickly manner via the run method.
     * Tasks are called only once, Regardless of elapsed ticks
     */
    @Override
    public void run()
    {
        long taskTicks = this.currentTicks;
        long currentTicks = this.movement == null ? taskTicks + 1 : this.movement.getAsLong();

        if (taskTicks <= currentTicks)
        {
            if (taskTicks < currentTicks)
                this.currentTicks = currentTicks;

            WheelQueue[] wheel = this.wheel;
            int length = wheel.length;
            long targetTick = Math.min(currentTicks, taskTicks + length - 1); // one rotate

            do
            {
                int index = (int) (taskTicks % length);
                WheelQueue queue = wheel[index];

                if (queue != null)
                {
                    WheelTask task;

                    while ((task = queue.peek()) != null && task.nextRun <= currentTicks)
                    {
                        task.state = RUNNING;

                        try
                        {
                            task.run();
                        }
                        catch (Throwable t)
                        {
                            t.printStackTrace();
                        }

                        if (task.state == RUNNING)
                        {
                            queue.unlinkFirst(task);

                            int period = task.period;

                            if (period > 0)
                            {
                                long nextRun = currentTicks + period;
                                task.nextRun = nextRun;
                                int futureIndex = (int) (nextRun % length);
                                WheelQueue futureQueue = wheel[futureIndex];

                                if (futureQueue == null)
                                    wheel[futureIndex] = futureQueue = new WheelQueue();

                                futureQueue.linkLast(task);
                            }
                            else
                            {
                                task.state = EXECUTED;
                            }
                        }
                    }
                }
            }
            while (++taskTicks <= targetTick);
        }
    }

    /**
     * Cancels all tasks.
     */
    public void clear()
    {
        WheelQueue[] wheel = this.wheel;

        for (WheelQueue queue : wheel)
        {
            if (queue != null)
            {
                WheelTask task = queue.peek();

                if (task != null)
                {
                    queue.unlinkFirst(task);
                    task.state = CANCELLED;
                }
            }
        }

        Arrays.fill(wheel, null);
    }
}
