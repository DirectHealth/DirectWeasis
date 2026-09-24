/*
 * Copyright (c) 2024 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.tp.raven.datetime.component.date.event;

import java.util.EventObject;

/**
 * DateControlEvent is a class that provides states for date events.
 *
 * @author Raven Laing
 * @see <a href="https://github.com/DJ-Raven/swing-datetime-picker">swing-datetime-picker</a>
 */
public class DateControlEvent extends EventObject {

  public static final int DAY_STATE = 1;
  public static final int MONTH_STATE = 2;
  public static final int YEAR_STATE = 3;

  public static final int BACK = 10;
  public static final int FORWARD = 11;
  public static final int MONTH = 12;
  public static final int YEAR = 13;

  protected int state;
  protected int type;

  public DateControlEvent(Object source, int state, int type) {
    super(source);
    this.state = state;
    this.type = type;
  }

  public int getState() {
    return state;
  }

  public int getType() {
    return type;
  }
}
