package org.weasis.core.ui.util;

/*
 * @copyright Copyright (c) 2009 Animati Sistemas de Informática Ltda. (http://www.animati.com.br)
 */

/*
 * PrintDialog.java
 *
 * Created on 17/11/2011, 09:54:28
 */
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.weasis.core.api.media.data.ImageElement;
import org.weasis.core.ui.editor.image.ImageViewerEventManager;
import org.weasis.core.ui.editor.image.ImageViewerPlugin;
import org.weasis.core.ui.util.PrintOptions.SCALE;

/**
 * 
 * @author Marcelo Porto (marcelo@animati.com.br)
 */
public class PrintDialog extends javax.swing.JDialog {

    /** A return status code - returned if Cancel button has been pressed */
    public static final int RET_CANCEL = 0;
    /** A return status code - returned if OK button has been pressed */
    public static final int RET_OK = 1;

    /** Creates new form PrintDialog */
    public PrintDialog(java.awt.Frame parent, boolean modal, ImageViewerEventManager eventManager) {
        super(parent, modal);
        this.eventManager = eventManager;
        initComponents();

        // Close the dialog when Esc is pressed
        String cancelName = "cancel";
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelName);
        ActionMap actionMap = getRootPane().getActionMap();
        actionMap.put(cancelName, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                doClose(RET_CANCEL);
            }
        });
        setVisible(true);
    }

    /** @return the return status of this dialog - one of RET_OK or RET_CANCEL */
    public int getReturnStatus() {
        return returnStatus;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        printButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        customImageSizeLabel = new javax.swing.JLabel();
        customImageSizeSlider = new javax.swing.JSlider();
        sliderValue = new javax.swing.JLabel();
        positionLabel = new javax.swing.JLabel();
        positionComboBox = new javax.swing.JComboBox();
        annotationsCheckBox = new javax.swing.JCheckBox();
        imageSizeLabel = new javax.swing.JLabel();
        imageSizeComboBox = new javax.swing.JComboBox();

        setMinimumSize(new java.awt.Dimension(480, 300));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        printButton.setText("Print");
        printButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        customImageSizeLabel.setText("Custom image size:");

        customImageSizeSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                customImageSizeSliderStateChanged(evt);
            }
        });

        sliderValue.setText("50%");

        positionLabel.setText("Image position:");

        positionComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Centralized", "Top-left" }));

        annotationsCheckBox.setText("Print image with annotations");
        annotationsCheckBox.setSelected(true);

        imageSizeLabel.setText("Image size:");

        imageSizeComboBox.setModel(new javax.swing.DefaultComboBoxModel(SCALE.values()));
        imageSizeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                imageSizeComboBoxActionPerformed(evt);
            }
        });
        if (eventManager.getSelectedView2dContainer().getImagePanels().size() > 1) {
            imageSizeComboBox.setSelectedItem(SCALE.FitToPage);
            imageSizeComboBox.setEnabled(false);
        }

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(imageSizeLabel)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(customImageSizeLabel)
                            .addComponent(positionLabel))
                        .addGap(26, 26, 26)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(positionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(imageSizeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(customImageSizeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(sliderValue)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 42, Short.MAX_VALUE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(printButton, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)))
                                .addComponent(cancelButton))))
                    .addComponent(annotationsCheckBox))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, printButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(imageSizeLabel)
                    .addComponent(imageSizeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(customImageSizeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sliderValue)
                    .addComponent(customImageSizeLabel))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(positionLabel)
                    .addComponent(positionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(annotationsCheckBox)
                .addGap(137, 137, 137))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(223, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(printButton))
                .addGap(49, 49, 49))
        );

        getRootPane().setDefaultButton(printButton);
        customImageSizeLabel.setVisible(false);
        customImageSizeSlider.setVisible(false);
        sliderValue.setVisible(false);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void imageSizeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_imageSizeComboBoxActionPerformed
        if (imageSizeComboBox.getSelectedItem() != SCALE.Custom) {
            customImageSizeLabel.setVisible(false);
            customImageSizeSlider.setVisible(false);
            sliderValue.setVisible(false);
        } else {
            customImageSizeLabel.setVisible(true);
            customImageSizeSlider.setVisible(true);
            sliderValue.setVisible(true);
        }
    }// GEN-LAST:event_imageSizeComboBoxActionPerformed

    private void printButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_printButtonActionPerformed
        Float imageScale = (float) customImageSizeSlider.getValue() / 100;
        PrintOptions printOptions = new PrintOptions(annotationsCheckBox.isSelected(), imageScale);
        printOptions.setScale((SCALE) imageSizeComboBox.getSelectedItem());
        printOptions.setHasAnnotations(annotationsCheckBox.isSelected());
        if (positionComboBox.getSelectedItem().equals("Centralized")) {
            printOptions.setCenter(true);
        } else {
            printOptions.setCenter(false);
        }

        // One View
        // ExportImage<ImageElement> exportImage = new ExportImage<ImageElement>(eventManager.getSelectedViewPane());
        // ImagePrint print = new ImagePrint(exportImage, printOptions);

        // Several views
        ImageViewerPlugin container = eventManager.getSelectedView2dContainer();
        if (container.getLayoutModel().getUIName().equals("DICOM information")) {
            JOptionPane.showMessageDialog(this, "Cannot print image in the current layout.", "Error", JOptionPane.ERROR_MESSAGE);
            doClose(RET_CANCEL);
            return;
        }
        ExportLayout<ImageElement> layout =
            new ExportLayout<ImageElement>(container.getImagePanels(), container.getLayoutModel());
        ImagePrint print = new ImagePrint(layout, printOptions);
        

        print.print();
        layout.dispose();
        // exportImage.dispose();

        doClose(RET_OK);

    }// GEN-LAST:event_printButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cancelButtonActionPerformed
        doClose(RET_CANCEL);
    }// GEN-LAST:event_cancelButtonActionPerformed

    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt) {// GEN-FIRST:event_closeDialog
        doClose(RET_CANCEL);
    }// GEN-LAST:event_closeDialog

    private void customImageSizeSliderStateChanged(javax.swing.event.ChangeEvent evt) {// GEN-FIRST:event_imageSizeSliderStateChanged
        if (!customImageSizeSlider.getValueIsAdjusting()) {
            // Get new value
            int value = customImageSizeSlider.getValue();
            sliderValue.setText(value + "%");
        }
    }// GEN-LAST:event_imageSizeSliderStateChanged

    private void doClose(int retStatus) {
        returnStatus = retStatus;
        setVisible(false);
        dispose();
    }

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        // <editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(PrintDialog.class.getName()).log(java.util.logging.Level.SEVERE, null,
                ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(PrintDialog.class.getName()).log(java.util.logging.Level.SEVERE, null,
                ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(PrintDialog.class.getName()).log(java.util.logging.Level.SEVERE, null,
                ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(PrintDialog.class.getName()).log(java.util.logging.Level.SEVERE, null,
                ex);
        }
        // </editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                PrintDialog dialog = new PrintDialog(new javax.swing.JFrame(), true, null);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {

                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox annotationsCheckBox;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel customImageSizeLabel;
    private javax.swing.JSlider customImageSizeSlider;
    private javax.swing.JComboBox imageSizeComboBox;
    private javax.swing.JLabel imageSizeLabel;
    private javax.swing.JComboBox positionComboBox;
    private javax.swing.JLabel positionLabel;
    private javax.swing.JButton printButton;
    private javax.swing.JLabel sliderValue;
    // End of variables declaration//GEN-END:variables
    private int returnStatus = RET_CANCEL;
    private ImageViewerEventManager eventManager;
}
