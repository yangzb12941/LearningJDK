package java.util;

/**
 * This class represents a timer task queue: a priority queue of TimerTasks,
 * ordered on nextExecutionTime.  Each Timer object has one of these, which it
 * shares with its TimerThread.  Internally this class uses a heap, which
 * offers log(n) performance for the add, removeMin and rescheduleMin
 * operations, and constant time performance for the getMin operation.
 */
// 此类表示计时器任务队列：TimerTasks的优先级队列，按nextExecutionTime排序。
// 每个Timer对象都有其中一个，它与其TimerThread共享。在内部，该类使用堆，
// 堆为add、removeMin和rescheduleMin操作提供log（n）性能，并为getMin操作带来恒定的时间性能。
// 定时器任务队列
class TaskQueue {
    /**
     * Priority queue represented as a balanced binary heap: the two children
     * of queue[n] are queue[2*n] and queue[2*n+1].  The priority queue is
     * ordered on the nextExecutionTime field: The TimerTask with the lowest
     * nextExecutionTime is in queue[1] (assuming the queue is nonempty).  For
     * each node n in the heap, and each descendant of n, d,
     * n.nextExecutionTime <= d.nextExecutionTime.
     */
    // 优先级队列表示为平衡的二进制堆：队列[n]的两个子队列是队列[2n]和队列[2n+1](用数组的方式，实现"堆"数据结构的操作)。
    // 优先级队列在nextExecutionTime字段上排序：nextExecutionTime 最低的TimerTask位于队列[1]中（假设队列非空）。
    // 对于堆中的每个节点n，以及n、d、n.nextExecutionTime的每个后代。
    // 小顶堆的实现，以距离运行时间排序。
    // 任务队列，实际存储任务的地方，索引0处空闲
    private TimerTask[] queue = new TimerTask[128];

    /**
     * The number of tasks in the priority queue.  (The tasks are stored in
     * queue[1] up to queue[size]).
     * 优先级队列中的任务数。（任务存储在队列[1]中，直到队列[大小]）。
     */
    // 任务数量
    private int size = 0;

    /**
     * Adds a new task to the priority queue.
     */
    // 将任务送入任务队列排队
    // 注意：并不是线程安全的方法
    void add(TimerTask task) {
        // Grow backing store if necessary
        // 必要时增加后备存储->扩容
        if(size + 1 == queue.length) {
            // 扩容
            queue = Arrays.copyOf(queue, 2 * queue.length);
        }

        //注意：++size
        queue[++size] = task;

        // 调整size处的任务到队列中的合适位置
        // size 已经是当前 task 在任务队列的位置
        fixUp(size);
    }

    /**
     * Return the "head task" of the priority queue.
     * (The head task is an task with the lowest nextExecutionTime.)
     * 返回优先级队列的“头任务”。（头任务是nextExecutionTime最低的任务。）
     */
    // 获取队头任务
    TimerTask getMin() {
        return queue[1];
    }

    /**
     * Return the ith task in the priority queue, where i ranges from 1 (the
     * head task, which is returned by getMin) to the number of tasks on the
     * queue, inclusive.
     */
    // 获取索引i处的任务
    TimerTask get(int i) {
        return queue[i];
    }

    /**
     * Remove the head task from the priority queue.
     */
    // 移除队头任务，并将触发时间最近的任务放在队头
    void removeMin() {
        // 先将队尾任务放到队头
        queue[1] = queue[size];
        queue[size--] = null;  // Drop extra reference to prevent memory leak 删除额外引用以防止内存泄漏
        // 调整当前队头任务（之前的队尾任务）到队列中合适的位置
        fixDown(1);
    }

    /**
     * Removes the ith element from queue without regard for maintaining
     * the heap invariant.  Recall that queue is one-based, so
     * 1 <= i <= size.
     */
    // 快速移除索引i处的任务（没有重建小顶堆）
    void quickRemove(int i) {
        assert i<=size;

        queue[i] = queue[size];
        queue[size--] = null;  // Drop extra ref to prevent memory leak
    }

    /**
     * Sets the nextExecutionTime associated with the head task to the
     * specified value, and adjusts priority queue accordingly.
     */
    // 重置队头任务的触发时间，并将其调整到队列中的合适位置
    void rescheduleMin(long newTime) {
        // 重置队头任务的触发时间
        queue[1].nextExecutionTime = newTime;
        // 将该任务调整到队列中的合适位置
        fixDown(1);
    }

    /**
     * Removes all elements from the priority queue.
     */
    // 清空任务队列
    void clear() {
        // Null out task references to prevent memory leak
        for(int i = 1; i<=size; i++) {
            queue[i] = null;
        }

        size = 0;
    }

    /**
     * Returns true if the priority queue contains no elements.
     */
    // 判断队列是否为空
    boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the number of tasks currently on the queue.
     */
    // 返回队列长度
    int size() {
        return size;
    }

    /**
     * Establishes the heap invariant (described above) in the entire tree,
     * assuming nothing about the order of the elements prior to the call.
     */
    // 重建小顶堆
    void heapify() {
        for(int i = size / 2; i >= 1; i--) {
            fixDown(i);
        }
    }

    /**
     * Establishes the heap invariant (described above) assuming the heap
     * satisfies the invariant except possibly for the leaf-node indexed by k
     * (which may have a nextExecutionTime less than its parent's).
     * 建立堆不变量（如上所述），假设堆满足不变量，但可能由k索引的叶节点除外（其nextExecutionTime可能小于其父节点）。
     *
     * This method functions by "promoting" queue[k] up the hierarchy
     * (by swapping it with its parent) repeatedly until queue[k]'s
     * nextExecutionTime is greater than or equal to that of its parent.
     * 此方法通过重复“提升”队列[k]的层次结构（通过将其与其父级交换），直到队列[k]'s nextExecutionTime大于或等于其父级。
     */
    // 插入。需要从小顶堆的结点k开始，向【上】查找一个合适的位置插入原k索引处的任务
    private void fixUp(int k) {
        while(k>1) {
            // 获取父结点索引
            int j = k >> 1;

            // 如果待插入元素大于父节点中的元素，则退出循环
            if(queue[k].nextExecutionTime>=queue[j].nextExecutionTime) {
                break;
            }

            // 子结点保存父结点中的元素
            TimerTask tmp = queue[j];
            queue[j] = queue[k];
            queue[k] = tmp;

            // 向上搜寻合适的插入位置
            k = j;
        }
    }

    /**
     * Establishes the heap invariant (described above) in the subtree
     * rooted at k, which is assumed to satisfy the heap invariant except
     * possibly for node k itself (which may have a nextExecutionTime greater
     * than its children's).
     *
     * 在以k为根的子树中建立堆不变量（如上所述），假设该不变量满足堆不变量，
     * 但节点k本身可能除外（其nextExecutionTime可能大于其子节点）。
     *
     * This method functions by "demoting" queue[k] down the hierarchy
     * (by swapping it with its smaller child) repeatedly until queue[k]'s
     * nextExecutionTime is less than or equal to those of its children.
     *
     * 此方法通过在层次结构中“降级”队列[k]（通过将其与较小的子队列交换）来实现，
     * 直到队列[k]'s nextExecutionTime小于或等于其子队列的nextExcecutionTime。
     */
    // 插入。需要从小顶堆的结点k开始，向【下】查找一个合适的位置插入原k索引处的任务
    private void fixDown(int k) {
        int j;

        while((j = k << 1)<=size && j>0) {
            // 让j存储子结点中较小结点的索引
            if(j<size && queue[j].nextExecutionTime>queue[j + 1].nextExecutionTime) {
                j++; // j indexes smallest kid
            }

            // 如果待插入元素小于子结点中较小的元素，则退出循环
            if(queue[k].nextExecutionTime<=queue[j].nextExecutionTime) {
                break;
            }

            // 父结点位置保存子结点中较小的元素
            TimerTask tmp = queue[j];
            queue[j] = queue[k];
            queue[k] = tmp;

            // 向下搜寻合适的插入位置
            k = j;
        }
    }
}

