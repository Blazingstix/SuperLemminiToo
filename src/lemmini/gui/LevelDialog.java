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

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JOptionPane;
import javax.swing.tree.*;
import lemmini.Lemmini;
import lemmini.game.*;
import lemmini.tools.ToolBox;

/**
 *
 * @author Ryan Sakowski
 */
public class LevelDialog extends javax.swing.JDialog {
    
    private static Path lvlPath = Paths.get(".");
    
    private DefaultMutableTreeNode topNode = null;
    private DefaultTreeModel levelModel = null;
    private LevelItem selectedLevel = null;

    /**
     * Creates new form LevelDialog
     */
    public LevelDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
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

        jScrollPaneLevels = new javax.swing.JScrollPane();
        topNode = new DefaultMutableTreeNode("Levels");
        levelModel = new DefaultTreeModel(topNode);
        refreshLevels();
        jTreeLevels = new javax.swing.JTree();
        jTreeLevels.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        jLabelLevelStats = new javax.swing.JLabel();
        jSeparatorLevelStats = new javax.swing.JSeparator();
        jLabelNumLemmings = new javax.swing.JLabel();
        jLabelNumToRescue = new javax.swing.JLabel();
        jLabelReleaseRate = new javax.swing.JLabel();
        jLabelTimeLimit = new javax.swing.JLabel();
        jTextFieldNumLemmings = new javax.swing.JTextField();
        jTextFieldNumToRescue = new javax.swing.JTextField();
        jTextFieldReleaseRate = new javax.swing.JTextField();
        jTextFieldTimeLimit = new javax.swing.JTextField();
        jSeparatorSkills = new javax.swing.JSeparator();
        jLabelNumClimbers = new javax.swing.JLabel();
        jLabelNumFloaters = new javax.swing.JLabel();
        jLabelNumBombers = new javax.swing.JLabel();
        jLabelNumBlockers = new javax.swing.JLabel();
        jLabelNumBuilders = new javax.swing.JLabel();
        jLabelNumBashers = new javax.swing.JLabel();
        jLabelNumMiners = new javax.swing.JLabel();
        jLabelNumDiggers = new javax.swing.JLabel();
        jTextFieldNumClimbers = new javax.swing.JTextField();
        jTextFieldNumFloaters = new javax.swing.JTextField();
        jTextFieldNumBombers = new javax.swing.JTextField();
        jTextFieldNumBlockers = new javax.swing.JTextField();
        jTextFieldNumBuilders = new javax.swing.JTextField();
        jTextFieldNumBashers = new javax.swing.JTextField();
        jTextFieldNumMiners = new javax.swing.JTextField();
        jTextFieldNumDiggers = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        jLabelRecords = new javax.swing.JLabel();
        jSeparatorRecords = new javax.swing.JSeparator();
        jLabelLemmingsSaved = new javax.swing.JLabel();
        jLabelSkillsUsed = new javax.swing.JLabel();
        jLabelTimeElapsed = new javax.swing.JLabel();
        jLabelScore = new javax.swing.JLabel();
        jTextFieldLemmingsSaved = new javax.swing.JTextField();
        jTextFieldSkillsUsed = new javax.swing.JTextField();
        jTextFieldTimeElapsed = new javax.swing.JTextField();
        jTextFieldScore = new javax.swing.JTextField();
        jButtonAddExternal = new javax.swing.JButton();
        jButtonOK = new javax.swing.JButton();
        jButtonCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Select Level");
        setResizable(false);

        jTreeLevels.setModel(levelModel);
        jTreeLevels.setRootVisible(false);
        jTreeLevels.setShowsRootHandles(true);
        jTreeLevels.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jTreeLevelsMousePressed(evt);
            }
        });
        jTreeLevels.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                jTreeLevelsValueChanged(evt);
            }
        });
        jTreeLevels.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTreeLevelsKeyPressed(evt);
            }
        });
        jScrollPaneLevels.setViewportView(jTreeLevels);

        jLabelLevelStats.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelLevelStats.setText("Level stats");

        jLabelNumLemmings.setLabelFor(jTextFieldNumLemmings);
        jLabelNumLemmings.setText("Number of Lemmings:");

        jLabelNumToRescue.setLabelFor(jTextFieldNumToRescue);
        jLabelNumToRescue.setText("Lemmings to be saved:");

        jLabelReleaseRate.setLabelFor(jTextFieldReleaseRate);
        jLabelReleaseRate.setText("Release rate:");

        jLabelTimeLimit.setLabelFor(jTextFieldTimeLimit);
        jLabelTimeLimit.setText("Time limit:");
        jLabelTimeLimit.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        jTextFieldNumLemmings.setEditable(false);

        jTextFieldNumToRescue.setEditable(false);

        jTextFieldReleaseRate.setEditable(false);

        jTextFieldTimeLimit.setEditable(false);

        jLabelNumClimbers.setLabelFor(jTextFieldNumClimbers);
        jLabelNumClimbers.setText("Climbers:");

        jLabelNumFloaters.setLabelFor(jTextFieldNumFloaters);
        jLabelNumFloaters.setText("Floaters:");

        jLabelNumBombers.setLabelFor(jLabelNumBombers);
        jLabelNumBombers.setText("Bombers:");

        jLabelNumBlockers.setLabelFor(jLabelNumBlockers);
        jLabelNumBlockers.setText("Blockers:");

        jLabelNumBuilders.setLabelFor(jLabelNumBuilders);
        jLabelNumBuilders.setText("Builders:");

        jLabelNumBashers.setLabelFor(jLabelNumBashers);
        jLabelNumBashers.setText("Bashers:");

        jLabelNumMiners.setLabelFor(jTextFieldNumMiners);
        jLabelNumMiners.setText("Miners:");

        jLabelNumDiggers.setLabelFor(jLabelNumDiggers);
        jLabelNumDiggers.setText("Diggers:");

        jTextFieldNumClimbers.setEditable(false);

        jTextFieldNumFloaters.setEditable(false);

        jTextFieldNumBombers.setEditable(false);

        jTextFieldNumBlockers.setEditable(false);

        jTextFieldNumBuilders.setEditable(false);

        jTextFieldNumBashers.setEditable(false);

        jTextFieldNumMiners.setEditable(false);

        jTextFieldNumDiggers.setEditable(false);

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jLabelRecords.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelRecords.setText("Records");

        jLabelLemmingsSaved.setLabelFor(jTextFieldLemmingsSaved);
        jLabelLemmingsSaved.setText("Most Lemmings saved:");

        jLabelSkillsUsed.setLabelFor(jTextFieldSkillsUsed);
        jLabelSkillsUsed.setText("Fewest skills used:");

        jLabelTimeElapsed.setLabelFor(jTextFieldTimeElapsed);
        jLabelTimeElapsed.setText("Best time (elapsed):");

        jLabelScore.setLabelFor(jTextFieldScore);
        jLabelScore.setText("Highest score:");

        jTextFieldLemmingsSaved.setEditable(false);

        jTextFieldSkillsUsed.setEditable(false);

        jTextFieldTimeElapsed.setEditable(false);

        jTextFieldScore.setEditable(false);

        jButtonAddExternal.setText("Add External Levels...");
        jButtonAddExternal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddExternalActionPerformed(evt);
            }
        });

        jButtonOK.setText("OK");
        jButtonOK.setEnabled(false);
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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jButtonAddExternal)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonOK)
                        .addGap(6, 6, 6)
                        .addComponent(jButtonCancel)
                        .addGap(10, 10, 10))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jScrollPaneLevels, javax.swing.GroupLayout.DEFAULT_SIZE, 382, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jLabelNumToRescue, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabelNumLemmings, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabelReleaseRate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabelTimeLimit, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabelNumClimbers, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabelNumFloaters, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabelNumBombers, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabelNumBlockers, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabelNumBuilders, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabelNumBashers, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabelNumMiners, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabelNumDiggers, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jTextFieldNumLemmings)
                                    .addComponent(jTextFieldNumToRescue)
                                    .addComponent(jTextFieldReleaseRate)
                                    .addComponent(jTextFieldTimeLimit)
                                    .addComponent(jTextFieldNumClimbers)
                                    .addComponent(jTextFieldNumFloaters)
                                    .addComponent(jTextFieldNumBombers)
                                    .addComponent(jTextFieldNumBlockers)
                                    .addComponent(jTextFieldNumBuilders)
                                    .addComponent(jTextFieldNumBashers)
                                    .addComponent(jTextFieldNumMiners)
                                    .addComponent(jTextFieldNumDiggers, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(jLabelLevelStats, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jSeparatorLevelStats)
                            .addComponent(jSeparatorSkills))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jLabelLemmingsSaved, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabelSkillsUsed, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabelTimeElapsed, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabelScore, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jTextFieldTimeElapsed)
                                    .addComponent(jTextFieldSkillsUsed)
                                    .addComponent(jTextFieldLemmingsSaved)
                                    .addComponent(jTextFieldScore, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(jLabelRecords, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jSeparatorRecords))
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPaneLevels)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelLevelStats)
                            .addComponent(jLabelRecords))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jSeparatorLevelStats, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabelNumLemmings)
                                    .addComponent(jTextFieldNumLemmings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabelNumToRescue)
                                    .addComponent(jTextFieldNumToRescue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabelReleaseRate)
                                    .addComponent(jTextFieldReleaseRate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabelTimeLimit)
                                    .addComponent(jTextFieldTimeLimit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jSeparatorRecords, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabelLemmingsSaved)
                                    .addComponent(jTextFieldLemmingsSaved, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabelSkillsUsed)
                                    .addComponent(jTextFieldSkillsUsed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabelTimeElapsed)
                                    .addComponent(jTextFieldTimeElapsed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabelScore)
                                    .addComponent(jTextFieldScore, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparatorSkills, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelNumClimbers)
                            .addComponent(jTextFieldNumClimbers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelNumFloaters)
                            .addComponent(jTextFieldNumFloaters, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelNumBombers)
                            .addComponent(jTextFieldNumBombers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelNumBlockers)
                            .addComponent(jTextFieldNumBlockers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelNumBuilders)
                            .addComponent(jTextFieldNumBuilders, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelNumBashers)
                            .addComponent(jTextFieldNumBashers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelNumMiners)
                            .addComponent(jTextFieldNumMiners, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelNumDiggers)
                            .addComponent(jTextFieldNumDiggers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jSeparator1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonOK)
                    .addComponent(jButtonCancel)
                    .addComponent(jButtonAddExternal))
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOKActionPerformed
        TreePath selPath = jTreeLevels.getSelectionPath();
        Object[] selPathArray = selPath.getPath();
        if (selPathArray.length >= 4) {
            selectedLevel = (LevelItem) ((DefaultMutableTreeNode) selPathArray[3]).getUserObject();
        }
        dispose();
    }//GEN-LAST:event_jButtonOKActionPerformed

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancelActionPerformed
        dispose();
    }//GEN-LAST:event_jButtonCancelActionPerformed

    private void jTreeLevelsValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_jTreeLevelsValueChanged
        TreePath selPath = jTreeLevels.getSelectionPath();
        if (selPath != null && selPath.getPathCount() >= 4) {
            LevelItem lvlItem = (LevelItem) ((DefaultMutableTreeNode) selPath.getPath()[3]).getUserObject();
            LevelPack lvlPack = GameController.getLevelPack(lvlItem.levelPack);
            LevelInfo lvlInfo = lvlPack.getInfo(lvlItem.rating, lvlItem.levelIndex);
            LevelRecord lvlRecord = Core.player.getLevelRecord(lvlPack.getName(), lvlPack.getRatings()[lvlItem.rating], lvlItem.levelIndex);
            int numLemmings = lvlInfo.getNumLemmings();
            int numToRescue = lvlInfo.getNumToRescue();
            int timeLimit = lvlInfo.getTimeLimit();
            jTextFieldNumLemmings.setText(Integer.toString(numLemmings));
            if (GameController.isNoPercentages() || numLemmings > 100) {
                jTextFieldNumToRescue.setText(Integer.toString(numToRescue));
            } else {
                jTextFieldNumToRescue.setText(Integer.toString(numToRescue * 100 / numLemmings) + "%");
            }
            jTextFieldReleaseRate.setText(Integer.toString(lvlInfo.getReleaseRate()));
            if (timeLimit <= 0) {
                jTextFieldTimeLimit.setText("None");
            } else {
                jTextFieldTimeLimit.setText(String.format("%d:%02d", timeLimit / 60, timeLimit % 60));
            }
            jTextFieldNumClimbers.setText(ToolBox.intToString(lvlInfo.getNumClimbers(), true));
            jTextFieldNumFloaters.setText(ToolBox.intToString(lvlInfo.getNumFloaters(), true));
            jTextFieldNumBombers.setText(ToolBox.intToString(lvlInfo.getNumBombers(), true));
            jTextFieldNumBlockers.setText(ToolBox.intToString(lvlInfo.getNumBlockers(), true));
            jTextFieldNumBuilders.setText(ToolBox.intToString(lvlInfo.getNumBuilders(), true));
            jTextFieldNumBashers.setText(ToolBox.intToString(lvlInfo.getNumBashers(), true));
            jTextFieldNumMiners.setText(ToolBox.intToString(lvlInfo.getNumMiners(), true));
            jTextFieldNumDiggers.setText(ToolBox.intToString(lvlInfo.getNumDiggers(), true));
            if (lvlRecord.isCompleted()) {
                int lemmingsSaved = lvlRecord.getLemmingsSaved();
                int timeElapsed = lvlRecord.getTimeElapsed();
                if (GameController.isNoPercentages() || numLemmings > 100) {
                    jTextFieldLemmingsSaved.setText(Integer.toString(lvlRecord.getLemmingsSaved()));
                } else {
                    jTextFieldLemmingsSaved.setText(Integer.toString(lemmingsSaved * 100 / numLemmings) + "%");
                }
                jTextFieldSkillsUsed.setText(Integer.toString(lvlRecord.getSkillsUsed()));
                jTextFieldTimeElapsed.setText(String.format("%d:%02d", timeElapsed / 60, timeElapsed % 60));
                jTextFieldScore.setText(Integer.toString(lvlRecord.getScore()));
            } else {
                jTextFieldLemmingsSaved.setText("");
                jTextFieldSkillsUsed.setText("");
                jTextFieldTimeElapsed.setText("");
                jTextFieldScore.setText("");
            }
            jButtonOK.setEnabled(true);
        } else {
            jTextFieldNumLemmings.setText("");
            jTextFieldNumToRescue.setText("");
            jTextFieldReleaseRate.setText("");
            jTextFieldTimeLimit.setText("");
            jTextFieldNumClimbers.setText("");
            jTextFieldNumFloaters.setText("");
            jTextFieldNumBombers.setText("");
            jTextFieldNumBlockers.setText("");
            jTextFieldNumBuilders.setText("");
            jTextFieldNumBashers.setText("");
            jTextFieldNumMiners.setText("");
            jTextFieldNumDiggers.setText("");
            jTextFieldLemmingsSaved.setText("");
            jTextFieldSkillsUsed.setText("");
            jTextFieldTimeElapsed.setText("");
            jTextFieldScore.setText("");
            jButtonOK.setEnabled(false);
        }
    }//GEN-LAST:event_jTreeLevelsValueChanged

    private void jTreeLevelsMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTreeLevelsMousePressed
        if (evt.getClickCount() == 2) {
            TreePath selPath = jTreeLevels.getPathForLocation(evt.getX(), evt.getY());
            if (selPath != null) {
                Object[] selPathArray = selPath.getPath();
                if (selPathArray.length >= 4) {
                    selectedLevel = (LevelItem) ((DefaultMutableTreeNode) selPathArray[3]).getUserObject();
                    dispose();
                }
            }
        }
    }//GEN-LAST:event_jTreeLevelsMousePressed

    private void jButtonAddExternalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddExternalActionPerformed
        Path[] externLvls = ToolBox.getFileNames(this, lvlPath, Core.LEVEL_EXTENSIONS, true, true);
        if (externLvls != null) {
            if (externLvls.length > 0) {
                lvlPath = externLvls[0].getParent();
            }
            String lastLvlExt = null;
            for (Path externLvl : externLvls) {
                if (Files.isDirectory(externLvl)) {
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(externLvl, "*.{ini,lvl,dat}")) {
                        for (Path lvl : stream) {
                            String lvlExt = Lemmini.addExternalLevel(lvl, false);
                            if (lvlExt != null) {
                                lastLvlExt = lvlExt;
                            }
                        }
                    } catch (IOException ex) {
                    }
                } else {
                    String lvlExt = Lemmini.addExternalLevel(externLvl, false);
                    if (lvlExt != null) {
                        lastLvlExt = lvlExt;
                    }
                }
            }
            if (lastLvlExt != null) {
                refreshLevels();
                Object[] selPathArray = new Object[4];
                switch (lastLvlExt) {
                    case "ini":
                    case "lvl":
                        TreeNode lp = topNode.getFirstChild();
                        TreeNode rating = lp.getChildAt(0);
                        TreeNode level = rating.getChildAt(rating.getChildCount() - 1);
                        selPathArray[0] = topNode;
                        selPathArray[1] = lp;
                        selPathArray[2] = rating;
                        selPathArray[3] = level;
                        break;
                    case "dat":
                        lp = topNode.getFirstChild();
                        rating = lp.getChildAt(lp.getChildCount() - 1);
                        level = rating.getChildAt(0);
                        selPathArray[0] = topNode;
                        selPathArray[1] = lp;
                        selPathArray[2] = rating;
                        selPathArray[3] = level;
                        break;
                    default:
                        break;
                }
                levelModel.reload();
                jTreeLevels.setSelectionPath(new TreePath(selPathArray));
            } else {
                JOptionPane.showMessageDialog(this, "No valid level files were loaded.", "Load Level", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_jButtonAddExternalActionPerformed

    private void jTreeLevelsKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTreeLevelsKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            TreePath selPath = jTreeLevels.getSelectionPath();
            Object[] selPathArray = selPath.getPath();
            if (selPathArray.length >= 4) {
                selectedLevel = (LevelItem) ((DefaultMutableTreeNode) selPathArray[3]).getUserObject();
                dispose();
            } else {
                if (jTreeLevels.isExpanded(selPath)) {
                    jTreeLevels.collapsePath(selPath);
                } else {
                    jTreeLevels.expandPath(selPath);
                }
            }
        }
    }//GEN-LAST:event_jTreeLevelsKeyPressed
    
    /**
     * Returns an int array consisting of the indices of the chosen levelIndex pack,
     * rating, and levelIndex. If no levelIndex was chosen, then null is returned.
     * @return int array or null
     */
    public int[] getSelectedLevel() {
        int[] retArray = null;
        if (selectedLevel != null) {
            retArray = new int[]{selectedLevel.levelPack,
                selectedLevel.rating, selectedLevel.levelIndex};
        }
        return retArray;
    }
    
    private void refreshLevels() {
        topNode.removeAllChildren();
        // read level packs
        for (int i = 0; i < GameController.getLevelPackCount(); i++) {
            LevelPack lp = GameController.getLevelPack(i);
            DefaultMutableTreeNode lpNode = new DefaultMutableTreeNode(lp.getName());
            // read ratings
            String[] ratings = lp.getRatings();
            for (int j = 0; j < ratings.length; j++) {
                DefaultMutableTreeNode ratingNode = new DefaultMutableTreeNode(ratings[j]);
                // read levels
                String[] levels = lp.getLevels(j);
                for (int k = 0; k < levels.length; k++) {
                    if (lp.getAllLevelsUnlocked()
                            || Core.player.isAvailable(lp.getName(), ratings[j], k)) {
                        DefaultMutableTreeNode levelNode = new DefaultMutableTreeNode(
                                new LevelItem(i, j, k, levels[k],
                                Core.player.getLevelRecord(lp.getName(), ratings[j], k).isCompleted()),
                                false);
                        ratingNode.add(levelNode);
                    }
                }
                if (ratingNode.getChildCount() > 0) {
                    lpNode.add(ratingNode);
                }
            }
            if (lpNode.getChildCount() > 0) {
                topNode.add(lpNode);
            }
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAddExternal;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonOK;
    private javax.swing.JLabel jLabelLemmingsSaved;
    private javax.swing.JLabel jLabelLevelStats;
    private javax.swing.JLabel jLabelNumBashers;
    private javax.swing.JLabel jLabelNumBlockers;
    private javax.swing.JLabel jLabelNumBombers;
    private javax.swing.JLabel jLabelNumBuilders;
    private javax.swing.JLabel jLabelNumClimbers;
    private javax.swing.JLabel jLabelNumDiggers;
    private javax.swing.JLabel jLabelNumFloaters;
    private javax.swing.JLabel jLabelNumLemmings;
    private javax.swing.JLabel jLabelNumMiners;
    private javax.swing.JLabel jLabelNumToRescue;
    private javax.swing.JLabel jLabelRecords;
    private javax.swing.JLabel jLabelReleaseRate;
    private javax.swing.JLabel jLabelScore;
    private javax.swing.JLabel jLabelSkillsUsed;
    private javax.swing.JLabel jLabelTimeElapsed;
    private javax.swing.JLabel jLabelTimeLimit;
    private javax.swing.JScrollPane jScrollPaneLevels;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparatorLevelStats;
    private javax.swing.JSeparator jSeparatorRecords;
    private javax.swing.JSeparator jSeparatorSkills;
    private javax.swing.JTextField jTextFieldLemmingsSaved;
    private javax.swing.JTextField jTextFieldNumBashers;
    private javax.swing.JTextField jTextFieldNumBlockers;
    private javax.swing.JTextField jTextFieldNumBombers;
    private javax.swing.JTextField jTextFieldNumBuilders;
    private javax.swing.JTextField jTextFieldNumClimbers;
    private javax.swing.JTextField jTextFieldNumDiggers;
    private javax.swing.JTextField jTextFieldNumFloaters;
    private javax.swing.JTextField jTextFieldNumLemmings;
    private javax.swing.JTextField jTextFieldNumMiners;
    private javax.swing.JTextField jTextFieldNumToRescue;
    private javax.swing.JTextField jTextFieldReleaseRate;
    private javax.swing.JTextField jTextFieldScore;
    private javax.swing.JTextField jTextFieldSkillsUsed;
    private javax.swing.JTextField jTextFieldTimeElapsed;
    private javax.swing.JTextField jTextFieldTimeLimit;
    private javax.swing.JTree jTreeLevels;
    // End of variables declaration//GEN-END:variables
}

class LevelItem {
    
    final int levelPack;
    final int rating;
    final int levelIndex;
    final String levelName;
    final boolean completed;
    
    LevelItem(int lp, int r, int li, String ln, boolean c) {
        levelPack = lp;
        rating = r;
        levelIndex = li;
        levelName = ln;
        completed = c;
    }
    
    @Override
    public String toString() {
        return (levelIndex + 1) + ": " + levelName + (completed ? " (completed)" : "");
    }
}
