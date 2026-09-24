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

import org.weasis.core.Messages;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.image.util.WindLevelParameters;
import org.weasis.core.api.media.data.ImageElement;
import org.weasis.core.util.LangUtil;
import org.weasis.opencv.data.PlanarImage;
import org.weasis.opencv.op.lut.DefaultWlPresentation;
import org.weasis.opencv.op.lut.PresentationStateLut;
import org.weasis.opencv.op.lut.WlPresentation;

/**
 * Operation for applying window/level transformations to medical images. This operation manages
 * brightness and contrast adjustments based on window and level parameters.
 */
public class WindowOp extends AbstractOp {

  public static final String OP_NAME = Messages.getString("WindowLevelOperation.title");

  public static final String P_IMAGE_ELEMENT = "img.element";
  public static final String P_FILL_OUTSIDE_LUT = "fill.outside.lut";
  public static final String P_APPLY_WL_COLOR = "weasis.color.wl.apply";
  public static final String P_INVERSE_LEVEL = "weasis.level.inverse";

  private static final String P_PR_ELEMENT = "pr.element";

  public WindowOp() {
    setName(OP_NAME);
  }

  public WindowOp(WindowOp op) {
    super(op);
  }

  @Override
  public WindowOp copy() {
    return new WindowOp(this);
  }

  @Override
  public void handleImageOpEvent(ImageOpEvent event) {
    switch (event.eventType()) {
      case IMAGE_CHANGE -> setParam(P_IMAGE_ELEMENT, event.image());
      case RESET_DISPLAY, SERIES_CHANGE -> handleDisplayReset(event.image());
      default -> {
        /* no action */
      }
    }
  }

  @Override
  public void process() throws Exception {
    PlanarImage source = getSourceImage();
    ImageElement imageElement = getImageElement();

    PlanarImage result =
        imageElement != null ? imageElement.getRenderedImage(source, params) : source;
    params.put(Param.OUTPUT_IMG, result);
  }

  private void handleDisplayReset(ImageElement img) {
    setParam(P_IMAGE_ELEMENT, img);
    if (img == null) {
      return;
    }
    ensureImageLoaded(img);
    WlPresentation wlp = getWlPresentation();
    setParam(ActionW.WINDOW.cmd(), img.getDefaultWindow(wlp));
    setParam(ActionW.LEVEL.cmd(), img.getDefaultLevel(wlp));
    setParam(ActionW.LEVEL_MIN.cmd(), img.getMinValue(wlp));
    setParam(ActionW.LEVEL_MAX.cmd(), img.getMaxValue(wlp));
  }

  private void ensureImageLoaded(ImageElement img) {
    if (!img.isImageAvailable()) {
      img.getImage();
    }
  }

  private ImageElement getImageElement() {
    return (ImageElement) params.get(P_IMAGE_ELEMENT);
  }

  /**
   * Creates a window/level presentation with pixel padding settings.
   *
   * @return the configured WlPresentation instance
   */
  public WlPresentation getWlPresentation() {
    boolean pixelPadding = LangUtil.nullToTrue((Boolean) getParam(ActionW.IMAGE_PIX_PADDING.cmd()));
    PresentationStateLut pr = (PresentationStateLut) getParam(P_PR_ELEMENT);
    return new DefaultWlPresentation(pr, pixelPadding);
  }

  /**
   * Retrieves the current window/level parameters for the active image.
   *
   * @return WindLevelParameters instance or null if no image element is set
   */
  public WindLevelParameters getWindLevelParameters() {
    ImageElement imageElement = getImageElement();
    return imageElement != null ? new WindLevelParameters(imageElement, params) : null;
  }
}
