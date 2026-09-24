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

/**
 * Functional interface for handling image operation events. Implementations receive notifications
 * when image operations occur.
 */
@FunctionalInterface
public interface OpEventListener {

  /**
   * Processes an image operation event.
   *
   * @param event the image operation event to handle
   */
  void handleImageOpEvent(ImageOpEvent event);
}
