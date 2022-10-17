/*
 * Copyright (c) 2005, 2010, Oracle and/or its affiliates. All rights reserved.
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
package java.lang;

import java.util.*;

/**
 * Class to track and run user level shutdown hooks registered through
 * 类来跟踪和运行通过注册的用户级关闭挂钩
 * {@link Runtime#addShutdownHook Runtime.addShutdownHook}.
 *
 * @see java.lang.Runtime#addShutdownHook
 * @see java.lang.Runtime#removeShutdownHook
 */
    // 用户级别的钩子，允许开发者通过Runtime#addShutdownHook()来注册一些需要在虚拟机关闭前处理的钩子
    //在应用程序层 可以组策很多钩子。但在Shutdown 中Runnable[] hooks 列表最大是10。这可以理解为
    //Runnable[] hooks 钩子的种类最多10种。
    //注意：类的访问权限是 protect 用户是直接操作不了。
class ApplicationShutdownHooks {

    /** The set of registered hooks */
    // 钩子列表，底层是用一个数组存储，key和value是紧紧挨在一起
    private static IdentityHashMap<Thread, Thread> hooks;


    static {
        try {
            // 将指定的钩子注册到编号为1的插槽中，以便在虚拟机关闭时处理这些钩子
            Shutdown.add(1     /* shutdown hook invocation order */, false /* not registered if shutdown in progress */, new Runnable() {
                public void run() {
                    runHooks();
                }
            });

            hooks = new IdentityHashMap<>();
        } catch(IllegalStateException e) {
            /* application shutdown hooks cannot be added if shutdown is in progress */
            hooks = null;
        }
    }


    private ApplicationShutdownHooks() {
    }

    /**
     * Add a new shutdown hook.
     * Checks the shutdown state and the hook itself, but does not do any security checks.
     */
    // 将指定的线程作为钩子添加到系统钩子列表中，以便虚拟机关闭时可以执行它
    static synchronized void add(Thread hook) {
        if(hooks == null) {
            throw new IllegalStateException("Shutdown in progress");
        }

        if(hook.isAlive()) {
            throw new IllegalArgumentException("Hook already running");
        }

        if(hooks.containsKey(hook)) {
            throw new IllegalArgumentException("Hook previously registered");
        }

        hooks.put(hook, hook);
    }

    /**
     * Remove a previously-registered hook.
     * Like the add method, this method does not do any security checks.
     */
    // 移除已注册的钩子
    static synchronized boolean remove(Thread hook) {
        if(hooks == null) {
            throw new IllegalStateException("Shutdown in progress");
        }

        if(hook == null) {
            throw new NullPointerException();
        }

        return hooks.remove(hook) != null;
    }

    /**
     * Iterates over all application hooks creating a new thread for each to run in.
     * Hooks are run concurrently and this method waits for them to finish.
     */
    //遍历所有应用程序挂钩，为每个挂钩创建一个新线程。钩子是并发运行的，此方法等待钩子完成。
    // 运行注册的钩子
    static void runHooks() {
        Collection<Thread> threads;

        // 获取钩子列表
        synchronized(ApplicationShutdownHooks.class) {
            threads = hooks.keySet();
            hooks = null;
        }

        // 遍历注册的钩子，并执行它
        for(Thread hook : threads) {
            hook.start();
        }

        // 等待所有钩子执行完
        for(Thread hook : threads) {
            while(true) {
                try {
                    /*
                     * 使当前线程进入WAITING状态，直到hook线程死亡之后，再去执行当前线程。
                     *
                     * join 的方法逻辑如下:
                     * A线程执行B线程对象的join方法，A线程会进入waitting状态，而B线程
                     * 仍然执行。B执行完之后，唤醒A线程继续执行。
                     *
                     * 当前线程 执行每一个钩子线程对象的join方法，在所有的钩子线程
                     * 执行完之后，A线程才会被唤醒继续执行。
                     * 换句话说，所有钩子线程执行完之前，当前线程不会退出。
                     * join底层调用的就是wait()方法。
                     */
                    hook.join();
                    break;
                } catch(InterruptedException ignored) {
                }
            }
        }
    }
}
