package lemmini.extract;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.Adler32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.JOptionPane;
import lemmini.game.Core;
import lemmini.tools.CaseInsensitiveFileTree;
import lemmini.tools.Props;
import lemmini.tools.ToolBox;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

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

/**
 * Extraction of resources.
 *
 * @author Volker Oth
 */
public class Extract implements Runnable {
    
    /** file name of extraction configuration */
    private static final String INI_NAME = "extract.ini";
    /** file name of resource CRCs (WINLEMM) */
    private static final String CRC_INI_NAME = "crc.ini";
    /** allows to use this module for creation of the CRC.ini */
    private static final boolean DO_CREATE_CRC = false;
    
    private static boolean doCreatePatches = false;
    private static boolean deleteOldFiles = false;
    /** index for files to be checked - static since multiple runs are possible */
    private static int checkNo = 0;
    /** index for CRCs - static since multiple runs are possible */
    private static int crcNo = 0;
    /** index for files to be extracted - static since multiple runs are possible */
    private static int extractNo = 0;
    /** index for files to be patched - static since multiple runs are possible */
    private static int patchNo = 0;
    /** array of extensions to be ignored - read from INI */
    private static String[] ignoreExt = ArrayUtils.EMPTY_STRING_ARRAY;
    /** output dialog */
    private static OutputFrame outputFrame;
    /** monitor the files created without erasing the target dir */
    private static Set<Path> createdFiles;
    /** tree of source path (WINLEMM) for extraction */
    private static CaseInsensitiveFileTree sourceTree;
    /** tree of destination path (Lemmini resource) for extraction */
    private static CaseInsensitiveFileTree destinationTree;
    /** reference path for creation of DIF files */
    private static Path referencePath;
    /** path of the DIF files */
    private static Path patchPath;
    /** exception caught in the thread */
    private static ExtractException threadException = null;
    /** static self reference to access thread from outside */
    private static Thread thisThread;
    /** reference to class loader */
    private static ClassLoader loader;
    
    /**
     * Display an exception message box.
     * @param ex Exception
     */
    private static void showException(final Throwable ex) {
        String m = "<html>" + ex.getClass().getName() + "<p>";
        if (ex.getMessage() != null) {
            m += ex.getMessage() + "<p>";
        }
        StackTraceElement[] ste = ex.getStackTrace();
        for (StackTraceElement ste1 : ste) {
            m += ste1.toString() + "<p>";
        }
        m += "</html>";
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, m, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     *
     * Extraction running in a Thread.
     */
    @Override
    public void run() {
        createdFiles = new HashSet<>(); // to monitor the files created without erasing the target dir
        Path tempFolder = null;
        
        try {
            // read INI file
            Props props = new Props();
            URL fn = findFile(Paths.get(INI_NAME));
            if (fn == null || !props.load(fn)) {
                throw new ExtractException("File " + INI_NAME + " not found or error while reading.");
            }
            
            ignoreExt = props.getArray("ignore_ext", ArrayUtils.EMPTY_STRING_ARRAY);
            
            // prolog_ check CRC
            out(String.format("%nValidating WINLEMM..."));
            URL fncrc = findFile(Paths.get(CRC_INI_NAME));
            Props cprops = new Props();
            if (fncrc == null || !cprops.load(fncrc)) {
                throw new ExtractException("File " + CRC_INI_NAME + " not found or error while reading.");
            }
            for (int i = 0; true; i++) {
                // 0: name, 1: size, 2: crc
                String[] crcbuf = cprops.getArray("crc_" + i, null);
                if (crcbuf == null) {
                    break;
                }
                Path sourceFile = sourceTree.getPath(crcbuf[0]);
                out(sourceTree.getRoot().relativize(sourceFile).toString());
                if (!Files.isRegularFile(sourceFile)) {
                    throw new ExtractException(String.format("File %s not found.", sourceTree.getRoot().relativize(sourceFile)));
                }
                long srcLen = Files.size(sourceFile);
                long reqLen = ToolBox.parseLong(crcbuf[1]);
                if (srcLen != reqLen) {
                    throw new ExtractException(String.format(
                            "CRC error for file %s. (Size is %,d, expected %,d.)%n",
                            sourceFile, srcLen, reqLen));
                }
                byte[] src = readFile(sourceFile);
                Adler32 crc32 = new Adler32();
                crc32.update(src);
                long srcCrc = crc32.getValue();
                long reqCrc = ToolBox.parseLong(crcbuf[2]);
                if (srcCrc != reqCrc) {
                    throw new ExtractException(String.format(
                            "CRC error for file %s. (CRC is %#010x, expected %#010x.)%n",
                            sourceFile, srcCrc, reqCrc));
                }
                checkCancel();
            }
            
            // create the temp folder
            tempFolder = Files.createTempDirectory("lemmini-");
            
            // step one: convert the levels
            out(String.format("%nConverting levels..."));
            for (int i = 0; true; i++) {
                // 0: srcPath, 1: destPath
                String[] lvls = props.getArray("level_" + i, null);
                if (lvls == null) {
                    break;
                }
                extractLevels(sourceTree, lvls[0], tempFolder.resolve(lvls[1]));
                checkCancel();
            }
            
            // step two: convert the styles
            out(String.format("%nConverting styles..."));
            ExtractSPR sprite = new ExtractSPR();
            ExtractSPR mask = new ExtractSPR();
            for (int i = 0; true; i++) {
                // 0: SPR, 1: masks, 2: PAL, 3: path, 4: fname
                String[] styles = props.getArray("style_" + i, null);
                if (styles == null) {
                    break;
                }
                out(styles[4]);
                Path dest = tempFolder.resolve(styles[3]);
                Files.createDirectories(dest);
                // load palette and sprite
                sprite.loadPalette(sourceTree.getPath(styles[2]));
                sprite.loadSPR(sourceTree.getPath(styles[0]));
                mask.loadPalette(sourceTree.getPath(styles[2]));
                mask.loadSPR(sourceTree.getPath(styles[1]));
                mask.createMasks();
                List<Path> files = sprite.saveAll(dest.resolve(styles[4]));
                List<Path> maskFiles = mask.saveAll(dest.resolve(styles[4] + "m"));
                createdFiles.addAll(files);
                createdFiles.addAll(maskFiles);
                checkCancel();
            }
            
            // step three: convert the objects
            out(String.format("%nConverting objects..."));
            for (int i = 0; true; i++) {
                // 0:SPR, 1:PAL, 2:resource, 3:path
                String[] object = props.getArray("objects_" + i, null);
                if (object == null) {
                    break;
                }
                Path src = sourceTree.getPath(object[0]);
                out(sourceTree.getRoot().relativize(src).toString());
                Path dest = tempFolder.resolve(object[3]);
                Files.createDirectories(dest);
                // load palette and sprite
                sprite.loadPalette(sourceTree.getPath(object[1]));
                sprite.loadSPR(src);
                for (int j = 0; true; j++) {
                    // 0: idx, 1: frames, 2: name
                    String[] member = props.getArray(object[2] + "_" + j, null);
                    if (member == null) {
                        break;
                    }
                    // save object
                    Path dest2 = dest.resolve(member[2]);
                    createdFiles.add(dest2);
                    sprite.saveAnim(dest2, ToolBox.parseInt(member[0]), ToolBox.parseInt(member[1]));
                    checkCancel();
                }
            }
            
            //if (false) { // debug only
            
            // step four: copy stuff
            out(String.format("%nCopying files..."));
            for (int i = 0; true; i++) {
                // 0: srcName, 1: destName
                String[] copy = props.getArray("copy_" + i, null);
                if (copy == null) {
                    break;
                }
                Path sourceFile = sourceTree.getPath(copy[0]);
                Path destinationFile = tempFolder.resolve(copy[1]);
                if (!Files.isRegularFile(sourceFile)) {
                    throw new ExtractException(String.format("File %s not found.", sourceFile));
                }
                Files.createDirectories(destinationFile.getParent());
                try {
                    copyFile(sourceFile, destinationFile);
                    createdFiles.add(destinationFile);
                } catch (Exception ex) {
                    throw new ExtractException(String.format("Unable to copy %s to %s.", sourceFile, destinationFile));
                }
                checkCancel();
            }
            
            // step five: clone files inside destination dir
            out(String.format("%nCloning files..."));
            for (int i = 0; true; i++) {
                // 0: srcName, 1: destName
                String[] clone = props.getArray("clone_" + i, null);
                if (clone == null) {
                    break;
                }
                Path sourceFile = tempFolder.resolve(clone[0]);
                Path destinationFile = tempFolder.resolve(clone[1]);
                Files.createDirectories(destinationFile.getParent());
                try {
                    copyFile(sourceFile, destinationFile);
                    createdFiles.add(destinationFile);
                } catch (Exception ex) {
                    throw new ExtractException(String.format("Unable to clone %s to %s.", sourceFile, destinationFile));
                }
                checkCancel();
            }
            
            destinationTree.createDirectories("/");
            
            if (doCreatePatches) {
                // this is not needed by Lemmini, but to create the DIF files (and CRCs)
                if (DO_CREATE_CRC) {
                    // create crc.ini
                    out(String.format("%nCreating crc.ini..."));
                    try (Writer fCRCList = destinationTree.newBufferedWriter(CRC_INI_NAME)) {
                        for (int i = 0; true; i++) {
                            String ppath = props.get("pcrc_" + i, StringUtils.EMPTY);
                            if (ppath.isEmpty()) {
                                break;
                            }
                            createCRCs(sourceTree, ppath, fCRCList);
                            checkCancel();
                        }
                    } catch (IOException ex) {
                        throw new ExtractException(String.format(
                                "Unable to create %s.", destinationTree.getRoot().resolve(CRC_INI_NAME)));
                    }
                }
                
                // step six: create patches and patch.ini
                Files.createDirectories(patchPath);
                out(String.format("%nCreating patch.ini..."));
                try (Writer fPatchList = destinationTree.newBufferedWriter(Core.PATCH_INI_NAME)) {
                    for (int i = 0; true; i++) {
                        String ppath = props.get("ppatch_" + i, StringUtils.EMPTY);
                        if (ppath.isEmpty()) {
                            break;
                        }
                        createPatches(referencePath, tempFolder, ppath, fPatchList);
                        checkCancel();
                    }
                } catch (IOException ex) {
                    throw new ExtractException(String.format(
                            "Unable to create %s.", destinationTree.getRoot().resolve(Core.PATCH_INI_NAME)));
                }
            }
            
            // read patch.ini file
            Props pprops = new Props();
            URL fnp = findFile(Paths.get(Core.PATCH_INI_NAME)); // if it's in the JAR or local directory
            if (!pprops.load(fnp)) {
                throw new ExtractException("File " + Core.PATCH_INI_NAME + " not found or error while reading.");
            }
            List<PatchINIEntry> extractList = new ArrayList<>(1024);
            List<PatchINIEntry> checkList = new ArrayList<>(2048);
            List<PatchINIEntry> patchList = new ArrayList<>(512);
            for (int i = 0; true; i++) {
                // 0: name, 1: crc
                String[] copy = pprops.getArray("extract_" + i, null);
                if (copy == null) {
                    break;
                }
                extractList.add(new PatchINIEntry(copy[0], ToolBox.parseInt(copy[1])));
            }
            for (int i = 0; true; i++) {
                // 0: name, 1: crc
                String[] check = pprops.getArray("check_" + i, null);
                if (check == null) {
                    break;
                }
                checkList.add(new PatchINIEntry(check[0], ToolBox.parseInt(check[1])));
            }
            for (int i = 0; true; i++) {
                // 0: name, 1: crc
                String[] patch = pprops.getArray("patch_" + i, null);
                if (patch == null) {
                    break;
                }
                patchList.add(new PatchINIEntry(patch[0], ToolBox.parseInt(patch[1])));
            }
            
            // step seven: use patch.ini to extract/patch all files
            destinationTree.delete(Core.ROOT_ZIP_NAME);
            try (ZipOutputStream zip = new ZipOutputStream(destinationTree.newOutputStream(Core.ROOT_ZIP_NAME))) {
                // extract
                out(String.format("%nAdding files to root.lzp..."));
                for (PatchINIEntry extract : extractList) {
                    String extractPath = extract.getPath();
                    out(Paths.get(extractPath).toString());
                    String fnDecorated = extractPath.replace('/', '@');
                    URL fnc = findFile(patchPath.resolve(fnDecorated));
                    ZipEntry zipEntry = new ZipEntry(extractPath);
                    try {
                        zip.putNextEntry(zipEntry);
                        copyFile(fnc, zip);
                        zip.closeEntry();
                    } catch (Exception ex) {
                        throw new ExtractException(String.format("Unable to add %s to root.lzp.",
                                patchPath.resolve(ToolBox.getFileName(extractPath))));
                    }
                    if (deleteOldFiles) {
                        try {
                            destinationTree.delete(extractPath);
                        } catch (IOException ex) {
                        }
                    }
                    checkCancel();
                }
                // check
                for (PatchINIEntry check : checkList) {
                    String checkPath = check.getPath();
                    out(Paths.get(checkPath).toString());
                    Path source = tempFolder.resolve(checkPath);
                    ZipEntry zipEntry = new ZipEntry(checkPath);
                    try {
                        zip.putNextEntry(zipEntry);
                        copyFile(source, zip);
                        zip.closeEntry();
                    } catch (Exception ex) {
                        throw new ExtractException(String.format("Unable to add %s to root.lzp.",
                                patchPath.resolve(ToolBox.getFileName(checkPath))));
                    }
                    if (deleteOldFiles) {
                        try {
                            destinationTree.delete(checkPath);
                        } catch (IOException ex) {
                        }
                    }
                    checkCancel();
                }
                // patch
                out(String.format("%nPatching files..."));
                for (PatchINIEntry patch : patchList) {
                    String pPath = patch.getPath();
                    out(Paths.get(pPath).toString());
                    String fnDif = pPath.replace('/', '@');
                    int pos = fnDif.lastIndexOf('.');
                    if (pos == StringUtils.INDEX_NOT_FOUND) {
                        pos = fnDif.length();
                    }
                    fnDif = fnDif.substring(0, pos) + ".dif";
                    URL urlDif = findFile(patchPath.resolve(fnDif));
                    Path source = tempFolder.resolve(pPath);
                    ZipEntry zipEntry = new ZipEntry(pPath);
                    if (urlDif == null) {
                        throw new ExtractException(String.format("Unable to patch file %s.%n", pPath));
                    }
                    byte[] dif = readFile(urlDif);
                    byte[] src = readFile(source);
                    try {
                        byte[] trg = Diff.patchBuffers(src, dif);
                        // write to zip file
                        zip.putNextEntry(zipEntry);
                        zip.write(trg);
                        zip.closeEntry();
                    } catch (DiffException ex) {
                        throw new ExtractException(String.format("Unable to patch file %s.%n%s",
                                pPath, ex.getMessage()));
                    } catch (IOException ex) {
                        throw new ExtractException(String.format("Unable to add %s to root.lzp.",
                                patchPath.resolve(ToolBox.getFileName(pPath))));
                    }
                    if (deleteOldFiles) {
                        try {
                            destinationTree.delete(pPath);
                        } catch (IOException ex) {
                        }
                    }
                    checkCancel();
                }
                
                ZipEntry zipEntry = new ZipEntry("revision.ini");
                zip.putNextEntry(zipEntry);
                Writer w = new BufferedWriter(new OutputStreamWriter(zip, StandardCharsets.UTF_8));
                w.write("revision = " + Core.RES_REVISION + "\r\n");
                w.flush();
                zip.closeEntry();
            }
            
            // step eight: create directories
            out(String.format("%nCreating directories..."));
            for (int i = 0; true; i++) {
                // 0: path
                String path = props.get("mkdir_" + i, StringUtils.EMPTY);
                if (path.isEmpty()) {
                    break;
                }
                out(destinationTree.getRoot().relativize(destinationTree.getPath(path)).toString());
                destinationTree.createDirectories(path);
                checkCancel();
            }
            
            if (deleteOldFiles) {
                // step nine: delete directories
                out(String.format("%nDeleting old directories..."));
                for (int i = 0; true; i++) {
                    // 0: path
                    String path = props.get("deldir_" + i, StringUtils.EMPTY);
                    if (path.isEmpty()) {
                        break;
                    }
                    out(destinationTree.getRoot().relativize(destinationTree.getPath(path)).toString());
                    try {
                        destinationTree.deleteIfEmpty(path);
                    } catch (IOException ex) {
                    }
                    checkCancel();
                }
            }
            //} // debug only
            
            // finished
            out(String.format("%nSuccessfully finished!"));
        } catch (ExtractException ex) {
            threadException = ex;
            out(ex.getMessage());
        } catch (Exception ex) {
            showException(ex);
            System.exit(1);
        } finally {
            if (tempFolder != null) {
                try {
                    ToolBox.deleteFileTree(tempFolder);
                } catch (IOException ex) {
                }
            }
        }
        outputFrame.enableOK();
    }
    
    /**
     * Get tree of source path (WINLEMM) for extraction.
     * @return tree of source path (WINLEMM) for extraction
     */
    public static CaseInsensitiveFileTree getSourceTree() {
        return sourceTree;
    }
    
    /**
     * Get tree of destination path (Lemmini resource) for extraction.
     * @return tree of destination path (Lemmini resource) for extraction
     */
    public static CaseInsensitiveFileTree getResourceTree() {
        return destinationTree;
    }
    
    /**
     * Extract all resources and create patch.ini if referencePath is not null
     * @param srcPath WINLEMM directory
     * @param dstTree tree of target (installation) directory.
     * @param refPath the reference path with the original (wanted) files
     * @param pPath the path to store the patch files to
     * @param createPatches create patches if true
     * @param deleteOld delete old files and folders if true
     * @throws ExtractException
     */
    public static void extract(final Path srcPath, final CaseInsensitiveFileTree dstTree,
            final Path refPath, final Path pPath,
            final boolean createPatches, final boolean deleteOld) throws ExtractException {
        doCreatePatches = createPatches;
        deleteOldFiles = deleteOld;
        Path sourcePath = srcPath;
        destinationTree = dstTree;
        referencePath = refPath;
        patchPath = pPath;
        
        loader = Extract.class.getClassLoader();
        
        FolderFrame fFrame;
        fFrame = new FolderFrame();
        fFrame.setParameters(sourcePath, destinationTree.getRoot());
        fFrame.setVisible(true);
        fFrame.waitUntilClosed();
        if (!fFrame.getSuccess()) {
            throw new ExtractException("Extraction canceled by user.", true);
        }
        sourcePath = fFrame.getSource();
        try {
            sourceTree = new CaseInsensitiveFileTree(sourcePath);
        } catch (IOException ex) {
            throw new ExtractException(String.format("Unable to read from %s.", sourcePath));
        }
        Path destinationPath = fFrame.getDestination();
        if (!destinationPath.equals(destinationTree.getRoot())) {
            try {
                destinationTree = new CaseInsensitiveFileTree(destinationPath);
            } catch (IOException ex) {
                throw new ExtractException(String.format("Unable to read from %s.", destinationPath));
            }
        }
        
        // open output dialog
        outputFrame = new OutputFrame();
        
        // start thread
        threadException = null;
        thisThread = new Thread(new Extract());
        thisThread.start();
        
        outputFrame.setVisible(true);
        outputFrame.waitUntilClosed();
        try {
            thisThread.join();
        } catch (InterruptedException ex) {
        }
        if (threadException != null) {
            throw threadException;
        }
    }
    
    /**
     * Extract the level INI files from LVL files
     * @param tree source tree
     * @param src name of folder with LVL files
     * @param dest destination folder for extraction (resource folder)
     * @throws ExtractException
     */
    private static void extractLevels(final CaseInsensitiveFileTree tree, final String src, final Path dest) throws ExtractException {
        // first extract the levels
        try {
            Files.createDirectories(dest);
        } catch (IOException ex) {
        }
        
        try {
            for (Path level : tree.getAllPathsRegex(ToolBox.literalToRegex(src) + "[^/]+\\.lvl")) {
                String fOutStr = level.getFileName().toString().toLowerCase(Locale.ROOT);
                Path fOut = dest.resolve(FilenameUtils.removeExtension(fOutStr) + ".ini");
                createdFiles.add(fOut);
                out(tree.getRoot().relativize(level).toString());
                try (Writer w = Files.newBufferedWriter(fOut)) {
                    ExtractLevel.convertLevel(level, w, false, true);
                }
            }
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (msg != null && !msg.isEmpty()) {
                out(msg);
            } else {
                out(ex.toString());
            }
            throw new ExtractException(msg);
        }
    }
    
    /**
     * Create the DIF files from reference files and the extracted files (development).
     * @param sPath The path with the original (wanted) files
     * @param dPath The path with the differing (to be patched) files
     * @param subDir SubDir to create patches for
     * @param fPatchList Writer to create patch.ini
     * @throws ExtractException
     */
    private static void createPatches(final Path sPath, final Path dPath, final String sDir, final Writer fPatchList) throws ExtractException {
        // create missing directories
        Path patchSourcePath = sPath.resolve(sDir);
        
        Path destPath = dPath.resolve(sDir);
        try {
            Files.createDirectories(destPath);
        } catch (IOException ex) {
        }
        
        Diff.setParameters(512, 4);
        String subDir = addSeparator(sDir);
        String subDirDecorated = subDir.replace('/', '@');
        
        try (DirectoryStream<Path> files = Files.newDirectoryStream(patchSourcePath, Files::isRegularFile)) {
            for (Path file : files) {
                String fileName = file.getFileName().toString();
                // check extension
                if (FilenameUtils.isExtension(file.toString().toLowerCase(Locale.ROOT), ignoreExt)) {
                    continue;
                }
                
                Path fnOut = destPath.resolve(fileName);
                Path fnPatch = patchPath.resolve(subDirDecorated
                        + FilenameUtils.removeExtension((file.getFileName().toString().toLowerCase(Locale.ROOT))) + ".dif");
                out(sPath.relativize(file).toString());
                // read sourceFile file
                byte[] src = readFile(file);
                byte[] trg = null;
                // read target file
                boolean fileExists;
                fileExists = createdFiles.contains(fnOut);
                if (fileExists) {
                    try {
                        trg = readFile(fnOut);
                    } catch (ExtractException ex) {
                        fileExists = false;
                    }
                }
                if (!fileExists) {
                    // mark missing files: needs to be extracted from JAR
                    Adler32 crc = new Adler32();
                    crc.update(src);
                    String out = String.format(Locale.ROOT, "%s%s, %#010x", subDir, fileName, crc.getValue());
                    out = ToolBox.addBackslashes(out, false);
                    fPatchList.write(String.format(Locale.ROOT, "extract_%d = %s\r\n", extractNo++, out));
                    // copy missing files to patch dir
                    copyFile(file, patchPath.resolve(subDirDecorated + fileName));
                } else {
                    // create diff
                    byte[] patch = Diff.diffBuffers(trg, src);
                    int crc = Diff.targetCRC; // crc of target buffer
                    String out = String.format(Locale.ROOT, "%s%s, %#010x", subDir, fileName, crc);
                    out = ToolBox.addBackslashes(out, false);
                    if (patch == null) {
                        //out("sourceFile and trg are identical");
                        fPatchList.write(String.format(Locale.ROOT, "check_%d = %s\r\n", checkNo++, out));
                    } else {
                        // apply patch to test whether it's OK
                        Diff.patchBuffers(trg, patch);
                        // write patch file
                        writeFile(fnPatch, patch);
                        fPatchList.write(String.format(Locale.ROOT, "patch_%d = %s\r\n", patchNo++, out));
                    }
                }
            }
        } catch (Exception ex) {
            throw new ExtractException(ex.getMessage());
        }
    }
    
    /**
     * Create CRCs for resources (development).
     * @param tree The tree with the files to create CRCs for
     * @param sDir SubDir to create patches for
     * @param fCRCList Writer to create crc.ini
     * @throws ExtractException
     */
    private static void createCRCs(final CaseInsensitiveFileTree tree, final String sDir,
            final Writer fCRCList) throws ExtractException {
        try {
            outerLoop:
            for (Path file : tree.getAllPathsRegex(ToolBox.literalToRegex(sDir) + "[^/]+")) {
                int pos;
                String fileName = file.getFileName().toString();
                // check extension
                pos = fileName.lastIndexOf('.');
                if (pos != StringUtils.INDEX_NOT_FOUND) {
                    String ext = fileName.substring(pos + 1);
                    for (String ignoreExt1 : ignoreExt) {
                        if (ignoreExt1.equalsIgnoreCase(ext)) {
                            continue outerLoop;
                        }
                    }
                }
                out(tree.getRoot().relativize(file).toString());
                // read sourceFile file
                byte[] src = readFile(file);
                Adler32 crc32 = new Adler32();
                crc32.update(src);
                String out = String.format(Locale.ROOT, "%s%s, %d, %#010x", addSeparator(sDir), fileName, src.length, crc32.getValue());
                out = out.toLowerCase(Locale.ROOT);
                out = ToolBox.addBackslashes(out, false);
                fCRCList.write(String.format(Locale.ROOT, "crc_%d = %s\r\n", crcNo++, out));
            }
        } catch (Exception ex) {
            throw new ExtractException(ex.getMessage());
        }
    }
    
    
    /**
     * Add separator "/" to path name (if there isn't one yet)
     * @param fName path name with or without separator
     * @return path name with separator
     */
    private static String addSeparator(final String fName) {
        int pos = fName.lastIndexOf(File.separator);
        if (pos != fName.length() - 1) {
            pos = fName.lastIndexOf("/");
        }
        if (pos != fName.length() - 1) {
            return fName + "/";
        } else {
            return fName;
        }
    }
    
    /**
     * Copy a file.
     * @param source URL of source file
     * @param destination output stream to copy to
     * @throws IOException
     */
    private static void copyFile(final URL source, final OutputStream destination) throws IOException {
        try (InputStream fSrc = source.openStream()) {
            IOUtils.copy(fSrc, destination);
        }
    }
    
    /**
     * Copy a file.
     * @param source URL of source file
     * @param destination full destination file name including path
     * @throws IOException
     */
    private static void copyFile(final URL source, final Path destination) throws IOException {
        try (InputStream fSrc = new BufferedInputStream(source.openStream())) {
            Files.copy(fSrc, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    /**
     * Copy a file.
     * @param source full source file name including path
     * @param destination full destination file name including path
     * @throws IOException
     */
    private static void copyFile(final Path source, final OutputStream destination) throws IOException {
        try (InputStream fSrc = Files.newInputStream(source)) {
            IOUtils.copy(fSrc, destination);
        }
    }
    
    /**
     * Copy a file.
     * @param source full source file name including path
     * @param destination full destination file name including path
     * @throws IOException
     */
    private static void copyFile(final Path source, final Path destination) throws IOException {
        try {
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            try (InputStream fSrc = Files.newInputStream(source);
                    OutputStream fDest = Files.newOutputStream(destination)) {
                IOUtils.copy(fSrc, fDest);
            }
        }
    }
    
    /**
     * Read file into an array of bytes.
     * @param fname file name
     * @return array of bytes
     * @throws ExtractException
     */
    private static byte[] readFile(final Path fname) throws ExtractException {
        try {
            return Files.readAllBytes(fname);
        } catch (FileNotFoundException ex) {
            throw new ExtractException(String.format("File %s not found.", fname));
        } catch (IOException ex) {
            throw new ExtractException(String.format("IO exception while reading file %s.", fname));
        }
    }
    
    /**
     * Read file into an array of bytes.
     * @param fname file name as URL
     * @return array of byte
     * @throws ExtractException
     */
    private static byte[] readFile(final URL fname) throws ExtractException {
        try (InputStream f = fname.openStream()) {
            return IOUtils.toByteArray(f);
        } catch (FileNotFoundException ex) {
            throw new ExtractException(String.format("File %s not found.", fname));
        } catch (IOException ex) {
            throw new ExtractException(String.format("IO exception while reading file %s.", fname));
        }
    }
    
    /**
     * Write array of bytes to file.
     * @param fname file name
     * @param buf array of bytes
     * @throws ExtractException
     */
    private static void writeFile(final Path fname, final byte[] buf) throws ExtractException {
        try {
            Files.write(fname, buf);
        } catch (IOException ex) {
            throw new ExtractException(String.format("IO exception while writing file %s.", fname));
        }
    }
    
    /**
     * Find a file.
     * @param fname File name (without absolute path)
     * @return URL of file
     */
    public static URL findFile(final Path fname) {
        URL retval = loader.getResource(fname.toString().replace(fname.getFileSystem().getSeparator(), "/"));
        try {
            if (retval == null) {
                retval = fname.toUri().toURL();
            }
            return retval;
        } catch (MalformedURLException ex) {
        }
        return null;
    }
    
    /**
     * Print string to output dialog.
     * @param s string to print
     */
    private static void out(final String s) {
        // System.out.println(s);
        if (outputFrame != null) {
            outputFrame.print(String.format("%s%n", s));
        }
    }
    
    /**
     * Return cancel state of output dialog
     * @throws ExtractException
     */
    private static void checkCancel() throws ExtractException {
        if (outputFrame.isCanceled()) {
            throw new ExtractException("Extraction canceled by user.", true);
        }
    }
}

/**
 * Filter for patch files.
 * @author Ryan Sakowski
 */
class PatchFilter implements DirectoryStream.Filter<Path> {
    
    @Override
    public boolean accept(Path entry) throws IOException {
        return Files.isRegularFile(entry);
    }
}

class PatchINIEntry {
    
    private final String path;
    private final int checksum;
    
    PatchINIEntry(String path, int checksum) {
        this.path = path;
        this.checksum = checksum;
    }
    
    String getPath() {
        return path;
    }
    
    int getChecksum() {
        return checksum;
    }
}

