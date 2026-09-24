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

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

/* SimpleTransition is a class that provides simple transitions for the PanelSlider.
 *
 * @author Raven Laing
 * @see <a href="https://github.com/DJ-Raven/swing-datetime-picker">swing-datetime-picker</a>
 */

public class SimpleTransition {

  public static SliderTransition get(SliderType sliderType) {
    SliderTransition transition = null;
    if (sliderType == SliderType.BACK) {
      transition =
          new SliderTransition() {

            @Override
            public void renderImageNew(
                Graphics g, Image image, int width, int height, float animate) {
              int x = -(int) (width * (1f - animate));
              g.drawImage(image, x, 0, null);
              g.dispose();
            }

            @Override
            public void renderImageOld(
                Graphics g, Image image, int width, int height, float animate) {
              int x = (int) (width * animate);
              g.drawImage(image, x, 0, null);
              g.dispose();
            }
          };
    } else if (sliderType == SliderType.FORWARD) {
      transition =
          new SliderTransition() {

            @Override
            public void renderImageNew(
                Graphics g, Image image, int width, int height, float animate) {
              int x = (int) (width - width * animate);
              g.drawImage(image, x, 0, null);
              g.dispose();
            }

            @Override
            public void renderImageOld(
                Graphics g, Image image, int width, int height, float animate) {
              int x = -(int) (width * animate);
              g.drawImage(image, x, 0, null);
              g.dispose();
            }
          };
    } else if (sliderType == SliderType.ZOOM_OUT) {
      transition =
          new SliderTransition() {

            @Override
            public void renderImageNew(
                Graphics g, Image image, int width, int height, float animate) {
              Graphics2D g2 = (Graphics2D) g;
              g2.setComposite(AlphaComposite.SrcOver.derive(animate));
              g.drawImage(image, 0, 0, null);
              g2.dispose();
            }

            @Override
            public void renderImageOld(
                Graphics g, Image image, int width, int height, float animate) {
              Graphics2D g2 = (Graphics2D) g;
              float sx = 1 + animate;
              float sy = 1 + animate;
              int x = width / 2;
              int y = height / 2;
              g2.translate(x, y);
              g2.scale(sx, sy);
              g2.translate(-x, -y);
              g2.setComposite(AlphaComposite.SrcOver.derive(1f - animate));
              g.drawImage(image, 0, 0, null);
              g2.dispose();
            }
          };
    } else if (sliderType == SliderType.TOP_DOWN) {
      transition =
          new SliderTransition() {

            @Override
            public void renderImageNew(
                Graphics g, Image image, int width, int height, float animate) {
              Graphics2D g2 = (Graphics2D) g;
              int y = (int) (-height + height * animate);
              g.drawImage(image, 0, y, null);
              g2.dispose();
            }

            @Override
            public void renderImageOld(
                Graphics g, Image image, int width, int height, float animate) {
              Graphics2D g2 = (Graphics2D) g;
              g.drawImage(image, 0, 0, null);
              g2.dispose();
            }
          };
    } else if (sliderType == SliderType.DOWN_TOP) {
      transition =
          new SliderTransition() {

            @Override
            public void renderImageNew(
                Graphics g, Image image, int width, int height, float animate) {
              Graphics2D g2 = (Graphics2D) g;
              g.drawImage(image, 0, 0, null);
              g2.dispose();
            }

            @Override
            public void renderImageOld(
                Graphics g, Image image, int width, int height, float animate) {
              Graphics2D g2 = (Graphics2D) g;
              int y = (int) (-height * animate);
              g.drawImage(image, 0, y, null);
              g2.dispose();
            }

            @Override
            public void render(
                Graphics g, Image imageOld, Image imageNew, int width, int height, float animate) {
              renderImageNew(g.create(), imageNew, width, height, animate);
              renderImageOld(g.create(), imageOld, width, height, animate);
            }
          };
    }
    return transition;
  }

  public enum SliderType {
    DEFAULT,
    BACK,
    FORWARD,
    ZOOM_IN,
    ZOOM_OUT,
    TOP_DOWN,
    DOWN_TOP
  }
}
