/*
 * FILE MODIFIED BY RYAN SAKOWSKI
 * 
 * 
 * Copyright 2009 Volker Oth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package lemmini.extract;

import java.awt.Toolkit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import lemmini.LemminiFrame;
import org.apache.commons.lang3.StringUtils;

/**
 * Frame to enter source and destination paths for resource extraction.
 *
 * @author Volker Oth
 */
public class FolderFrame extends JFrame {
    
    private static final long serialVersionUID = 0x01L;
    
    /** destination (Lemmini resource) path for extraction */
    private String destination;
    /** source (WINLEMM) path for extraction */
    private String source;
    /** flag that tells whether to extract or not */
    private boolean doExtract = false;
    
    /**
     * Creates new form FolderFrame
     */
    public FolderFrame() {
        initComponents();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabelHeader = new javax.swing.JLabel();
        jLabelSrc = new javax.swing.JLabel();
        jTextFieldSrc = new javax.swing.JTextField();
        jButtonSrc = new javax.swing.JButton();
        jLabelDest = new javax.swing.JLabel();
        jTextFieldDest = new javax.swing.JTextField();
        jButtonDest = new javax.swing.JButton();
        jButtonExtract = new javax.swing.JButton();
        jButtonQuit = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("SuperLemmini Resource Extractor");
        setIconImage(Toolkit.getDefaultToolkit().getImage(LemminiFrame.class.getClassLoader().getResource("icon_32.png")));
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        jLabelHeader.setText("Extract the resources from Lemmings for Windows.");

        jLabelSrc.setLabelFor(jTextFieldSrc);
        jLabelSrc.setText("Lemmings for Windows (WINLEMM) Path");

        jTextFieldSrc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldSrcActionPerformed(evt);
            }
        });

        jButtonSrc.setText("Browse...");
        jButtonSrc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSrcActionPerformed(evt);
            }
        });

        jLabelDest.setLabelFor(jTextFieldDest);
        jLabelDest.setText("Destination Path (must be different from WINLEMM path)");

        jTextFieldDest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldDestActionPerformed(evt);
            }
        });

        jButtonDest.setText("Browse...");
        jButtonDest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDestActionPerformed(evt);
            }
        });

        jButtonExtract.setText("Extract");
        jButtonExtract.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExtractActionPerformed(evt);
            }
        });

        jButtonQuit.setText("Quit");
        jButtonQuit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonQuitActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jTextFieldSrc)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonSrc))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabelHeader)
                            .addComponent(jLabelSrc)
                            .addComponent(jLabelDest))
                        .addGap(0, 166, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jTextFieldDest)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonDest))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButtonExtract)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonQuit)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabelHeader)
                .addGap(18, 18, 18)
                .addComponent(jLabelSrc)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldSrc, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonSrc))
                .addGap(18, 18, 18)
                .addComponent(jLabelDest)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldDest, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonDest))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 18, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonExtract)
                    .addComponent(jButtonQuit))
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents
    
    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        source = jTextFieldSrc.getText();
        destination = jTextFieldDest.getText();
        synchronized (this) {
            notifyAll();
        }
    }//GEN-LAST:event_formWindowClosed
    
    private void jTextFieldSrcActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldSrcActionPerformed
        source = jTextFieldSrc.getText();
    }//GEN-LAST:event_jTextFieldSrcActionPerformed
    
    private void jButtonSrcActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSrcActionPerformed
        JFileChooser jf = new JFileChooser(source);
        jf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = jf.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            source = jf.getSelectedFile().getAbsolutePath();
            jTextFieldSrc.setText(source);
        }
    }//GEN-LAST:event_jButtonSrcActionPerformed
    
    private void jTextFieldDestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldDestActionPerformed
        destination = jTextFieldDest.getText();
    }//GEN-LAST:event_jTextFieldDestActionPerformed
    
    private void jButtonDestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDestActionPerformed
        JFileChooser jf = new JFileChooser(destination);
        jf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = jf.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            destination = jf.getSelectedFile().getAbsolutePath();
            jTextFieldDest.setText(destination);
        }
    }//GEN-LAST:event_jButtonDestActionPerformed
    
    private void jButtonExtractActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonExtractActionPerformed
        source = jTextFieldSrc.getText();
        destination = jTextFieldDest.getText();
        // check if source path exists
        Path sourcePath = Paths.get(source);
        if (Files.isDirectory(sourcePath)) {
            Path destinationPath = Paths.get(destination);
            if (!sourcePath.equals(destinationPath)) {
                doExtract = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Target path must be different from the Windows Lemmings path.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, String.format("Windows Lemmings path %s doesn't exist!", sourcePath), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_jButtonExtractActionPerformed
    
    private void jButtonQuitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonQuitActionPerformed
        doExtract = false;
        dispose();
    }//GEN-LAST:event_jButtonQuitActionPerformed
    
    /**
     * Set parameters for text edit boxes.
     * @param srcPath source (WINLEMM) path for extraction
     * @param destPath destination (Lemmini resource) path for extraction
     */
    public void setParameters(final Path srcPath, final Path destPath) {
        source = srcPath.toString();
        jTextFieldSrc.setText(source);
        destination = destPath.toString();
        jTextFieldDest.setText(destination);
    }
    
    /**
     * Get destination (Lemmini resource) path for extraction.
     * @return destination (Lemmini resource) path for extraction
     */
    public Path getDestination() {
        if (destination != null) {
            return Paths.get(destination);
        } else {
            return Paths.get(StringUtils.EMPTY);
        }
    }
    
    /**
     * Get source (WINLEMM) path for extraction.
     * @return source (WINLEMM) path for extraction
     */
    public Path getSource() {
        if (source != null) {
            return Paths.get(source);
        } else {
            return Paths.get(StringUtils.EMPTY);
        }
    }
    
    /**
     * Get extraction selection status.
     * @return true if extraction was chosen, false otherwise
     */
    public boolean getSuccess() {
        return doExtract;
    }
    
    /**
     * Blocks until the window is closed. If this window is already closed,
     * then this method returns immediately.
     */
    public synchronized void waitUntilClosed() {
        while (isVisible()) {
            try {
                wait();
            } catch (InterruptedException ex) {
            }
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonDest;
    private javax.swing.JButton jButtonExtract;
    private javax.swing.JButton jButtonQuit;
    private javax.swing.JButton jButtonSrc;
    private javax.swing.JLabel jLabelDest;
    private javax.swing.JLabel jLabelHeader;
    private javax.swing.JLabel jLabelSrc;
    private javax.swing.JTextField jTextFieldDest;
    private javax.swing.JTextField jTextFieldSrc;
    // End of variables declaration//GEN-END:variables
}
