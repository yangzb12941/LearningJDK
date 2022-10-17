package java.util;

/**
 * This "helper class" implements the timer's task execution thread, which
 * waits for tasks on the timer queue, executions them when they fire,
 * reschedules repeating tasks, and removes cancelled tasks and spent
 * non-repeating tasks from the queue.
 */
// 这个“helper类”实现计时器的任务执行线程，该线程等待计时器队列上的任务，
// 在它们触发时执行它们，重新安排重复任务，并从队列中删除取消的任务和花费的非重复任务。
// 定时器线程
class TimerThread extends Thread {
    /**
     * This flag is set to false by the reaper to inform us that there are no more live references to our Timer object.
     * Once this flag is true and there are no more tasks in our queue,
     * there is no work left for us to do, so we terminate gracefully.
     * Note that this field is protected by queue's monitor!
     */
    // 定时器将此标志设置为false，以通知我们不再有对Timer对象的活动引用。
    // 一旦此标志为true，并且队列中没有更多任务，就没有剩余的工作要做，
    // 因此我们可以优雅地终止。请注意，此字段受队列监视器的保护！
    // 定时器是否已取消（不再执行新任务）
    boolean newTasksMayBeScheduled = true;

    /**
     * Our Timer's queue.  We store this reference in preference to
     * a reference to the Timer so the reference graph remains acyclic.
     * Otherwise, the Timer would never be garbage-collected and this
     * thread would never go away.
     */
    // 我们的定时器队列。我们优先存储这个引用，而不是Timer的引用，
    // 因此引用图保持非循环。否则，计时器将永远不会被垃圾收集，并且此线程也永远不会消失。
    // 任务队列
    private TaskQueue queue;

    TimerThread(TaskQueue queue) {
        this.queue = queue;
    }

    // 初始化定时器后，定时器线程随之启动
    public void run() {
        try {
            // 进入定时器主循环
            mainLoop();
        } finally {
            // Someone killed this Thread, behave as if Timer cancelled
            // 有人杀死了这个线程，表现得好像计时器被取消了一样
            synchronized(queue) {
                newTasksMayBeScheduled = false;
                // 清空任务队列
                queue.clear();  // Eliminate obsolete references
            }
        }
    }

    /**
     * The main timer loop.  (See class comment.)
     */
    // 定时器主循环
    private void mainLoop() {
        for(; ; ) {
            try {
                TimerTask task;
                boolean taskFired;

                synchronized(queue) {
                    // Wait for queue to become non-empty
                    // 等待队列变为非空
                    while(queue.isEmpty() && newTasksMayBeScheduled) {
                        // 如果任务队列为空，且定时器未取消，则阻塞定时器线程，等待任务到来
                        queue.wait();
                    }

                    // 定时器线程醒来后，如果队列为空，且定时器已取消，直接退出
                    if(queue.isEmpty()) {
                        break; // Queue is empty and will forever remain; die
                    }

                    /* 至此，任务队列不为空 */

                    // Queue nonempty; look at first evt and do the right thing
                    // 队列非空；看看第一个evt，做正确的事情
                    long currentTime, executionTime;

                    // 获取队头任务
                    task = queue.getMin();

                    synchronized(task.lock) {
                        // 如果该任务已被取消
                        if(task.state == TimerTask.CANCELLED) {
                            // 移除队头任务，并将触发时间最近的任务放在队头
                            queue.removeMin();
                            // 重新开始主循环
                            continue;  // No action required, poll queue again
                        }

                        // 任务触发时间
                        executionTime = task.nextExecutionTime;

                        // 当前时间（可以近似地认为是任务本次实际触发时间）
                        currentTime = System.currentTimeMillis();

                        // 如果任务可以开始执行了
                        if(taskFired = (executionTime<=currentTime)) {
                            // 一次性任务，执行完就移除
                            if(task.period == 0) {
                                // 移除队头任务，并将触发时间最近的任务放在队头
                                queue.removeMin();
                                // 任务进入【执行】状态
                                task.state = TimerTask.EXECUTED;
                            } else {
                                // 计算重复性任务的下次触发时间
                                long newTime = task.period<0
                                    ? currentTime - task.period     // 固定延时，任务下次的触发时间=任务本次实际触发时间+(-period)
                                    : executionTime + task.period;  // 固定周期，从任务初次被触发开始，以后每隔period时间就被触发一次

                                // 重置队头任务的触发时间，并将其调整到队列中的合适位置
                                // 重复任务，设置完下一次的执行时间，在定时任务队列中根据下次运行时间重新排队。
                                // 任务并没有重队列中移除。
                                queue.rescheduleMin(newTime);
                            }
                        }
                    }

                    // 如果任务还未到触发时间，定时器线程进入阻塞
                    if(!taskFired) {
                        // Task hasn't yet fired; wait
                        queue.wait(executionTime - currentTime);
                    }
                }

                // 如果任务可以开始执行了
                if(taskFired) {
                    // Task fired; run it, holding no locks
                    task.run();
                }
            } catch(InterruptedException e) {
            }
        }// for(; ; )
    }
}
