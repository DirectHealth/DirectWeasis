/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.api.image.util;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.image.PseudoColorOp;
import org.weasis.core.api.image.WindowOp;
import org.weasis.core.api.media.data.ImageElement;
import org.weasis.opencv.op.lut.DefaultWlPresentation;
import org.weasis.opencv.op.lut.LutShape;
import org.weasis.opencv.op.lut.PresentationStateLut;
import org.weasis.opencv.op.lut.WlParams;

/**
 * Immutable window/level parameters for image display operations.
 *
 * <p>This class encapsulates all parameters needed for window/level adjustments in medical imaging,
 * including LUT configuration, pixel padding, and display options.
 */
public final class WindLevelParameters implements WlParams {
  private static final String PRESENTATION_STATE_KEY = "pr.element";

  private final double window;
  private final double level;
  private final double levelMin;
  private final double levelMax;

  private final boolean pixelPadding;
  private final boolean inverseLut;
  private final boolean fillOutsideLutRange;
  private final boolean allowWinLevelOnColorImage;

  private final LutShape lutShape;
  private final PresentationStateLut presentationStateLut;

  /**
   * Creates WindLevelParameters from image element and parameter map.
   *
   * @param imageElement the image element to extract defaults from
   * @param params the parameter map, can be null
   * @throws NullPointerException if imageElement is null
   */
  public WindLevelParameters(ImageElement imageElement, Map<String, Object> params) {
    Objects.requireNonNull(imageElement, "Image element cannot be null");

    var extractor = new ParameterExtractor(params);
    var presentation = createPresentation(extractor);

    this.presentationStateLut = extractor.getPresentationState();
    this.pixelPadding = extractor.getPixelPadding();
    this.inverseLut = extractor.getInverseLut();
    this.fillOutsideLutRange = extractor.getFillOutsideLutRange();
    this.allowWinLevelOnColorImage = extractor.getAllowWinLevelOnColorImage();

    this.window =
        extractor.getWindow().orElseGet(() -> imageElement.getDefaultWindow(presentation));
    this.level = extractor.getLevel().orElseGet(() -> imageElement.getDefaultLevel(presentation));
    this.lutShape =
        extractor.getLutShape().orElseGet(() -> imageElement.getDefaultShape(presentation));

    var levelRange = calculateLevelRange(extractor, imageElement, presentation);
    this.levelMin = levelRange.min();
    this.levelMax = levelRange.max();
  }

  private WindLevelParameters(Builder builder) {
    this.window = builder.window;
    this.level = builder.level;
    this.levelMin = builder.levelMin;
    this.levelMax = builder.levelMax;
    this.pixelPadding = builder.pixelPadding;
    this.inverseLut = builder.inverseLut;
    this.fillOutsideLutRange = builder.fillOutsideLutRange;
    this.allowWinLevelOnColorImage = builder.allowWinLevelOnColorImage;
    this.lutShape = builder.lutShape;
    this.presentationStateLut = builder.presentationStateLut;
  }

  @Override
  public double getWindow() {
    return window;
  }

  @Override
  public double getLevel() {
    return level;
  }

  @Override
  public double getLevelMin() {
    return levelMin;
  }

  @Override
  public double getLevelMax() {
    return levelMax;
  }

  @Override
  public boolean isPixelPadding() {
    return pixelPadding;
  }

  @Override
  public boolean isInverseLut() {
    return inverseLut;
  }

  @Override
  public boolean isFillOutsideLutRange() {
    return fillOutsideLutRange;
  }

  @Override
  public boolean isAllowWinLevelOnColorImage() {
    return allowWinLevelOnColorImage;
  }

  @Override
  public LutShape getLutShape() {
    return lutShape;
  }

  @Override
  public PresentationStateLut getPresentationState() {
    return presentationStateLut;
  }

  /**
   * Gets the window range (level ± window/2).
   *
   * @return the window range as [min, max]
   */
  public double[] getWindowRange() {
    double halfWindow = window / 2.0;
    return new double[] {level - halfWindow, level + halfWindow};
  }

  /**
   * Gets the level range.
   *
   * @return the level range as [min, max]
   */
  public double[] getLevelRange() {
    return new double[] {levelMin, levelMax};
  }

  /**
   * Validates these parameters.
   *
   * @return true if parameters are valid, false otherwise
   */
  public boolean isValid() {
    return Double.isFinite(window)
        && window > 0
        && Double.isFinite(level)
        && Double.isFinite(levelMin)
        && Double.isFinite(levelMax)
        && levelMin <= levelMax
        && lutShape != null;
  }

  /**
   * Creates a copy with modified window/level values.
   *
   * @param newWindow the new window value
   * @param newLevel the new level value
   * @return new WindLevelParameters instance
   */
  public WindLevelParameters withWindowLevel(double newWindow, double newLevel) {
    return builder().from(this).window(newWindow).level(newLevel).build();
  }

  /**
   * Creates a copy with modified LUT shape.
   *
   * @param newLutShape the new LUT shape
   * @return new WindLevelParameters instance
   */
  public WindLevelParameters withLutShape(LutShape newLutShape) {
    return builder().from(this).lutShape(newLutShape).build();
  }

  @Override
  public String toString() {
    return "WindLevelParameters[window=%.2f, level=%.2f, range=[%.2f-%.2f], pixelPadding=%s, inverseLut=%s, lutShape=%s]"
        .formatted(window, level, levelMin, levelMax, pixelPadding, inverseLut, lutShape);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof WindLevelParameters that
        && Double.compare(that.window, window) == 0
        && Double.compare(that.level, level) == 0
        && Double.compare(that.levelMin, levelMin) == 0
        && Double.compare(that.levelMax, levelMax) == 0
        && pixelPadding == that.pixelPadding
        && inverseLut == that.inverseLut
        && fillOutsideLutRange == that.fillOutsideLutRange
        && allowWinLevelOnColorImage == that.allowWinLevelOnColorImage
        && Objects.equals(lutShape, that.lutShape)
        && Objects.equals(presentationStateLut, that.presentationStateLut);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        window,
        level,
        levelMin,
        levelMax,
        pixelPadding,
        inverseLut,
        fillOutsideLutRange,
        allowWinLevelOnColorImage,
        lutShape,
        presentationStateLut);
  }

  private DefaultWlPresentation createPresentation(ParameterExtractor extractor) {
    return new DefaultWlPresentation(extractor.getPresentationState(), extractor.getPixelPadding());
  }

  private LevelRange calculateLevelRange(
      ParameterExtractor extractor, ImageElement imageElement, DefaultWlPresentation presentation) {
    double imageMin = imageElement.getMinValue(presentation);
    double imageMax = imageElement.getMaxValue(presentation);

    return extractor
        .getLevelMin()
        .flatMap(
            min ->
                extractor
                    .getLevelMax()
                    .map(max -> new LevelRange(Math.min(min, imageMin), Math.max(max, imageMax))))
        .orElseGet(
            () -> {
              double halfWindow = window / 2.0;
              return new LevelRange(
                  Math.min(level - halfWindow, imageMin), Math.max(level + halfWindow, imageMax));
            });
  }

  private record ParameterExtractor(Map<String, Object> params) {

    private <T> Optional<T> getParam(String key, Class<T> type) {
      return Optional.ofNullable(params)
          .map(p -> p.get(key))
          .filter(type::isInstance)
          .map(type::cast);
    }

    Optional<Double> getWindow() {
      return getParam(ActionW.WINDOW.cmd(), Double.class);
    }

    Optional<Double> getLevel() {
      return getParam(ActionW.LEVEL.cmd(), Double.class);
    }

    Optional<Double> getLevelMin() {
      return getParam(ActionW.LEVEL_MIN.cmd(), Double.class);
    }

    Optional<Double> getLevelMax() {
      return getParam(ActionW.LEVEL_MAX.cmd(), Double.class);
    }

    Optional<LutShape> getLutShape() {
      return getParam(ActionW.LUT_SHAPE.cmd(), LutShape.class);
    }

    boolean getPixelPadding() {
      return getParam(ActionW.IMAGE_PIX_PADDING.cmd(), Boolean.class).orElse(true);
    }

    boolean getInverseLut() {
      return getParam(PseudoColorOp.P_LUT_INVERSE, Boolean.class).orElse(false);
    }

    boolean getFillOutsideLutRange() {
      return getParam(WindowOp.P_FILL_OUTSIDE_LUT, Boolean.class).orElse(false);
    }

    boolean getAllowWinLevelOnColorImage() {
      return getParam(WindowOp.P_APPLY_WL_COLOR, Boolean.class).orElse(false);
    }

    PresentationStateLut getPresentationState() {
      return getParam(PRESENTATION_STATE_KEY, PresentationStateLut.class).orElse(null);
    }
  }

  private record LevelRange(double min, double max) {
    LevelRange {
      if (!Double.isFinite(min) || !Double.isFinite(max) || min > max) {
        throw new IllegalArgumentException("Invalid level range: [%f, %f]".formatted(min, max));
      }
    }
  }

  public static class Builder {
    private double window = 1.0;
    private double level = 0.0;
    private double levelMin = 0.0;
    private double levelMax = 1.0;
    private boolean pixelPadding = true;
    private boolean inverseLut = false;
    private boolean fillOutsideLutRange = false;
    private boolean allowWinLevelOnColorImage = false;
    private LutShape lutShape = LutShape.LINEAR;
    private PresentationStateLut presentationStateLut = null;

    public Builder window(double window) {
      this.window = window;
      return this;
    }

    public Builder level(double level) {
      this.level = level;
      return this;
    }

    public Builder levelMin(double levelMin) {
      this.levelMin = levelMin;
      return this;
    }

    public Builder levelMax(double levelMax) {
      this.levelMax = levelMax;
      return this;
    }

    public Builder pixelPadding(boolean pixelPadding) {
      this.pixelPadding = pixelPadding;
      return this;
    }

    public Builder inverseLut(boolean inverseLut) {
      this.inverseLut = inverseLut;
      return this;
    }

    public Builder fillOutsideLutRange(boolean fillOutsideLutRange) {
      this.fillOutsideLutRange = fillOutsideLutRange;
      return this;
    }

    public Builder allowWinLevelOnColorImage(boolean allowWinLevelOnColorImage) {
      this.allowWinLevelOnColorImage = allowWinLevelOnColorImage;
      return this;
    }

    public Builder lutShape(LutShape lutShape) {
      this.lutShape = lutShape;
      return this;
    }

    public Builder presentationStateLut(PresentationStateLut presentationStateLut) {
      this.presentationStateLut = presentationStateLut;
      return this;
    }

    public Builder from(WindLevelParameters params) {
      this.window = params.window;
      this.level = params.level;
      this.levelMin = params.levelMin;
      this.levelMax = params.levelMax;
      this.pixelPadding = params.pixelPadding;
      this.inverseLut = params.inverseLut;
      this.fillOutsideLutRange = params.fillOutsideLutRange;
      this.allowWinLevelOnColorImage = params.allowWinLevelOnColorImage;
      this.lutShape = params.lutShape;
      this.presentationStateLut = params.presentationStateLut;
      return this;
    }

    public WindLevelParameters build() {
      return new WindLevelParameters(this);
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
