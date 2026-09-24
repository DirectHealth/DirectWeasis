/*
 * Copyright (c) 2024 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.tp.raven.slider;

import java.awt.Graphics;
import java.awt.Image;

/* SliderTransition is an abstract class that provides methods for rendering images with transitions.
 *
 * @author Raven Laing
 * @see <a href="https://github.com/DJ-Raven/swing-datetime-picker">swing-datetime-picker</a>
 */
public abstract class SliderTransition {

  public abstract void renderImageOld(
      Graphics g, Image image, int width, int height, float animate);

  public abstract void renderImageNew(
      Graphics g, Image image, int width, int height, float animate);

  public void render(
      Graphics g, Image imageOld, Image imageNew, int width, int height, float animate) {
    renderImageOld(g.create(), imageOld, width, height, animate);
    renderImageNew(g.create(), imageNew, width, height, animate);
  }
}
