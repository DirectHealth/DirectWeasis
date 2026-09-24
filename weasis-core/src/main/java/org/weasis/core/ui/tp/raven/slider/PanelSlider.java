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

import com.formdev.flatlaf.util.Animator;
import com.formdev.flatlaf.util.CubicBezierEasing;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.VolatileImage;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;

/**
 * PanelSlider is a JLayeredPane that allows you to slide between two components with a transition.
 *
 * @author Raven Laing
 * @see <a href="https://github.com/DJ-Raven/swing-datetime-picker">swing-datetime-picker</a>
 */
public class PanelSlider extends JLayeredPane {

  public Component getSlideComponent() {
    return slideComponent;
  }

  private PanelSnapshot panelSnapshot;
  private Component slideComponent;

  public PanelSlider() {
    init();
  }

  private void init() {
    panelSnapshot = new PanelSnapshot();
    setLayout(new CardLayout());
    setLayer(panelSnapshot, JLayeredPane.DRAG_LAYER);
    add(panelSnapshot);
    panelSnapshot.setVisible(false);
  }

  public void addSlide(Component component, SliderTransition transition) {
    this.slideComponent = component;
    if (getComponentCount() == 1) {
      add(component);
      repaint();
      revalidate();
      component.setVisible(true);
    } else {
      Component oldComponent = getComponent(1);
      add(component);
      if (transition != null) {
        doLayout();
        component.doLayout();
        Image oldImage = createImage(oldComponent);
        Image newImage = createImage(component);
        remove(oldComponent);
        panelSnapshot.animate(component, transition, oldImage, newImage);
      } else {
        component.setVisible(true);
        remove(oldComponent);
        revalidate();
        repaint();
      }
    }
  }

  private Image createImage(Component component) {
    VolatileImage snapshot = component.createVolatileImage(getWidth(), getHeight());
    if (snapshot != null) {
      component.paint(snapshot.getGraphics());
    }
    return snapshot;
  }

  private static class PanelSnapshot extends JComponent {

    private final Animator animator;
    private Component component;
    private float animate;

    private SliderTransition sliderTransition;
    private Image oldImage;
    private Image newImage;

    public PanelSnapshot() {
      animator =
          new Animator(
              400,
              new Animator.TimingTarget() {
                @Override
                public void timingEvent(float v) {
                  animate = v;
                  repaint();
                }

                @Override
                public void end() {
                  setVisible(false);
                  component.setVisible(true);
                  if (newImage != null) {
                    newImage.flush();
                  }
                  if (oldImage != null) {
                    oldImage.flush();
                  }
                }
              });
      animator.setInterpolator(CubicBezierEasing.EASE);
    }

    protected void animate(
        Component component, SliderTransition sliderTransition, Image oldImage, Image newImage) {
      if (animator.isRunning()) {
        animator.stop();
      }
      this.component = component;
      this.oldImage = oldImage;
      this.newImage = newImage;
      this.sliderTransition = sliderTransition;
      this.animate = 0f;
      repaint();
      setVisible(true);
      animator.start();
    }

    @Override
    public void paint(Graphics g) {
      if (sliderTransition != null) {
        int width = getWidth();
        int height = getHeight();
        sliderTransition.render(g, oldImage, newImage, width, height, animate);
      }
    }
  }
}
