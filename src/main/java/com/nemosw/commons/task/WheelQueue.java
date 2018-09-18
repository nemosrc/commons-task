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

final class WheelQueue
{
    private WheelTask first, last;

    void linkLast(WheelTask task)
    {
        final WheelTask l = last;
        task.prev = l;
        last = task;
        if (l == null)
            first = task;
        else
            l.next = task;
        task.queue = this;
    }

    WheelTask peek()
    {
        return this.first;
    }

    void unlink(WheelTask x)
    {
        final WheelTask next = x.next;
        final WheelTask prev = x.prev;

        if (prev == null)
        {
            first = next;
        }
        else
        {
            prev.next = next;
            x.prev = null;
        }

        if (next == null)
        {
            last = prev;
        }
        else
        {
            next.prev = prev;
            x.next = null;
        }
        x.queue = null;
    }

    void unlinkFirst(WheelTask f)
    {
        final WheelTask next = f.next;
        f.queue = null;
        f.next = null; // help GC
        first = next;
        if (next == null)
            last = null;
        else
            next.prev = null;
    }
}
