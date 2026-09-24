/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.api.image.measure;

import java.util.Objects;

/**
 * Adapter for converting between pixel and calibrated measurements. Handles coordinate
 * transformations, offsets, and unit calibration.
 *
 * @param calibrationRatio the ratio for converting pixels to calibrated units (must be positive)
 * @param offsetX the horizontal offset in pixels
 * @param offsetY the vertical offset in pixels
 * @param upYAxis true if Y-axis increases upward, false if downward
 * @param imageHeight the height of the image in pixels
 * @param unit the measurement unit (defaults to "px" if null)
 */
public record MeasurementsAdapter(
    double calibrationRatio,
    int offsetX,
    int offsetY,
    boolean upYAxis,
    int imageHeight,
    String unit) {
  private static final double DEFAULT_CALIBRATION_RATIO = 1.0;

  private static final String DEFAULT_UNIT = "px"; // NON-NLS

  public MeasurementsAdapter {
    if (Double.isNaN(calibrationRatio)
        || Double.isInfinite(calibrationRatio)
        || calibrationRatio <= 0) {
      throw new IllegalArgumentException("Calibration ratio must be a positive finite number");
    }

    imageHeight = Math.max(0, imageHeight - 1);
    unit = Objects.requireNonNullElse(unit, DEFAULT_UNIT);
  }

  /**
   * Gets the uncalibrated X value with offset applied.
   *
   * @param xVal the X value to transform
   * @return the transformed X value in pixels
   */
  public double getXUncalibratedValue(double xVal) {
    return xVal + offsetX;
  }

  /**
   * Gets the uncalibrated Y value with axis transformation and offset applied.
   *
   * @param yVal the Y value to transform
   * @return the transformed Y value in pixels
   */
  public double getYUncalibratedValue(double yVal) {
    return transformY(yVal) + offsetY;
  }

  /**
   * Gets the calibrated X value with offset and calibration ratio applied.
   *
   * @param xVal the X value to transform
   * @return the calibrated X value in measurement units
   */
  public double getXCalibratedValue(double xVal) {
    return calibrationRatio * getXUncalibratedValue(xVal);
  }

  /**
   * Gets the calibrated Y value with axis transformation, offset, and calibration ratio applied.
   *
   * @param yVal the Y value to transform
   * @return the calibrated Y value in measurement units
   */
  public double getYCalibratedValue(double yVal) {
    return calibrationRatio * getYUncalibratedValue(yVal);
  }

  /**
   * Checks if calibration is applied.
   *
   * @return true if calibration ratio differs from 1.0, false otherwise
   */
  public boolean isCalibrated() {
    return Double.compare(calibrationRatio, DEFAULT_CALIBRATION_RATIO) != 0;
  }

  private double transformY(double yVal) {
    return upYAxis ? imageHeight - yVal : yVal;
  }

  @Override
  public String toString() {
    return "MeasurementsAdapter{ratio=%.3f, unit='%s', offsetX=%d, offsetY=%d, upYAxis=%s, imageHeight=%d}"
        .formatted(calibrationRatio, unit, offsetX, offsetY, upYAxis, imageHeight); // NON-NLS
  }
}
