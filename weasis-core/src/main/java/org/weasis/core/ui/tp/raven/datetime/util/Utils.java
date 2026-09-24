/*
 * Copyright (c) 2024 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.tp.raven.datetime.util;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.ColorFunctions;
import com.formdev.flatlaf.util.UIScale;
import java.awt.*;
import javax.swing.*;

public class Utils {

  public static Point adjustPopupLocation(JPopupMenu popupMenu, Component component, Point space) {
    Window window = SwingUtilities.getWindowAncestor(component);
    if (window == null) {
      return new Point(0, component.getHeight());
    }
    Insets frameInsets = window.getInsets();
    frameInsets = FlatUIUtils.addInsets(frameInsets, UIScale.scale(new Insets(5, 5, 5, 5)));
    Dimension popupSize = popupMenu.getComponent().getPreferredSize();
    int frameWidth = window.getWidth() - (frameInsets.left + frameInsets.right);
    int frameHeight = window.getHeight() - (frameInsets.top + frameInsets.bottom);
    Point locationOnFrame =
        SwingUtilities.convertPoint(component, new Point(0, component.getHeight()), window);
    int bottomSpace = frameHeight - locationOnFrame.y - popupSize.height - space.y;
    int rightSpace = frameWidth - locationOnFrame.x - popupSize.width - space.x;
    int x =
        frameInsets.left + rightSpace > 0
            ? locationOnFrame.x + space.x
            : Math.max(
                locationOnFrame.x + component.getWidth() - popupSize.width - space.x,
                frameInsets.left);
    int y =
        frameInsets.top + bottomSpace > 0
            ? locationOnFrame.y + space.y
            : Math.max(
                locationOnFrame.y - component.getHeight() - popupSize.height - space.y,
                frameInsets.top);
    return SwingUtilities.convertPoint(window, new Point(x, y), component);
  }

  public static Color getColor(Color color, boolean press, boolean hover) {
    if (press) {
      return FlatLaf.isLafDark()
          ? ColorFunctions.lighten(color, 0.1f)
          : ColorFunctions.darken(color, 0.1f);
    } else if (hover) {
      return FlatLaf.isLafDark()
          ? ColorFunctions.lighten(color, 0.03f)
          : ColorFunctions.darken(color, 0.03f);
    }
    return color;
  }
}
