/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.acquire.explorer;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionListener;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.net.Status;
import org.weasis.acquire.explorer.core.bean.DefaultTaggable;
import org.weasis.core.api.gui.util.GuiExecutor;
import org.weasis.core.api.gui.util.GuiUtils;
import org.weasis.core.api.media.data.TagW;
import org.weasis.core.api.util.FontItem;
import org.weasis.core.ui.util.SimpleTableModel;
import org.weasis.core.ui.util.TableColumnAdjuster;
import org.weasis.dicom.codec.TagD;
import org.weasis.dicom.op.CFind;
import org.weasis.dicom.param.AdvancedParams;
import org.weasis.dicom.param.ConnectOptions;
import org.weasis.dicom.param.DicomNode;
import org.weasis.dicom.param.DicomParam;
import org.weasis.dicom.param.DicomState;
import org.weasis.dicom.tool.ModalityWorklist;

public class WorklistDialog extends JDialog {

  private JLabel selection;

  private final DicomNode calling;
  private final DicomNode called;

  private JScrollPane tableContainer;

  private JTable jtable;
  private ListSelectionListener selectionListener;
  private Attributes selectedItem;

  public WorklistDialog(Window parent, String title, DicomNode calling, DicomNode called) {
    super(parent, title, ModalityType.APPLICATION_MODAL);
    initComponents();
    this.calling = Objects.requireNonNull(calling);
    this.called = Objects.requireNonNull(called);
    fillTable();
    pack();
  }

  private void initComponents() {
    final JPanel rootPane = new JPanel();
    rootPane.setBorder(GuiUtils.getEmptyBorder(10, 15, 10, 15));
    this.setContentPane(rootPane);

    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    rootPane.setLayout(new BorderLayout(0, 0));

    jtable = new JTable();
    jtable.setFont(FontItem.SMALL.getFont());
    jtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    jtable.setRowSelectionAllowed(true);
    jtable.setShowHorizontalLines(true);
    jtable.setShowVerticalLines(true);
    jtable.getTableHeader().setReorderingAllowed(false);

    tableContainer = new JScrollPane();
    tableContainer.setPreferredSize(GuiUtils.getDimension(920, 400));

    this.getContentPane().add(tableContainer, BorderLayout.CENTER);

    JPanel footPanel = new JPanel();
    FlowLayout flowLayout = (FlowLayout) footPanel.getLayout();
    flowLayout.setVgap(15);
    flowLayout.setAlignment(FlowLayout.RIGHT);
    flowLayout.setHgap(20);
    getContentPane().add(footPanel, BorderLayout.SOUTH);
    selection = new JLabel();
    footPanel.add(selection);
    JButton okButton = new JButton();
    footPanel.add(okButton);

    okButton.setText(Messages.getString("WorklistDialog.apply"));
    okButton.addActionListener(e -> okButtonActionPerformed());
    JButton cancelButton = new JButton();
    footPanel.add(cancelButton);

    cancelButton.setText(Messages.getString("WorklistDialog.cancel"));
    cancelButton.addActionListener(e -> dispose());
  }

  public void fillTable() {
    DicomState state = queryWorklist(calling, called);
    jtable.getSelectionModel().removeListSelectionListener(selectionListener);
    List<Attributes> items = state.getDicomRSP();
    if (items != null && !items.isEmpty()) {
      DicomParam[] cols = {
        CFind.PatientName,
        CFind.PatientID,
        CFind.PatientBirthDate,
        CFind.PatientSex,
        CFind.AccessionNumber,
        ModalityWorklist.ScheduledProcedureStepDescription,
        ModalityWorklist.Modality,
        ModalityWorklist.ScheduledStationName
      };

      TagW[] tags = TagD.getTagFromIDs(Arrays.stream(cols).mapToInt(DicomParam::getTag).toArray());

      Object[][] labels = new Object[items.size()][];
      for (int i = 0; i < labels.length; i++) {
        Attributes m = items.get(i);
        Object[] row = new Object[tags.length];

        for (int j = 0; j < tags.length; j++) {
          int[] pSeq = cols[j].getParentSeqTags();
          if (pSeq == null || pSeq.length == 0) {
            row[j] = tags[j].getFormattedTagValue(tags[j].getValue(m), null);
          } else {
            Attributes parent = m;
            for (int value : pSeq) {
              Attributes p = parent.getNestedDataset(value);
              if (p == null) {
                break;
              }
              parent = p;
            }
            row[j] = tags[j].getFormattedTagValue(tags[j].getValue(parent), null);
          }
        }
        labels[i] = row;
      }
      jtable.setModel(
          new SimpleTableModel(
              Arrays.stream(tags).map(TagW::getDisplayedName).toArray(String[]::new), labels));
      TableColumnAdjuster.pack(jtable);
      selectionListener =
          event -> {
            int row = jtable.getSelectedRow();
            if (row < 0) {
              selectedItem = null;
              selection.setText("");
            } else {
              selectedItem = items.get(row);
              TagW name = TagD.get(Tag.PatientName);
              StringBuilder buf =
                  new StringBuilder(name.getFormattedTagValue(name.getValue(selectedItem), null));
              buf.append(" ");
              TagW date = TagD.get(Tag.PatientBirthDate);
              buf.append(date.getFormattedTagValue(date.getValue(selectedItem), null));
              selection.setText(buf.toString());
            }
          };
      jtable.getSelectionModel().addListSelectionListener(selectionListener);
    } else {
      if (state.getStatus() != Status.Success) {
        GuiExecutor.instance()
            .execute(
                () ->
                    JOptionPane.showMessageDialog(
                        this, state.getMessage(), null, JOptionPane.ERROR_MESSAGE));
        dispose();
        throw new IllegalStateException(state.getMessage());
      }
      jtable.setModel(new SimpleTableModel(new String[] {}, new Object[][] {}));
      tableContainer.setPreferredSize(GuiUtils.getDimension(450, 50));
    }
    tableContainer.setViewportView(jtable);
  }

  private boolean applySelection() {
    if (selectedItem != null) {
      DefaultTaggable taggable = new DefaultTaggable();

      TagW[] addTags =
          TagD.getTagFromIDs(
              Tag.AccessionNumber,
              Tag.IssuerOfAccessionNumberSequence,
              Tag.ReferringPhysicianName,
              Tag.PatientName,
              Tag.PatientID,
              Tag.IssuerOfPatientID,
              Tag.PatientBirthDate,
              Tag.PatientSex,
              Tag.PatientWeight,
              Tag.MedicalAlerts,
              Tag.Allergies,
              Tag.PregnancyStatus,
              Tag.StudyInstanceUID,
              Tag.RequestingPhysician,
              Tag.RequestingService,
              Tag.RequestedProcedureDescription,
              Tag.RequestedProcedureCodeSequence,
              Tag.AdmissionID,
              Tag.IssuerOfAdmissionIDSequence,
              Tag.SpecialNeeds,
              Tag.CurrentPatientLocation,
              Tag.PatientState);
      for (TagW t : addTags) {
        t.readValue(selectedItem, taggable);
      }

      Attributes seq = selectedItem.getNestedDataset(Tag.ScheduledProcedureStepSequence);
      taggable.setTagNoNull(
          TagD.get(Tag.StudyDescription),
          TagD.get(Tag.ScheduledProcedureStepDescription).getValue(seq));
      TagW tModality = TagD.get(Tag.Modality);
      taggable.setTagNoNull(tModality, tModality.getValue(seq));
      taggable.setTagNoNull(
          TagD.get(Tag.StationName), TagD.get(Tag.ScheduledStationName).getValue(seq));

      AcquireManager.getInstance().applyToGlobal(taggable);
      selectedItem = null;
      selection.setText("");
      jtable.getSelectionModel().clearSelection();
      return true;
    }
    return false;
  }

  private void okButtonActionPerformed() {
    if (applySelection()) {
      dispose();
    }
  }

  private static DicomState queryWorklist(DicomNode calling, DicomNode called) {
    DicomParam stationAet = new DicomParam(Tag.ScheduledStationAETitle, calling.getAet());

    DicomParam[] keys = {
      CFind.AccessionNumber,
      CFind.IssuerOfAccessionNumberSequence,
      CFind.ReferringPhysicianName,
      CFind.PatientName,
      CFind.PatientID,
      CFind.IssuerOfPatientID,
      CFind.PatientBirthDate,
      CFind.PatientSex,
      ModalityWorklist.PatientWeight,
      ModalityWorklist.MedicalAlerts,
      ModalityWorklist.Allergies,
      ModalityWorklist.PregnancyStatus,
      CFind.StudyInstanceUID,
      ModalityWorklist.RequestingPhysician,
      ModalityWorklist.RequestingService,
      ModalityWorklist.RequestedProcedureDescription,
      ModalityWorklist.RequestedProcedureCodeSequence,
      ModalityWorklist.AdmissionID,
      ModalityWorklist.IssuerOfAdmissionIDSequence,
      ModalityWorklist.SpecialNeeds,
      ModalityWorklist.CurrentPatientLocation,
      ModalityWorklist.PatientState,
      ModalityWorklist.RequestedProcedureID,
      ModalityWorklist.RequestedProcedurePriority,
      ModalityWorklist.PatientTransportArrangements,
      ModalityWorklist.PlacerOrderNumberImagingServiceRequest,
      ModalityWorklist.FillerOrderNumberImagingServiceRequest,
      ModalityWorklist.ConfidentialityConstraintOnPatientDataDescription,
      // Scheduled Procedure Step Sequence
      ModalityWorklist.Modality,
      ModalityWorklist.RequestedContrastAgent,
      stationAet,
      ModalityWorklist.ScheduledProcedureStepStartDate,
      ModalityWorklist.ScheduledProcedureStepStartTime,
      ModalityWorklist.ScheduledPerformingPhysicianName,
      ModalityWorklist.ScheduledProcedureStepDescription,
      ModalityWorklist.ScheduledProcedureStepID,
      ModalityWorklist.ScheduledStationName,
      ModalityWorklist.ScheduledProcedureStepLocation,
      ModalityWorklist.PreMedication,
      ModalityWorklist.ScheduledProcedureStepStatus,
      ModalityWorklist.ScheduledProtocolCodeSequence
    };

    AdvancedParams params = new AdvancedParams();
    ConnectOptions connectOptions = new ConnectOptions();
    connectOptions.setConnectTimeout(3000);
    connectOptions.setAcceptTimeout(5000);
    params.setConnectOptions(connectOptions);

    return ModalityWorklist.process(params, calling, called, 0, keys);
  }
}
