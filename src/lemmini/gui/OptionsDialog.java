/*
 * Copyright 2014 Ryan Sakowski.
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

import java.awt.Frame;
import java.awt.Toolkit;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import lemmini.LemminiFrame;
import lemmini.game.Core;
import lemmini.game.GameController;
import lemmini.sound.Music;

/**
 *
 * @author Ryan Sakowski
 */
public class OptionsDialog extends JDialog {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
     * Creates new form OptionsDialog
     * @param parent
     * @param modal
     */
    public OptionsDialog(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setLocationRelativeTo(parent);
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelSound = new javax.swing.JPanel();
        jCheckBoxEnableMusic = new javax.swing.JCheckBox();
        jLabelMusicVolume = new javax.swing.JLabel();
        jSliderMusicVolume = new javax.swing.JSlider();
        jCheckBoxEnableSound = new javax.swing.JCheckBox();
        jLabelSoundVolume = new javax.swing.JLabel();
        jSliderSoundVolume = new javax.swing.JSlider();
        jLabelMixer = new javax.swing.JLabel();
        jComboBoxMixer = new JComboBox<String>(GameController.sound.getMixers());
        jPanelGraphics = new javax.swing.JPanel();
        jCheckBoxBilinear = new javax.swing.JCheckBox();
        jPanelMisc = new javax.swing.JPanel();
        jCheckBoxAdvanced = new javax.swing.JCheckBox();
        jCheckBoxClassicCursor = new javax.swing.JCheckBox();
        jCheckBoxSwap = new javax.swing.JCheckBox();
        jCheckBoxFaster = new javax.swing.JCheckBox();
        jCheckBoxPauseStopsFastForward = new javax.swing.JCheckBox();
        jCheckBoxNoPercentages = new javax.swing.JCheckBox();
        jCheckBoxReplayScroll = new javax.swing.JCheckBox();
        jCheckBoxUnpauseOnAssignment = new javax.swing.JCheckBox();
        jCheckBoxTimedBombers = new javax.swing.JCheckBox();
        jCheckBoxUnlockAllLevels = new javax.swing.JCheckBox();
        jCheckBoxDisableScrollWheel = new javax.swing.JCheckBox();
        jCheckBoxDisableFrameStepping = new javax.swing.JCheckBox();
        jCheckBoxVisualSfx = new javax.swing.JCheckBox();
        jCheckBoxEnhancedStatus = new javax.swing.JCheckBox();
        jButtonOK = new javax.swing.JButton();
        jButtonCancel = new javax.swing.JButton();
        jButtonApply = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Options");
        setIconImage(Toolkit.getDefaultToolkit().getImage(LemminiFrame.class.getClassLoader().getResource("icon_32.png")));
        setResizable(false);

        jPanelSound.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Sound", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jCheckBoxEnableMusic.setSelected(GameController.isOptionEnabled(GameController.Option.MUSIC_ON));
        jCheckBoxEnableMusic.setText("Enable Music");

        jLabelMusicVolume.setLabelFor(jSliderMusicVolume);
        jLabelMusicVolume.setText("Music Volume");

        jSliderMusicVolume.setMajorTickSpacing(10);
        jSliderMusicVolume.setMaximum(200);
        jSliderMusicVolume.setPaintTicks(true);
        jSliderMusicVolume.setValue((int) (100 * GameController.getMusicGain()));

        jCheckBoxEnableSound.setSelected(GameController.isOptionEnabled(GameController.Option.SOUND_ON));
        jCheckBoxEnableSound.setText("Enable Sound Effects");

        jLabelSoundVolume.setLabelFor(jSliderSoundVolume);
        jLabelSoundVolume.setText("Sound Volume");

        jSliderSoundVolume.setMajorTickSpacing(10);
        jSliderSoundVolume.setMaximum(200);
        jSliderSoundVolume.setPaintTicks(true);
        jSliderSoundVolume.setValue((int) (100 * GameController.getSoundGain()));

        jLabelMixer.setText("SFX Mixer");

        jCheckBoxVisualSfx.setSelected(GameController.isOptionEnabled(GameController.Option.VISUAL_SFX));
        jCheckBoxVisualSfx.setText("Visual SFX");
        
        jCheckBoxEnhancedStatus.setSelected(GameController.isOptionEnabled(GameController.Option.ENHANCED_STATUS));
        jCheckBoxEnhancedStatus.setText("Enhanced Status Bar");

        jComboBoxMixer.setSelectedIndex(GameController.sound.getMixerIdx());

        javax.swing.GroupLayout jPanelSoundLayout = new javax.swing.GroupLayout(jPanelSound);
        jPanelSound.setLayout(jPanelSoundLayout);
        jPanelSoundLayout.setHorizontalGroup(
            jPanelSoundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSoundLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelSoundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jCheckBoxEnableSound)
                    .addComponent(jCheckBoxEnableMusic)
                    .addComponent(jLabelMusicVolume)
                    .addComponent(jSliderMusicVolume, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelSoundVolume)
                    .addComponent(jSliderSoundVolume, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelMixer)
                    .addComponent(jComboBoxMixer, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jCheckBoxVisualSfx)
                    )
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelSoundLayout.setVerticalGroup(
            jPanelSoundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSoundLayout.createSequentialGroup()
                .addContainerGap()
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
                .addComponent(jLabelMixer)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboBoxMixer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(37, Short.MAX_VALUE)
                .addComponent(jCheckBoxVisualSfx)
                )
        );

        jPanelGraphics.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Graphics", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jCheckBoxBilinear.setSelected(Core.isBilinear());
        jCheckBoxBilinear.setText("Bilinear Filtering");

        javax.swing.GroupLayout jPanelGraphicsLayout = new javax.swing.GroupLayout(jPanelGraphics);
        jPanelGraphics.setLayout(jPanelGraphicsLayout);
        jPanelGraphicsLayout.setHorizontalGroup(
            jPanelGraphicsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelGraphicsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCheckBoxBilinear)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelGraphicsLayout.setVerticalGroup(
            jPanelGraphicsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelGraphicsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCheckBoxBilinear)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelMisc.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Miscellaneous", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jCheckBoxAdvanced.setSelected(GameController.isOptionEnabled(GameController.Option.ADVANCED_SELECT));
        jCheckBoxAdvanced.setText("Advanced Select");
        jCheckBoxAdvanced.setToolTipText("Hold directional keys to select only Lemmings going in that same direction. Hold Up to select only Walkers.");

        jCheckBoxClassicCursor.setSelected(GameController.isOptionEnabled(GameController.Option.CLASSIC_CURSOR));
        jCheckBoxClassicCursor.setText("Classic Cursor");
        jCheckBoxClassicCursor.setToolTipText("The Standard Cursor centers around the selected lemming. The Classic Cursor follows the mouse.");

        jCheckBoxSwap.setSelected(GameController.isOptionEnabled(GameController.Option.SWAP_BUTTONS));
        jCheckBoxSwap.setText("Swap Middle/Right Mouse Buttons");
        jCheckBoxSwap.setToolTipText("When disabled: Middle button drags the viewport, Right button only selects Walkers.");

        jCheckBoxFaster.setSelected(GameController.isOptionEnabled(GameController.Option.FASTER_FAST_FORWARD));
        jCheckBoxFaster.setText("Double Fast-Forward Speed");
        jCheckBoxFaster.setToolTipText("Standard Fast-Forward is 3x faster than normal. Doubled Fast-Forward is 6x faster than normal.");

        jCheckBoxPauseStopsFastForward.setSelected(GameController.isOptionEnabled(GameController.Option.PAUSE_STOPS_FAST_FORWARD));
        jCheckBoxPauseStopsFastForward.setText("Stop Fast-Forward When Pausing");

        jCheckBoxNoPercentages.setSelected(GameController.isOptionEnabled(GameController.Option.NO_PERCENTAGES));
        jCheckBoxNoPercentages.setText("Never Show Percentages");

        jCheckBoxReplayScroll.setSelected(GameController.isOptionEnabled(GameController.Option.REPLAY_SCROLL));
        jCheckBoxReplayScroll.setText("Scroll Level During Replay");

        jCheckBoxUnpauseOnAssignment.setSelected(GameController.isOptionEnabled(GameController.Option.UNPAUSE_ON_ASSIGNMENT));
        jCheckBoxUnpauseOnAssignment.setText("Unpause After Assigning Skill");

        jCheckBoxTimedBombers.setSelected(GameController.isOptionEnabled(GameController.Option.TIMED_BOMBERS));
        jCheckBoxTimedBombers.setText("Enable 5 second timed bombers");

        jCheckBoxUnlockAllLevels.setSelected(GameController.isOptionEnabled(GameController.Option.UNLOCK_ALL_LEVELS));
        jCheckBoxUnlockAllLevels.setText("Unlock all levels");
        jCheckBoxUnlockAllLevels.setToolTipText("All access to all levels, without having to complete previous ones.");

        jCheckBoxDisableScrollWheel.setSelected(GameController.isOptionEnabled(GameController.Option.DISABLE_SCROLL_WHEEL));
        jCheckBoxDisableScrollWheel.setText("Disable Scroll Wheel");
        jCheckBoxDisableScrollWheel.setToolTipText("Prevent the scroll wheel from changing the selected skill.");

        jCheckBoxDisableFrameStepping.setSelected(GameController.isOptionEnabled(GameController.Option.DISABLE_FRAME_STEPPING));
        jCheckBoxDisableFrameStepping.setText("Disable Frame Stepping");
        jCheckBoxDisableFrameStepping.setToolTipText("Disable advancing the game by single frames when paused.");
        
        javax.swing.GroupLayout jPanelMiscLayout = new javax.swing.GroupLayout(jPanelMisc);
        jPanelMisc.setLayout(jPanelMiscLayout);
        jPanelMiscLayout.setHorizontalGroup(
            jPanelMiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMiscLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelMiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBoxAdvanced)
                    .addComponent(jCheckBoxClassicCursor)
                    .addComponent(jCheckBoxSwap)
                    .addComponent(jCheckBoxFaster)
                    .addComponent(jCheckBoxNoPercentages)
                    .addComponent(jCheckBoxReplayScroll)
                    .addComponent(jCheckBoxPauseStopsFastForward)
                    .addComponent(jCheckBoxUnpauseOnAssignment)
                	.addComponent(jCheckBoxTimedBombers)
                	.addComponent(jCheckBoxUnlockAllLevels)
                	.addComponent(jCheckBoxDisableScrollWheel)
                	.addComponent(jCheckBoxDisableFrameStepping)
                	.addComponent(jCheckBoxEnhancedStatus)
                	)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelMiscLayout.setVerticalGroup(
            jPanelMiscLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMiscLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCheckBoxAdvanced)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxClassicCursor)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxSwap)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxFaster)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxPauseStopsFastForward)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxNoPercentages)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxReplayScroll)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxUnpauseOnAssignment)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxTimedBombers)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxUnlockAllLevels)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxDisableScrollWheel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxDisableFrameStepping)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxEnhancedStatus)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

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
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButtonOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonCancel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonApply)
                    )
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanelSound, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanelGraphics, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanelMisc, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        )
                    )
                )
                .addContainerGap()
            )
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelSound, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelGraphics, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanelMisc, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    )
                )
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonApply)
                    .addComponent(jButtonCancel)
                    .addComponent(jButtonOK)
                )
                .addContainerGap()
            )
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
    	// set all game settings based on GUI options first
    	// apply sound settings
        GameController.setOption(GameController.Option.MUSIC_ON, jCheckBoxEnableMusic.isSelected());
        if (GameController.getLevel() != null) {
            if (GameController.isOptionEnabled(GameController.Option.MUSIC_ON)
                    && GameController.getGameState() == GameController.State.LEVEL) {
                Music.play();
            } else {
                Music.stop();
            }
        }
        GameController.setMusicGain(jSliderMusicVolume.getValue() / 100.0);
        GameController.setOption(GameController.Option.SOUND_ON, jCheckBoxEnableSound.isSelected());
        GameController.setSoundGain(jSliderSoundVolume.getValue() / 100.0);
        GameController.sound.setMixerIdx(jComboBoxMixer.getSelectedIndex());
        // apply graphics settings
        Core.setBilinear(jCheckBoxBilinear.isSelected());
        // apply miscellaneous settings
        GameController.setOption(GameController.Option.ADVANCED_SELECT, jCheckBoxAdvanced.isSelected());
        GameController.setOption(GameController.Option.CLASSIC_CURSOR, jCheckBoxClassicCursor.isSelected());
        GameController.setOption(GameController.Option.SWAP_BUTTONS, jCheckBoxSwap.isSelected());
        GameController.setOption(GameController.Option.FASTER_FAST_FORWARD, jCheckBoxFaster.isSelected());
        GameController.setOption(GameController.Option.PAUSE_STOPS_FAST_FORWARD, jCheckBoxPauseStopsFastForward.isSelected());
        GameController.setOption(GameController.Option.NO_PERCENTAGES, jCheckBoxNoPercentages.isSelected());
        GameController.setOption(GameController.Option.REPLAY_SCROLL, jCheckBoxReplayScroll.isSelected());
        GameController.setOption(GameController.Option.UNPAUSE_ON_ASSIGNMENT, jCheckBoxUnpauseOnAssignment.isSelected());
        GameController.setOption(GameController.Option.TIMED_BOMBERS, jCheckBoxTimedBombers.isSelected());
        GameController.setOption(GameController.Option.UNLOCK_ALL_LEVELS, jCheckBoxUnlockAllLevels.isSelected());
        GameController.setOption(GameController.Option.DISABLE_SCROLL_WHEEL, jCheckBoxDisableScrollWheel.isSelected());
        GameController.setOption(GameController.Option.DISABLE_FRAME_STEPPING, jCheckBoxDisableFrameStepping.isSelected());
        GameController.setOption(GameController.Option.VISUAL_SFX, jCheckBoxVisualSfx.isSelected());
        GameController.setOption(GameController.Option.ENHANCED_STATUS, jCheckBoxEnhancedStatus.isSelected());
        
        //then commit all those settings to disk
        Core.saveSettings();
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
    private javax.swing.JCheckBox jCheckBoxPauseStopsFastForward;
    private javax.swing.JCheckBox jCheckBoxReplayScroll;
    private javax.swing.JCheckBox jCheckBoxSwap;
    private javax.swing.JCheckBox jCheckBoxUnpauseOnAssignment;
    private javax.swing.JCheckBox jCheckBoxTimedBombers;
    private javax.swing.JCheckBox jCheckBoxUnlockAllLevels;
    private javax.swing.JCheckBox jCheckBoxDisableScrollWheel;
    private javax.swing.JCheckBox jCheckBoxDisableFrameStepping;
    private javax.swing.JCheckBox jCheckBoxVisualSfx;
    private javax.swing.JCheckBox jCheckBoxEnhancedStatus;
    private javax.swing.JComboBox<String> jComboBoxMixer;
    private javax.swing.JLabel jLabelMixer;
    private javax.swing.JLabel jLabelMusicVolume;
    private javax.swing.JLabel jLabelSoundVolume;
    private javax.swing.JPanel jPanelGraphics;
    private javax.swing.JPanel jPanelMisc;
    private javax.swing.JPanel jPanelSound;
    private javax.swing.JSlider jSliderMusicVolume;
    private javax.swing.JSlider jSliderSoundVolume;
    // End of variables declaration//GEN-END:variables
}
