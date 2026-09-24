/*
 * Copyright (c) 2024 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.tp.raven.datetime.component.time;

import com.formdev.flatlaf.util.Animator;
import com.formdev.flatlaf.util.CubicBezierEasing;
import java.awt.Component;

/* AnimationChange is a class that provides animations for the TimeSpinner.
 *
 * @author Raven Laing
 * @see <a href="https://github.com/DJ-Raven/swing-datetime-picker">swing-datetime-picker</a>
 */
public class AnimationChange {

  public final Animator animator;

  private final AnimationValue angleValue = new AnimationValue();
  private final AnimationValue marginValue = new AnimationValue();

  private float angle;

  private float margin;

  public AnimationChange(Component component) {
    animator =
        new Animator(
            350,
            v -> {
              angle = angleValue.interpolate(v);
              margin = marginValue.interpolate(v);
              component.repaint();
            });
    animator.setInterpolator(CubicBezierEasing.EASE);
  }

  public void start(float angleTarget, float marginTarget, boolean animated) {
    if (angle != angleTarget || margin != marginTarget) {
      angleValue.set(angle, angleTarget);
      marginValue.set(margin, marginTarget);
      if (animator.isRunning()) {
        animator.stop();
      }
      if (animated) {
        animator.start();
      } else {
        angle = angleValue.interpolate(1f);
        margin = marginValue.interpolate(1f);
      }
    }
  }

  public float getAngle() {
    return angle;
  }

  public float getMargin() {
    return margin;
  }

  public void set(float angle, float margin) {
    if (animator.isRunning()) {
      animator.stop();
    }
    this.angle = angle;
    this.margin = margin;
  }

  public boolean isRunning() {
    return animator.isRunning();
  }

  private static class AnimationValue {

    private float from;
    private float target;

    public void set(float from, float target) {
      this.from = from;
      this.target = target;
    }

    private float interpolate(float fraction) {
      return from + ((target - from) * fraction);
    }
  }
}
