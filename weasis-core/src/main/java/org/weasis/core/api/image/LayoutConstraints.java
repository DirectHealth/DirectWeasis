/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.api.image;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Objects;
import java.util.Optional;
import org.weasis.core.api.util.Copyable;

/**
 * Enhanced GridBagConstraints with additional layout metadata. This class extends
 * GridBagConstraints to include type information, layout ID, and color properties for better layout
 * management and visualization.
 */
public final class LayoutConstraints extends GridBagConstraints
    implements Comparable<LayoutConstraints>, Copyable<LayoutConstraints> {

  public static final int DEFAULT_SPACE = 3;

  private final String type;
  private final int layoutID;
  private Color color;

  /**
   * Creates a new LayoutConstraints with specified parameters.
   *
   * @param type the component type identifier
   * @param layoutID unique identifier for this layout constraint
   * @param gridX the grid x position
   * @param gridY the grid y position
   * @param gridWidth the grid width span
   * @param gridHeight the grid height span
   * @param weightX the horizontal weight
   * @param weightY the vertical weight
   * @param anchor the anchor position
   * @param fill the fill behavior
   */
  public LayoutConstraints(
      String type,
      int layoutID,
      int gridX,
      int gridY,
      int gridWidth,
      int gridHeight,
      double weightX,
      double weightY,
      int anchor,
      int fill) {
    super(
        gridX,
        gridY,
        gridWidth,
        gridHeight,
        weightX,
        weightY,
        anchor,
        fill,
        calculateInsets(gridX, gridY),
        0,
        0);
    this.type = Objects.requireNonNull(type, "Type cannot be null");
    this.layoutID = layoutID;
  }

  /**
   * Copy constructor that creates a deep copy of the given LayoutConstraints.
   *
   * @param source the LayoutConstraints to copy
   */
  public LayoutConstraints(LayoutConstraints source) {
    super(
        source.gridx,
        source.gridy,
        source.gridwidth,
        source.gridheight,
        source.weightx,
        source.weighty,
        source.anchor,
        source.fill,
        new Insets(
            source.insets.top, source.insets.left, source.insets.bottom, source.insets.right),
        source.ipadx,
        source.ipady);

    this.type = source.type;
    this.layoutID = source.layoutID;
    this.color = source.color;
  }

  /** Builder pattern for creating LayoutConstraints. */
  public static final class Builder {
    private final String type;
    private final int layoutID;
    private int gridX;
    private int gridY;
    private int gridWidth = 1;
    private int gridHeight = 1;
    private double weightX;
    private double weightY;
    private int anchor = CENTER;
    private int fill = NONE;
    private Color color;

    private Builder(String type, int layoutID) {
      this.type = Objects.requireNonNull(type, "Type cannot be null");
      this.layoutID = layoutID;
    }

    public Builder gridPosition(int x, int y) {
      this.gridX = x;
      this.gridY = y;
      return this;
    }

    public Builder gridSize(int width, int height) {
      this.gridWidth = Math.max(1, width);
      this.gridHeight = Math.max(1, height);
      return this;
    }

    public Builder weights(double weightX, double weightY) {
      this.weightX = Math.max(0.0, weightX);
      this.weightY = Math.max(0.0, weightY);
      return this;
    }

    public Builder anchor(int anchor) {
      this.anchor = anchor;
      return this;
    }

    public Builder fill(int fill) {
      this.fill = fill;
      return this;
    }

    public Builder color(Color color) {
      this.color = color;
      return this;
    }

    public LayoutConstraints build() {
      var constraints =
          new LayoutConstraints(
              type, layoutID, gridX, gridY, gridWidth, gridHeight, weightX, weightY, anchor, fill);
      constraints.color = color;
      return constraints;
    }
  }

  private static Insets calculateInsets(int gridX, int gridY) {
    return new Insets(gridY == 0 ? 0 : DEFAULT_SPACE, gridX == 0 ? 0 : DEFAULT_SPACE, 0, 0);
  }

  public String type() {
    return type;
  }

  public int layoutID() {
    return layoutID;
  }

  public Optional<Color> colorOptional() {
    return Optional.ofNullable(color);
  }

  public Color color() {
    return color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  public boolean hasColor() {
    return color != null;
  }

  public boolean isSpanning() {
    return gridwidth > 1 || gridheight > 1;
  }

  public boolean hasWeight() {
    return weightx > 0.0 || weighty > 0.0;
  }

  @Override
  public int compareTo(LayoutConstraints other) {
    if (other == null) {
      return 1;
    }

    // Primary sort by layout ID
    int result = Integer.compare(this.layoutID, other.layoutID);
    if (result != 0) {
      return result;
    }

    // Secondary sort by grid position (y first, then x)
    result = Integer.compare(this.gridy, other.gridy);
    if (result != 0) {
      return result;
    }

    return Integer.compare(this.gridx, other.gridx);
  }

  @Override
  public LayoutConstraints copy() {
    return new LayoutConstraints(this);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof LayoutConstraints that
        && layoutID == that.layoutID
        && gridx == that.gridx
        && gridy == that.gridy
        && gridwidth == that.gridwidth
        && gridheight == that.gridheight
        && Double.compare(weightx, that.weightx) == 0
        && Double.compare(weighty, that.weighty) == 0
        && anchor == that.anchor
        && fill == that.fill
        && Objects.equals(type, that.type)
        && Objects.equals(color, that.color)
        && Objects.equals(insets, that.insets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        type,
        layoutID,
        gridx,
        gridy,
        gridwidth,
        gridheight,
        weightx,
        weighty,
        anchor,
        fill,
        color,
        insets);
  }

  @Override
  public String toString() {
    return "LayoutConstraints[type=%s, id=%d, grid=(%d,%d), size=%dx%d, weight=(%.2f,%.2f), anchor=%d, fill=%d%s]"
        .formatted(
            type,
            layoutID,
            gridx,
            gridy,
            gridwidth,
            gridheight,
            weightx,
            weighty,
            anchor,
            fill,
            hasColor() ? ", color=" + color : "");
  }

  public static Builder builder(String type, int layoutID) {
    return new Builder(type, layoutID);
  }
}
