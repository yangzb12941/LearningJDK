/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.util;

/**
 * A task that can be scheduled for one-time or repeated execution by a
 * {@link Timer}.
 * {@link Timer}可以安排一次性或重复执行的任务。
 *
 * <p>A timer task is <em>not</em> reusable.  Once a task has been scheduled
 * for execution on a {@code Timer} or cancelled, subsequent attempts to
 * schedule it for execution will throw {@code IllegalStateException}.
 * <p>计时器任务是不可重用的。一旦任务被安排在{@code Timer}上执行或被取消，
 * 随后计划执行任务的尝试将引发{@code IllegalStateException}。
 *
 * @author Josh Bloch
 * @since 1.3
 */
// 定时任务
public abstract class TimerTask implements Runnable {
    /**
     * This task has not yet been scheduled.
     * 尚未计划此任务。
     */
    static final int VIRGIN    = 0; // 【初始化】
    /**
     * This task is scheduled for execution.  If it is a non-repeating task,
     * it has not yet been executed.
     *
     * 此任务计划执行。如果是非重复任务，则尚未执行。
     */
    static final int SCHEDULED = 1; // 【排队】
    /**
     * This non-repeating task has already executed (or is currently
     * executing) and has not been cancelled.
     *
     * 此非重复任务已执行（或当前正在执行），尚未取消。
     */
    static final int EXECUTED  = 2; // 【执行】
    /**
     * This task has been cancelled (with a call to TimerTask.cancel).
     *
     * 此任务已取消（调用TimerTask.cancel）。
     */
    static final int CANCELLED = 3; // 【取消】

    /**
     * The state of this task, chosen from the constants below.
     * 此任务的状态，从以下常量中选择。
     */
    int state = VIRGIN; // 任务状态

    /**
     * Next execution time for this task in the format returned by
     * System.currentTimeMillis, assuming this task is scheduled for execution.
     * For repeating tasks, this field is updated prior to each task execution.
     */
    // 以系统返回的格式执行此任务的下一次执行时间。currentTimeMillis，
    // 假设此任务计划执行。对于重复任务，此字段将在每次执行任务之前更新。
    // 任务触发时间
    long nextExecutionTime;

    /**
     * Period in milliseconds for repeating tasks.  A positive value indicates
     * fixed-rate execution.  A negative value indicates fixed-delay execution.
     * A value of 0 indicates a non-repeating task.
     */
    /*
     * 任务的重复模式：
     * 零：非重复任务：只执行一次
     * 正数：重复性任务：固定周期，从任务初次被触发开始，以后每隔period时间就被触发一次
     * 负数：重复性任务：固定延时，任务下次的开始时间=任务上次结束时间+(-period)
     */
    long period = 0;

    /**
     * This object is used to control access to the TimerTask internals.
     *
     * 此对象用于控制对TimerTask内部的访问。
     */
    final Object lock = new Object();

    /**
     * Creates a new timer task.
     */
    protected TimerTask() {
    }

    /**
     * The action to be performed by this timer task.
     * 此计时器任务要执行的操作。
     */
    // 执行任务
    public abstract void run();

    /**
     * Cancels this timer task.  If the task has been scheduled for one-time
     * execution and has not yet run, or has not yet been scheduled, it will
     * never run.  If the task has been scheduled for repeated execution, it
     * will never run again.  (If the task is running when this call occurs,
     * the task will run to completion, but will never run again.)
     *
     * 取消此计时器任务。如果任务已计划为一次性执行，但尚未运行或尚未计划，则它将永远不会运行。
     * 如果任务被安排重复执行，它将永远不会再次运行。（如果此调用发生时任务正在运行，
     * 则任务将一直运行到完成，但不会再次运行。）
     *
     * <p>Note that calling this method from within the {@code run} method of
     * a repeating timer task absolutely guarantees that the timer task will
     * not run again.
     *
     * 请注意，从重复计时器任务的{@code-run}方法中调用此方法绝对可以保证计时器任务不会再次运行。
     *
     * <p>This method may be called repeatedly; the second and subsequent
     * calls have no effect.
     *
     * 此方法可以重复调用；第二个调用和随后的调用无效。
     *
     * @return true if this task is scheduled for one-time execution and has
     * not yet run, or this task is scheduled for repeated execution.
     * Returns false if the task was scheduled for one-time execution
     * and has already run, or if the task was never scheduled, or if
     * the task was already cancelled.  (Loosely speaking, this method
     * returns {@code true} if it prevents one or more scheduled
     * executions from taking place.)
     *
     * 如果此任务计划一次性执行但尚未运行，或者此任务计划重复执行，则为true。
     * 如果任务计划为一次性执行且已运行，或者从未计划任务，或者任务已取消，则返回false。
     * （松散地说，如果此方法阻止一个或多个计划执行，则返回{@code true}。）
     */
    // 取消处于【排队】状态的任务
    public boolean cancel() {
        synchronized(lock) {
            // 如果任务处于【排队】状态，则可以取消
            boolean result = (state == SCHEDULED);
            // 任务进入【取消】状态
            state = CANCELLED;
            return result;
        }
    }

    /**
     * Returns the <i>scheduled</i> execution time of the most recent
     * <i>actual</i> execution of this task.  (If this method is invoked
     * while task execution is in progress, the return value is the scheduled
     * execution time of the ongoing task execution.)
     *
     * 返回此任务最近的实际执行时间。（如果在任务执行过程中调用此方法，则返回值为正在执行的任务的计划执行时间。）
     *
     * <p>This method is typically invoked from within a task's run method, to
     * determine whether the current execution of the task is sufficiently
     * timely to warrant performing the scheduled activity:
     *
     * 此方法通常从任务的运行方法中调用，以确定任务的当前执行是否足够及时，以保证执行计划的活动：
     *
     * <pre>{@code
     *   public void run() {
     *       if (System.currentTimeMillis() - scheduledExecutionTime() >=
     *           MAX_TARDINESS)
     *               return;  // Too late; skip this execution.
     *       // Perform the task
     *   }
     * }</pre>
     * This method is typically <i>not</i> used in conjunction with
     * <i>fixed-delay execution</i> repeating tasks, as their scheduled
     * execution times are allowed to drift over time, and so are not terribly
     * significant.
     *
     * 这种方法通常不与固定延迟执行重复任务结合使用，因为它们的计划执行时间可以随时间推移而变化，因此不太重要。
     *
     * @return the time at which the most recent execution of this task was
     * scheduled to occur, in the format returned by Date.getTime().
     * The return value is undefined if the task has yet to commence
     * its first execution.
     *
     * 计划执行此任务的最新时间，格式为Date.getTime（）返回的格式。如果任务尚未开始第一次执行，则返回值未定义。
     *
     * @see Date#getTime()
     */
    // 返回任务被安排去【排队】时的时间
    public long scheduledExecutionTime() {
        synchronized(lock) {
            return (period<0
                ? nextExecutionTime + period
                : nextExecutionTime - period);
        }
    }
}
