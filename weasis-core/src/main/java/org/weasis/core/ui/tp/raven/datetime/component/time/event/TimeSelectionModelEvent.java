/*
 * Copyright (c) 2024 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.tp.raven.datetime.component.time.event;

import java.util.EventObject;

/**
 * {@link TimeSelectionModelEvent}
 *
 * @author Raven Laing
 * @see <a href="https://github.com/DJ-Raven/swing-datetime-picker">swing-datetime-picker</a>
 */
public class TimeSelectionModelEvent extends EventObject {

  public static final int HOUR = 1;
  public static final int MINUTE = 2;
  public static final int HOUR_MINUTE = 3;

  protected int action;

  public TimeSelectionModelEvent(Object source, int action) {
    super(source);
    this.action = action;
  }

  public int getAction() {
    return action;
  }
}
