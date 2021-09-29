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
    /** source path (WINLEMM) for extraction */
    private static Path sourcePath;
    /** destination path (Lemmini resource) for extraction */
    private static Path destinationPath;
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
                out(crcbuf[0]);
                Path sourceFile = sourcePath.resolve(crcbuf[0]);
                if (!Files.isRegularFile(sourceFile)) {
                    throw new ExtractException(String.format("File %s not found.", sourceFile));
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
                extractLevels(sourcePath.resolve(lvls[0]), tempFolder.resolve(lvls[1]));
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
                sprite.loadPalette(sourcePath.resolve(styles[2]));
                sprite.loadSPR(sourcePath.resolve(styles[0]));
                mask.loadPalette(sourcePath.resolve(styles[2]));
                mask.loadSPR(sourcePath.resolve(styles[1]));
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
                out(object[0]);
                Path dest = tempFolder.resolve(object[3]);
                Files.createDirectories(dest);
                // load palette and sprite
                sprite.loadPalette(sourcePath.resolve(object[1]));
                sprite.loadSPR(sourcePath.resolve(object[0]));
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
                Path sourceFile = sourcePath.resolve(copy[0]);
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
            
            if (doCreatePatches) {
                Path patchINIPath = destinationPath.resolve(Core.PATCH_INI_NAME);
                // this is not needed by Lemmini, but to create the DIF files (and CRCs)
                if (DO_CREATE_CRC) {
                    // create crc.ini
                    out(String.format("%nCreating crc.ini..."));
                    Path crcINIPath = destinationPath.resolve(CRC_INI_NAME);
                    try (Writer fCRCList = Files.newBufferedWriter(crcINIPath, StandardCharsets.UTF_8)) {
                        for (int i = 0; true; i++) {
                            String ppath;
                            ppath = props.get("pcrc_" + i, StringUtils.EMPTY);
                            if (ppath.isEmpty()) {
                                break;
                            }
                            createCRCs(sourcePath, ppath, fCRCList);
                            checkCancel();
                        }
                    } catch (IOException ex) {
                        throw new ExtractException(String.format("Unable to create %s.", crcINIPath));
                    }
                }
                
                // step six: create patches and patch.ini
                Files.createDirectories(patchPath);
                out(String.format("%nCreating patch.ini..."));
                try (Writer fPatchList = Files.newBufferedWriter(patchINIPath, StandardCharsets.UTF_8)) {
                    for (int i = 0; true; i++) {
                        String ppath;
                        ppath = props.get("ppatch_" + i, StringUtils.EMPTY);
                        if (ppath.isEmpty()) {
                            break;
                        }
                        createPatches(referencePath, tempFolder, ppath, fPatchList);
                        checkCancel();
                    }
                } catch (IOException ex) {
                    throw new ExtractException(String.format("Unable to create %s.", patchINIPath));
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
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(destinationPath.resolve(Core.ROOT_ZIP_NAME)))) {
                // extract
                out(String.format("%nAdding files to zip..."));
                for (PatchINIEntry extract : extractList) {
                    String extractPath = extract.getPath();
                    out(extractPath);
                    String fnDecorated = extractPath.replace('/', '@');
                    URL fnc = findFile(patchPath.resolve(fnDecorated));
                    ZipEntry zipEntry = new ZipEntry(extractPath);
                    try {
                        zip.putNextEntry(zipEntry);
                        copyFile(fnc, zip);
                        zip.closeEntry();
                    } catch (Exception ex) {
                        throw new ExtractException(String.format("Unable to add %s to zip file.",
                                patchPath.resolve(FilenameUtils.getName(extractPath))));
                    }
                    try {
                        Files.deleteIfExists(destinationPath.resolve(extractPath));
                    } catch (IOException ex) {
                    }
                    checkCancel();
                }
                // check
                for (PatchINIEntry check : checkList) {
                    String checkPath = check.getPath();
                    out(checkPath);
                    Path source = tempFolder.resolve(checkPath);
                    ZipEntry zipEntry = new ZipEntry(checkPath);
                    try {
                        zip.putNextEntry(zipEntry);
                        copyFile(source, zip);
                        zip.closeEntry();
                    } catch (Exception ex) {
                        throw new ExtractException(String.format("Unable to add %s to zip file.",
                                patchPath.resolve(FilenameUtils.getName(checkPath))));
                    }
                    try {
                        Files.deleteIfExists(destinationPath.resolve(checkPath));
                    } catch (IOException ex) {
                    }
                    checkCancel();
                }
                // patch
                out(String.format("%nPatching files..."));
                for (PatchINIEntry patch : patchList) {
                    String pPath = patch.getPath();
                    out(pPath);
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
                        throw new ExtractException(String.format("Unable to add %s to zip file.",
                                patchPath.resolve(FilenameUtils.getName(pPath))));
                    }
                    try {
                        Files.deleteIfExists(destinationPath.resolve(pPath));
                    } catch (IOException ex) {
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
                out(path);
                Path dest = destinationPath.resolve(path);
                Files.createDirectories(dest);
                checkCancel();
            }
            
            if (deleteOldFiles) {
                // step nine: delete files
                out(String.format("%nDeleting old files..."));
                // extract
                for (PatchINIEntry extract : extractList) {
                    String extractPath = extract.getPath();
                    try {
                        Files.deleteIfExists(destinationPath.resolve(extractPath));
                    } catch (IOException ex) {
                    }
                    checkCancel();
                }
                // check
                for (PatchINIEntry check : checkList) {
                    String checkPath = check.getPath();
                    try {
                        Files.deleteIfExists(destinationPath.resolve(checkPath));
                    } catch (IOException ex) {
                    }
                    checkCancel();
                }
                // patch
                for (PatchINIEntry patch : patchList) {
                    String patchPath = patch.getPath();
                    try {
                        Files.deleteIfExists(destinationPath.resolve(patchPath));
                    } catch (IOException ex) {
                    }
                    checkCancel();
                }
                
                // step ten: delete directories
                out(String.format("%nDeleting old directories..."));
                for (int i = 0; true; i++) {
                    // 0: path
                    String path = props.get("deldir_" + i, StringUtils.EMPTY);
                    if (path.isEmpty()) {
                        break;
                    }
                    out(path);
                    Path dest = destinationPath.resolve(path);
                    if (Files.isDirectory(dest)) {
                        try {
                            Files.deleteIfExists(dest);
                        } catch (IOException ex) {
                        }
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
     * Get source path (WINLEMM) for extraction.
     * @return source path (WINLEMM) for extraction
     */
    public static Path getSourcePath() {
        return sourcePath;
    }
    
    /**
     * Get destination path (Lemmini resource) for extraction.
     * @return destination path (Lemmini resource) for extraction
     */
    public static Path getResourcePath() {
        return destinationPath;
    }
    
    /**
     * Extract all resources and create patch.ini if referencePath is not null
     * @param srcPath WINLEMM directory
     * @param dstPath target (installation) directory. May also be a relative path inside JAR
     * @param refPath the reference path with the original (wanted) files
     * @param pPath the path to store the patch files to
     * @param createPatches create patches if true
     * @param deleteOld delete old files and folders if true
     * @throws ExtractException
     */
    public static void extract(final Path srcPath, final Path dstPath,
            final Path refPath, final Path pPath,
            final boolean createPatches, final boolean deleteOld) throws ExtractException {
        doCreatePatches = createPatches;
        deleteOldFiles = deleteOld;
        sourcePath = srcPath;
        destinationPath = dstPath;
        if (refPath != null) {
            referencePath = refPath;
        }
        patchPath = pPath;
        
        loader = Extract.class.getClassLoader();
        
        FolderFrame fFrame;
        fFrame = new FolderFrame();
        fFrame.setParameters(sourcePath, destinationPath);
        fFrame.setVisible(true);
        fFrame.waitUntilClosed();
        if (!fFrame.getSuccess()) {
            throw new ExtractException("Extraction canceled by user.", true);
        }
        sourcePath = fFrame.getSource();
        destinationPath = fFrame.getTarget();
        
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
     * @param r name of root folder (source of LVL files)
     * @param dest destination folder for extraction (resource folder)
     * @throws ExtractException
     */
    private static void extractLevels(final Path r, final Path dest) throws ExtractException {
        // first extract the levels
        try {
            Files.createDirectories(dest);
        } catch (IOException ex) {
        }
        
        try (DirectoryStream<Path> levels = Files.newDirectoryStream(r, "*.LVL")) {
            for (Path level : levels) {
                Path fIn = r.resolve(level.getFileName());
                String fOutStr = level.getFileName().toString().toLowerCase(Locale.ROOT);
                Path fOut = dest.resolve(FilenameUtils.removeExtension(fOutStr) + ".ini");
                createdFiles.add(fOut);
                out(level.getFileName().toString());
                ExtractLevel.convertLevel(fIn, fOut, false, true);
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
        
        String out;
        try {
            Files.createDirectories(patchPath);
        } catch (IOException ex) {
        }
        
        Diff.setParameters(512, 4);
        String subDir = addSeparator(sDir);
        String subDirDecorated = subDir.replace('/', '@');
        
        try (DirectoryStream<Path> files = Files.newDirectoryStream(patchSourcePath, new PatchFilter())) {
            outerLoop:
                for (Path file : files) {
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
                    
                    Path fnIn = patchSourcePath.resolve(fileName);
                    Path fnOut = destPath.resolve(fileName);
                    pos = fileName.lastIndexOf('.');
                    if (pos == StringUtils.INDEX_NOT_FOUND) {
                        pos = fileName.length();
                    }
                    Path fnPatch = patchPath.resolve(subDirDecorated + fileName.substring(0, pos).toLowerCase(Locale.ROOT) + ".dif");
                    out(fnIn.toString());
                    // read sourceFile file
                    byte[] src = readFile(fnIn);
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
                        out = String.format(Locale.ROOT, "%s%s, %#010x", subDir, fileName, crc.getValue());
                        out = ToolBox.addBackslashes(out, false);
                        fPatchList.write(String.format(Locale.ROOT, "extract_%d = %s\r\n", extractNo++, out));
                        // copy missing files to patch dir
                        copyFile(fnIn, patchPath.resolve(subDirDecorated + fileName));
                        continue;
                    }
                    // create diff
                    byte[] patch = Diff.diffBuffers(trg, src);
                    int crc = Diff.targetCRC; // crc of target buffer
                    out = String.format(Locale.ROOT, "%s%s, %#010x", subDir, fileName, crc);
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
        } catch (Exception ex) {
            throw new ExtractException(ex.getMessage());
        }
    }
    
    /**
     * Create CRCs for resources (development).
     * @param rPath The root path with the files to create CRCs for
     * @param sDir SubDir to create patches for
     * @param fCRCList Writer to create crc.ini
     * @throws ExtractException
     */
    private static void createCRCs(final Path rPath, final String sDir, final Writer fCRCList) throws ExtractException {
        Path rootPath = rPath.resolve(sDir);
        String out;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(rootPath, new PatchFilter())) {
            outerLoop:
                for (Path file : files) {
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
                    Path fnIn = rootPath.resolve(fileName);
                    out(fnIn.toString());
                    // read sourceFile file
                    byte[] src = readFile(fnIn);
                    Adler32 crc32 = new Adler32();
                    crc32.update(src);
                    out = String.format(Locale.ROOT, "%s%s, %d, %#010x", addSeparator(sDir), fileName, src.length, crc32.getValue());
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
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
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

