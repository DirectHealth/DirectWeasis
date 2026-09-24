/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.api.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.gui.util.AppProperties;
import org.weasis.core.util.MathUtil;

public class BundlePreferences {
  private static final Logger LOGGER = LoggerFactory.getLogger(BundlePreferences.class);

  private BundlePreferences() {}

  /**
   * Gets a path to the data folder for the given bundle context.
   *
   * @param context the bundle context
   * @return Path to the data folder for the bundle, or the default data folder if context is null
   */
  public static Path getDataFolder(BundleContext context) {
    if (context != null) {
      Path dataFolder =
          AppProperties.WEASIS_PATH.resolve("data").resolve(context.getBundle().getSymbolicName());
      try {
        Files.createDirectories(dataFolder);
      } catch (IOException e) {
        // Log error but continue, return the path anyway
        LOGGER.warn("Failed to create data folder: {}", dataFolder, e);
      }
      return dataFolder;
    }
    return AppProperties.WEASIS_PATH.resolve("data");
  }

  /**
   * Gets a file path within the data folder for the given bundle context.
   *
   * @param context the bundle context
   * @param filename the filename to append to the data folder path
   * @return Path to the file within the data folder
   */
  public static Path getFileInDataFolder(BundleContext context, String filename) {
    Path dataFolder = getDataFolder(context);
    return dataFolder.resolve(filename);
  }

  public static Preferences getDefaultPreferences(BundleContext context) {
    return getUserPreferences(context, AppProperties.WEASIS_USER);
  }

  public static Preferences getUserPreferences(BundleContext context, String name) {
    if (context != null) {
      String user = name == null ? AppProperties.WEASIS_USER : name;
      PreferencesService prefService =
          BundlePreferences.getService(context, PreferencesService.class);
      if (prefService != null) {
        return prefService.getUserPreferences(user);
      }
    }
    return null;
  }

  public static <S> S getService(BundleContext context, Class<S> clazz) {
    if (clazz != null) {
      try {
        ServiceReference<S> serviceRef = context.getServiceReference(clazz);
        if (serviceRef != null) {
          return context.getService(serviceRef);
        }
      } catch (Exception e) {
        LOGGER.error("Cannot get OSGI service from {}", clazz, e);
      }
    }
    return null;
  }

  public static void putStringPreferences(Preferences pref, String key, String value) {
    // Does not support null key or value
    if (pref != null && key != null && value != null) {
      String val2 = pref.get(key, null);
      // Update only if the value is different to avoid setting the changeSet to true
      if (!value.equals(val2)) {
        pref.put(key, value);
      }
    }
  }

  public static void putBooleanPreferences(Preferences pref, String key, boolean value) {
    // Does not support null key
    if (pref != null && key != null) {
      Boolean result = null;
      final String s = pref.get(key, null);
      if (s != null) {
        result = Boolean.valueOf(s);
      }
      // Update only if the value is different to avoid setting the changeSet to true
      if (result == null || result != value) {
        pref.putBoolean(key, value);
      }
    }
  }

  public static void putByteArrayPreferences(Preferences pref, String key, byte[] value) {
    // Does not support null key or value
    if (pref != null && key != null && value != null) {
      byte[] val2 = pref.getByteArray(key, null);
      // Update only if the value is different to avoid setting the changeSet to true
      if (val2 == null || !Arrays.equals(value, val2)) {
        pref.putByteArray(key, value);
      }
    }
  }

  public static void putDoublePreferences(Preferences pref, String key, double value) {
    // Does not support null key
    if (pref != null && key != null) {
      Double result = null;
      final String s = pref.get(key, null);
      if (s != null) {
        try {
          result = Double.parseDouble(s);
        } catch (NumberFormatException ignore) {
          LOGGER.error("Cannot parse {} into double", s);
        }
      }
      // Update only if the value is different to avoid setting the changeSet to true
      if (result == null || !MathUtil.isEqual(result, value)) {
        pref.putDouble(key, value);
      }
    }
  }

  public static void putFloatPreferences(Preferences pref, String key, float value) {
    // Does not support null key
    if (pref != null && key != null) {
      Float result = null;
      final String s = pref.get(key, null);
      if (s != null) {
        try {
          result = Float.parseFloat(s);
        } catch (NumberFormatException ignore) {
          LOGGER.error("Cannot parse {} into float", s);
        }
      }
      // Update only if the value is different to avoid setting the changeSet to true
      if (result == null || !MathUtil.isEqual(result, value)) {
        pref.putFloat(key, value);
      }
    }
  }

  public static void putIntPreferences(Preferences pref, String key, int value) {
    // Does not support null key
    if (pref != null && key != null) {
      Integer result = null;
      final String s = pref.get(key, null);
      if (s != null) {
        try {
          result = Integer.parseInt(s);
        } catch (NumberFormatException ignore) {
          LOGGER.error("Cannot parse {} into int", s);
        }
      }
      // Update only if the value is different to avoid setting the changeSet to true
      if (result == null || result != value) {
        pref.putInt(key, value);
      }
    }
  }

  public static void putLongPreferences(Preferences pref, String key, long value) {
    // Does not support null key
    if (pref != null && key != null) {
      Long result = null;
      final String s = pref.get(key, null);
      if (s != null) {
        try {
          result = Long.parseLong(s);
        } catch (NumberFormatException ignore) {
          LOGGER.error("Cannot parse {} into long", s);
        }
      }
      // Update only if the value is different to avoid setting the changeSet to true
      if (result == null || result != value) {
        pref.putLong(key, value);
      }
    }
  }
}
