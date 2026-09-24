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

import java.awt.geom.AffineTransform;
import org.weasis.core.api.image.OpManager;
import org.weasis.core.api.image.SimpleOpManager;
import org.weasis.core.api.media.data.ImageElement;
import org.weasis.opencv.data.PlanarImage;

/**
 * Layer interface for managing image display with transformations and operations.
 *
 * <p>This interface provides functionality for:
 *
 * <ul>
 *   <li>Image management (source and display images)
 *   <li>Geometric transformations
 *   <li>Display operations pipeline
 *   <li>Layer visibility control
 * </ul>
 *
 * @param <E> the image element type
 */
public interface ImageLayer<E extends ImageElement> extends MeasurableLayer {

  // === Image Management ===

  /**
   * Gets the source image element.
   *
   * @return the source image, or null if not set
   */
  E getSourceImage();

  /**
   * Gets the processed display image.
   *
   * @return the display image, or null if not available
   */
  PlanarImage getDisplayImage();

  /**
   * Sets the image with optional preprocessing operations.
   *
   * @param image the image element to set
   * @param preprocessing the preprocessing operations to apply
   */
  void setImage(E image, OpManager preprocessing);

  // ======= Transform Operations =======

  /**
   * Gets the current transform applied to the layer.
   *
   * @return the transform, or null if no transform is applied
   */
  AffineTransform getTransform();

  /**
   * Sets the transform to apply to the layer.
   *
   * @param transform the transform to set
   */
  void setTransform(AffineTransform transform);

  // ======= Display Operations =======

  /**
   * Gets the display operation manager.
   *
   * @return the display operation manager
   */
  SimpleOpManager getDisplayOpManager();

  /** Updates the display operations and refreshes the display image. */
  void updateDisplayOperations();

  /**
   * Checks if display operations are enabled.
   *
   * @return true if enabled, false otherwise
   */
  boolean isEnableDispOperations();

  /**
   * Enables or disables display operations.
   *
   * @param enabled true to enable, false to disable
   */
  void setEnableDispOperations(boolean enabled);

  // ======= Layer Properties =======

  /**
   * Sets the visibility of the layer.
   *
   * @param visible true to make visible, false to hide
   */
  void setVisible(Boolean visible);

  /**
   * Gets the visibility state of the layer.
   *
   * @return true if visible, false if hidden
   */
  Boolean getVisible();
}
