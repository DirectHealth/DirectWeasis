/*
 * Copyright (c) 2024 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.tp.raven.datetime.component.date;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Component;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.miginfocom.swing.MigLayout;
import org.weasis.core.ui.tp.raven.datetime.DatePicker;

/* PanelMonth is a class that provides a panel for selecting a month in the DatePicker.
 *
 * @author Raven Laing
 * @see <a href="https://github.com/DJ-Raven/swing-datetime-picker">swing-datetime-picker</a>
 */
public class PanelMonth extends JPanel {

  private final DatePicker datePicker;
  private final int year;
  private int selectedMonth = -1;

  public PanelMonth(DatePicker datePicker, int year) {
    this.datePicker = datePicker;
    this.year = year;
    init();
  }

  private void init() {
    putClientProperty(FlatClientProperties.STYLE, "background:null");
    setLayout(
        new MigLayout(
            "novisualpadding,wrap 3,insets 0,fillx,gap 0,al center center",
            "fill,sg main",
            "fill"));

    final int count = 12;
    for (int i = 0; i < count; i++) {
      final int month = i;
      ButtonMonthYear button = new ButtonMonthYear(datePicker, i);
      button.setText(DatePicker.getDefaultMonths()[i]);
      if (checkSelected(month + 1)) {
        button.setSelected(true);
      }
      button.addActionListener(
          e -> {
            this.selectedMonth = month;
            fireMonthChanged(new ChangeEvent(this));
          });
      add(button);
    }
  }

  public boolean checkSelected(int month) {
    DateSelectionModel dateSelectionModel = datePicker.getDateSelectionModel();
    if (dateSelectionModel.getDateSelectionMode()
        == DatePicker.DateSelectionMode.SINGLE_DATE_SELECTED) {
      return dateSelectionModel.getDate() != null
          && year == dateSelectionModel.getDate().getYear()
          && month == dateSelectionModel.getDate().getMonth();
    } else {
      return (dateSelectionModel.getDate() != null
              && year == dateSelectionModel.getDate().getYear()
              && month == dateSelectionModel.getDate().getMonth())
          || (dateSelectionModel.getToDate() != null
              && year == dateSelectionModel.getToDate().getYear()
              && month == dateSelectionModel.getToDate().getMonth());
    }
  }

  public void checkSelection() {
    for (int i = 0; i < getComponentCount(); i++) {
      Component com = getComponent(i);
      if (com instanceof ButtonMonthYear) {
        ButtonMonthYear button = (ButtonMonthYear) com;
        button.setSelected(checkSelected(button.getValue() + 1));
      }
    }
  }

  public void addChangeListener(ChangeListener listener) {
    listenerList.add(ChangeListener.class, listener);
  }

  public void removeChangeListener(ChangeListener listener) {
    listenerList.remove(ChangeListener.class, listener);
  }

  public void fireMonthChanged(ChangeEvent event) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == ChangeListener.class) {
        ((ChangeListener) listeners[i + 1]).stateChanged(event);
      }
    }
  }

  public int getSelectedMonth() {
    return selectedMonth;
  }
}
