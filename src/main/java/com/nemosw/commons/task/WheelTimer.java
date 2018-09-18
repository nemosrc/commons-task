package com.nemosw.commons.task;

import java.util.Arrays;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static com.nemosw.commons.task.WheelTask.*;

public final class WheelTimer implements Runnable
{

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private int maxTick = 0;

        private Supplier<LongSupplier> movementSupplier;

        private Builder() {}

        public Builder maxTick(int maxTick)
        {
            if (maxTick <= 0)
                throw new IllegalArgumentException("max tick must be greater than 0");

            this.maxTick = maxTick;

            return this;
        }

        public Builder movement(LongSupplier movement)
        {
            if (movement == null)
                throw new NullPointerException("movement cannot be null");

            this.movementSupplier = () -> movement;

            return this;
        }

        public Builder movement(final LongSupplier timeSupplier, final long interval)
        {
            if (timeSupplier == null)
                throw new NullPointerException("time supplier cannot be null");

            this.movementSupplier = () -> {
                final long initTime = timeSupplier.getAsLong();
                return () -> (timeSupplier.getAsLong() - initTime) / interval;
            };

            return this;
        }

        public WheelTimer build()
        {
            int maxTick = this.maxTick;

            if (maxTick == 0)
                throw new IllegalStateException("max tick has not been initialized.");

            Supplier<LongSupplier> movementSupplier = this.movementSupplier;
            LongSupplier movement = movementSupplier != null ? movementSupplier.get() : new LongSupplier()
            {

                private int ticks;

                @Override
                public long getAsLong()
                {
                    return ticks++;
                }
            };

            return new WheelTimer(maxTick, movement);
        }
    }

    private final WheelQueue[] wheel;

    private final LongSupplier movement;

    private long currentTicks;

    private WheelTimer(int maxTick, LongSupplier movement)
    {
        this.wheel = new WheelQueue[maxTick + 1];
        this.movement = movement;
    }

    public void schedule(WheelTask task)
    {
        registerTask(task, 0, 0);
    }

    public void schedule(WheelTask task, int delay)
    {
        registerTask(task, Math.max(0, delay), 0);
    }

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

    @Override
    public void run()
    {
        long taskTicks = this.currentTicks;
        long currentTicks = this.movement.getAsLong();

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
