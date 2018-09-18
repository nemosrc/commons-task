package com.nemosw.commons.task;

final class WheelQueue
{
    private WheelTask first, last;

//    void linkFirst(WheelTask task)
//    {
//        final WheelTask f = this.first;
//        task.next = f;
//        first = task;
//        if (f == null)
//            last = task;
//        else
//            f.prev = task;
//        task.queue = this;
//    }

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
