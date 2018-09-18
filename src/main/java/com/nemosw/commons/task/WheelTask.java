package com.nemosw.commons.task;

public abstract class WheelTask implements Runnable
{
    static final int VIRGIN = 0;

    static final int SCHEDULED = 1;

    static final int RUNNING = 2;

    static final int EXECUTED = 3;

    static final int CANCELLED = 4;

    WheelQueue queue;

    WheelTask prev, next;

    int state = VIRGIN;

    int period;

    long nextRun;

    @Override
    public abstract void run();

    public boolean cancel()
    {
        WheelQueue queue = this.queue;

        if (queue == null)
        {
            return false;
        }

        queue.unlink(this);
        state = CANCELLED;
        return true;
    }
}
