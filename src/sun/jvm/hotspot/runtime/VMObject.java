/*
 * Copyright (c) 2000, 2002, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */

package sun.jvm.hotspot.runtime;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.types.*;

/** This is a base class for all VM runtime objects which wrap
    Addresses. The rationale is that without this in place, every
    class would have to implement equals() and hashCode() with
    boilerplate code, a practice which is inherently error-prone. */
//这是包装地址的所有VM运行时对象的基类。其基本原理是，如果没有这一点，
// 每个类都必须使用样板代码实现equals（）和hashCode（），这是一种天生容易出错的实践。
public class VMObject {
  protected Address addr;

  /** All of the objects have this as their constructor's signature
      anyway */
  //无论如何，所有对象都将此作为其构造函数的签名
  public VMObject(Address addr) {
    this.addr = addr;
  }

  public String toString() {
    return getClass().getName() + "@" + addr;
  }

  public boolean equals(Object arg) {
    if (arg == null) {
      return false;
    }

    if (!getClass().equals(arg.getClass())) {
      return false;
    }

    VMObject obj = (VMObject) arg;
    if (!addr.equals(obj.addr)) {
      return false;
    }

    return true;
  }

  public int hashCode() {
    return addr.hashCode();
  }

  public Address getAddress() {
    return addr;
  }
}
