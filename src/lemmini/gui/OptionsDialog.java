/*
 * Copyright 2014 Ryan Sakwoski.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package lemmini.gui;

import java.awt.Toolkit;
import lemmini.LemminiFrame;
import lemmini.game.Core;
import lemmini.game.GameController;
import lemmini.sound.Music;

/**
 *
 * @author Ryan Sakwoski
 */
public class OptionsDialog extends javax.swing.JDialog {

    /**
     * Creates new form OptionsDialog
     */
    public OptionsDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setLocationRelativeTo(parent);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabelSound = new javax.swing.JLabel();
        jCheckBoxEnableMusic = new javax.swing.JCheckBox();
        jLabelMusicVolume = new javax.swing.JLabel();
        jSliderMusicVolume = new javax.swing.JSlider();
        jCheckBoxEnableSound = new javax.swing.JCheckBox();
        jLabelSoundVolume = new javax.swing.JLabel();
        jSliderSoundVolume = new javax.swing.JSlider();
        jLabelMixer = new javax.swing.JLabel();
        jComboBoxMixer = new javax.swing.JComboBox<String>(GameController.sound.getMixers());
        jSeparatorCenter = new javax.swing.JSeparator();
        jLabelGraphics = new javax.swing.JLabel();
        jCheckBoxBilinear = new javax.swing.JCheckBox();
        jSeparatorRight = new javax.swing.JSeparator();
        jLabelMisc = new javax.swing.JLabel();
        jCheckBoxAdvanced = new javax.swing.JCheckBox();
        jCheckBoxClassicCursor = new javax.swing.JCheckBox();
        jCheckBoxSwap = new javax.swing.JCheckBox();
        jCheckBoxFaster = new javax.swing.JCheckBox();
        jCheckBoxNoPercentages = new javax.swing.JCheckBox();
        jSeparatorBottom = new javax.swing.JSeparator();
        jButtonOK = new javax.swing.JButton();
        jButtonCancel = new javax.swing.JButton();
        jButtonApply = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Options");
        setIconImage(Toolkit.getDefaultToolkit().getImage(LemminiFrame.class.getClassLoader().getResource("icon_32.png")));
        setResizable(false);

        jLabelSound.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelSound.setText("Sound");

        jCheckBoxEnableMusic.setSelected(GameController.isMusicOn());
        jCheckBoxEnableMusic.setText("Enable Music");

        jLabelMusicVolume.setLabelFor(jSliderMusicVolume);
        jLabelMusicVolume.setText("Music Volume");

        jSliderMusicVolume.setMajorTickSpacing(10);
        jSliderMusicVolume.setMaximum(200);
        jSliderMusicVolume.setPaintTicks(true);
        jSliderMusicVolume.setValue((int) (100 * GameController.getMusicGain()));

        jCheckBoxEnableSound.setSelected(GameController.isSoundOn());
        jCheckBoxEnableSound.setText("Enable Sound Effects");

        jLabelSoundVolume.setText("Sound Volume");

        jSliderSoundVolume.setMajorTickSpacing(10);
        jSliderSoundVolume.setMaximum(200);
        jSliderSoundVolume.setPaintTicks(true);
        jSliderSoundVolume.setValue((int) (100 * GameController.getSoundGain()));

        jLabelMixer.setLabelFor(jComboBoxMixer);
        jLabelMixer.setText("SFX Mixer");

        jComboBoxMixer.setSelectedIndex(GameController.sound.getMixerIdx());

        jSeparatorCenter.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jLabelGraphics.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelGraphics.setText("Graphics");

        jCheckBoxBilinear.setSelected(Core.isBilinear());
        jCheckBoxBilinear.setText("Bilinear Filtering");

        jLabelMisc.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelMisc.setText("Miscellaneous");

        jCheckBoxAdvanced.setSelected(GameController.isAdvancedSelect());
        jCheckBoxAdvanced.setText("Advanced Select");

        jCheckBoxClassicCursor.setSelected(GameController.isClassicCursor());
        jCheckBoxClassicCursor.setText("Classic Cursor");

        jCheckBoxSwap.setSelected(GameController.doSwapButtons());
        jCheckBoxSwap.setText("Swap Middle/Right Mouse Buttons");

        jCheckBoxFaster.setSelected(GameController.isFasterFastForward());
        jCheckBoxFaster.setText("Increase Fast-Forward Speed");

        jCheckBoxNoPercentages.setSelected(GameController.isNoPercentages());
        jCheckBoxNoPercentages.setText("Never Show Percentages");

        jButtonOK.setText("OK");
        jButtonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOKActionPerformed(evt);
            }
        });

        jButtonCancel.setText("Cancel");
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });

        jButtonApply.setText("Apply");
        jButtonApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonApplyActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparatorBottom)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jLabelSound, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jSliderMusicVolume, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jComboBoxMixer, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabelMusicVolume, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabelSoundVolume, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabelMixer, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jSliderSoundVolume, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(jCheckBoxEnableMusic)
                            .addComponent(jCheckBoxEnableSound))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparatorCenter, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabelMisc, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabelGraphics, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jSeparatorRight, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jCheckBoxAdvanced)
                                    .addComponent(jCheckBoxSwap)
                                    .addComponent(jCheckBoxFaster)
                                    .addComponent(jCheckBoxNoPercentages)
                                    .addComponent(jCheckBoxBilinear)
                                    .addComponent(jCheckBoxClassicCursor))
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButtonOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonCancel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonApply)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelSound)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jCheckBoxEnableMusic)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabelMusicVolume)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSliderMusicVolume, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jCheckBoxEnableSound)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabelSoundVolume)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSliderSoundVolume, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabelMixer))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelGraphics)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jCheckBoxBilinear)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSeparatorRight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabelMisc)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jCheckBoxAdvanced)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckBoxClassicCursor)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckBoxSwap)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckBoxFaster)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckBoxNoPercentages)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBoxMixer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 4, Short.MAX_VALUE))
                    .addComponent(jSeparatorCenter))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparatorBottom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonApply)
                    .addComponent(jButtonCancel)
                    .addComponent(jButtonOK))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOKActionPerformed
        applyChanges();
        dispose();
    }//GEN-LAST:event_jButtonOKActionPerformed

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancelActionPerformed
        dispose();
    }//GEN-LAST:event_jButtonCancelActionPerformed

    private void jButtonApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonApplyActionPerformed
        applyChanges();
    }//GEN-LAST:event_jButtonApplyActionPerformed
    
    private void applyChanges() {
        // apply sound settings
        GameController.setMusicOn(jCheckBoxEnableMusic.isSelected());
        Core.programProps.setBoolean("music", GameController.isMusicOn());
        if (GameController.getLevel() != null) {
            if (GameController.isMusicOn() && GameController.getGameState() == GameController.State.LEVEL) {
                Music.play();
            } else {
                Music.stop();
            }
        }
        GameController.setMusicGain(jSliderMusicVolume.getValue() / 100.0);
        GameController.setSoundOn(jCheckBoxEnableSound.isSelected());
        Core.programProps.setBoolean("sound", GameController.isSoundOn());
        GameController.setSoundGain(jSliderSoundVolume.getValue() / 100.0);
        GameController.sound.setMixerIdx(jComboBoxMixer.getSelectedIndex());
        Core.programProps.set("mixerName", GameController.sound.getMixers()[GameController.sound.getMixerIdx()]);
        // apply graphics settings
        Core.setBilinear(jCheckBoxBilinear.isSelected());
        Core.programProps.setBoolean("bilinear", Core.isBilinear());
        // apply miscellaneous settings
        GameController.setAdvancedSelect(jCheckBoxAdvanced.isSelected());
        Core.programProps.setBoolean("advancedSelect", GameController.isAdvancedSelect());
        GameController.setClassicCursor(jCheckBoxClassicCursor.isSelected());
        Core.programProps.setBoolean("classicalCursor", GameController.isClassicCursor());
        GameController.setSwapButtons(jCheckBoxSwap.isSelected());
        Core.programProps.setBoolean("swapButtons", GameController.doSwapButtons());
        GameController.setFasterFastForward(jCheckBoxFaster.isSelected());
        Core.programProps.setBoolean("fasterFastForward", GameController.isFasterFastForward());
        GameController.setNoPercentages(jCheckBoxNoPercentages.isSelected());
        Core.programProps.setBoolean("noPercentages", GameController.isNoPercentages());
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonApply;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonOK;
    private javax.swing.JCheckBox jCheckBoxAdvanced;
    private javax.swing.JCheckBox jCheckBoxBilinear;
    private javax.swing.JCheckBox jCheckBoxClassicCursor;
    private javax.swing.JCheckBox jCheckBoxEnableMusic;
    private javax.swing.JCheckBox jCheckBoxEnableSound;
    private javax.swing.JCheckBox jCheckBoxFaster;
    private javax.swing.JCheckBox jCheckBoxNoPercentages;
    private javax.swing.JCheckBox jCheckBoxSwap;
    private javax.swing.JComboBox<String> jComboBoxMixer;
    private javax.swing.JLabel jLabelGraphics;
    private javax.swing.JLabel jLabelMisc;
    private javax.swing.JLabel jLabelMixer;
    private javax.swing.JLabel jLabelMusicVolume;
    private javax.swing.JLabel jLabelSound;
    private javax.swing.JLabel jLabelSoundVolume;
    private javax.swing.JSeparator jSeparatorBottom;
    private javax.swing.JSeparator jSeparatorCenter;
    private javax.swing.JSeparator jSeparatorRight;
    private javax.swing.JSlider jSliderMusicVolume;
    private javax.swing.JSlider jSliderSoundVolume;
    // End of variables declaration//GEN-END:variables
}