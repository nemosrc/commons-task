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

/**
 * A task that can be scheduled for one-time or repeated execution by a WheelTimer.
 *
 * @author  Nemo
 * @see     WheelTimer
 */
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

    /**
     * Overrides the run method to execute the created work.
     */
    @Override
    public abstract void run();

    /**
     * Cancels a registered task.<br>
     * After the cancellation, the run method is no longer called in the future.
     *
     * @return Returns true if successfully removed from the queue.
     */
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
