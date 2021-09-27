package lemmini.extract;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.Adler32;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import lemmini.tools.Props;

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
public class Extract extends Thread {

    /** file name of extraction configuration */
    private static final String INI_NAME = "extract.ini";
    /** file name of patching configuration */
    private static final String PATCH_INI_NAME = "patch.ini";
    /** file name of resource CRCs (WINLEMM) */
    private static final String CRC_INI_NAME = "crc.ini";
    /** allows to use this module for creation of the CRC.ini */
    private static final boolean DO_CREATE_CRC = false;
    
    private static boolean doCreatePatches = false;
    /** index for files to be checked - static since multiple runs are possible */
    private static int checkNo = 0;
    /** index for CRCs - static since multiple runs are possible */
    private static int crcNo = 0;
    /** index for files to be extracted - static since multiple runs are possible */
    private static int extractNo = 0;
    /** index for files to be patched - static since multiple runs are possible */
    private static int patchNo = 0;
    /** array of extensions to be ignored - read from INI */
    private static String[] ignoreExt = {};
    /** output dialog */
    private static OutputDialog outputDiag;
    /** monitor the files created without erasing the target dir */
    private static Map<String,Object> createdFiles;
    /** source path (WINLEMM) for extraction */
    private static String sourcePath;
    /** destination path (Lemmini resource) for extraction */
    private static String destinationPath;
    /** reference path for creation of DIF files */
    private static String referencePath;
    /** path of the DIF files */
    private static String patchPath;
    /** path of the CRC INI (without the file name) */
    private static String crcPath;
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
     * @see java.lang.Thread#run()
     *
     * Extraction running in a Thread.
     */
    @Override
    public void run() {
        createdFiles = new HashMap<>(); // to monitor the files created without erasing the target dir

        try {
            // read INI file
            Props props = new Props();
            URL fn = findFile(INI_NAME);
            if (fn == null || !props.load(fn)) {
                throw new ExtractException("File " + INI_NAME + " not found or error while reading.");
            }

            ignoreExt = props.getArray("ignore_ext", ignoreExt);

            // prolog_ check CRC
            out(String.format("%nValidating WINLEMM"));
            URL fncrc = findFile(CRC_INI_NAME);
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
                long len = Files.size(Paths.get(sourcePath, crcbuf[0]));
                if (len != Long.parseLong(crcbuf[1])) {
                    throw new ExtractException(String.format("CRC error for file %s%s.%n", sourcePath, crcbuf[0]));
                }
                byte[] src = readFile(sourcePath + crcbuf[0]);
                Adler32 crc32 = new Adler32();
                crc32.update(src);
                if (Long.toHexString(crc32.getValue()).compareToIgnoreCase(crcbuf[2].substring(2)) != 0) {
                    throw new ExtractException(String.format("CRC error for file %s%s.%n", sourcePath, crcbuf[0]));
                }
                checkCancel();
            }

            // step one: extract the levels
            out(String.format("%nExtracting levels"));
            for (int i = 0; true; i++) {
                // 0: srcPath, 1: destPath
                String[] lvls = props.getArray("level_" + i, null);
                if (lvls == null) {
                    break;
                }
                extractLevels(sourcePath + lvls[0], destinationPath + lvls[1]);
                checkCancel();
            }

            // step two: extract the styles
            out(String.format("%nExtracting styles"));
            ExtractSPR sprite = new ExtractSPR();
            ExtractSPR mask = new ExtractSPR();
            for (int i = 0; true; i++) {
                // 0: SPR, 1: masks, 2: PAL, 3: path, 4: fname
                String[] styles = props.getArray("style_" + i, null);
                if (styles == null) {
                    break;
                }
                out(styles[4]);
                Path dest = Paths.get(destinationPath + styles[3]);
                Files.createDirectories(dest);
                // load palette and sprite
                sprite.loadPalette(sourcePath + styles[2]);
                sprite.loadSPR(sourcePath + styles[0]);
                mask.loadPalette(sourcePath + styles[2]);
                mask.loadSPR(sourcePath + styles[1]);
                mask.createMasks();
                String[] files = sprite.saveAll(destinationPath + addSeparator(styles[3]) + styles[4], false);
                String[] maskFiles = mask.saveAll(destinationPath + addSeparator(styles[3]) + styles[4] + "m", false);
                for (String file : files) {
                    createdFiles.put(file.toLowerCase(Locale.ROOT), null);
                }
                for (String maskFile : maskFiles) {
                    createdFiles.put(maskFile.toLowerCase(Locale.ROOT), null);
                }
                checkCancel();
            }

            // step three: extract the objects
            out(String.format("%nExtracting objects"));
            for (int i = 0; true; i++) {
                // 0:SPR, 1:PAL, 2:resource, 3:path
                String[] object = props.getArray("objects_" + i, null);
                if (object == null) {
                    break;
                }
                out(object[0]);
                Path dest = Paths.get(destinationPath, object[3]);
                Files.createDirectories(dest);
                // load palette and sprite
                sprite.loadPalette(sourcePath + object[1]);
                sprite.loadSPR(sourcePath + object[0]);
                for (int j = 0; true; j++) {
                    // 0: idx, 1: frames, 2: name
                    String[] member = props.getArray(object[2] + "_" + j, null);
                    if (member == null) {
                        break;
                    }
                    // save object
                    createdFiles.put((destinationPath + addSeparator(object[3]) + member[2]).toLowerCase(Locale.ROOT), null);
                    sprite.saveAnim(destinationPath + addSeparator(object[3]) + member[2],
                            Props.parseInt(member[0]), Props.parseInt(member[1]));
                    checkCancel();
                }
            }

            //if (false) { // debug only

            // step four: create directories
            out(String.format("%nCreate directories"));
            for (int i = 0; true; i++) {
                // 0: path
                String path = props.get("mkdir_" + i, "");
                if (path.isEmpty()) {
                    break;
                }
                out(path);
                Path dest = Paths.get(destinationPath, path);
                Files.createDirectories(dest);
                checkCancel();
            }

            // step five: copy stuff
            out(String.format("%nCopy files"));
            for (int i = 0; true; i++) {
                // 0: srcName, 1: destName
                String[] copy = props.getArray("copy_" + i, null);
                if (copy == null) {
                    break;
                }
                try {
                    copyFile(sourcePath + copy[0], destinationPath + copy[1]);
                    createdFiles.put((destinationPath + copy[1]).toLowerCase(Locale.ROOT), null);
                } catch (Exception ex) {
                    throw new ExtractException(String.format("Unable to copy %s%s to %s%s.", sourcePath, copy[0], destinationPath, copy[1]));
                }
                checkCancel();
            }

            // step five: clone files inside destination dir
            out(String.format("%nClone files"));
            for (int i = 0; true; i++) {
                // 0: srcName, 1: destName
                String[] clone = props.getArray("clone_" + i, null);
                if (clone == null) {
                    break;
                }
                try {
                    copyFile(destinationPath + clone[0], destinationPath + clone[1]);
                    createdFiles.put((destinationPath + clone[1]).toLowerCase(Locale.ROOT), null);
                } catch (Exception ex) {
                    throw new ExtractException(String.format("Unable to clone %1$s%2$s to %1$s%3$s.", destinationPath, clone[0], clone[1]));
                }
                checkCancel();
            }

            if (doCreatePatches) {
                // this is not needed by Lemmini, but to create the DIF files (and CRCs)
                if (DO_CREATE_CRC) {
                    // create crc.ini
                    out(String.format("%nCreate crc.ini"));
                    try (Writer fCRCList = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(crcPath + CRC_INI_NAME), StandardCharsets.UTF_8))) {
                        for (int i = 0; true; i++) {
                            String ppath;
                            ppath = props.get("pcrc_" + i, "");
                            if (ppath.isEmpty()) {
                                break;
                            }
                            createCRCs(sourcePath, ppath, fCRCList);
                        }
                    } catch (IOException ex) {
                        throw new ExtractException(String.format("Unable to create %s%s.", crcPath, CRC_INI_NAME));
                    }
                    checkCancel();
                }

                // step seven: create patches and patch.ini
                Files.createDirectories(Paths.get(patchPath));
                out(String.format("%nCreate patch INI"));
                try (Writer fPatchList = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(patchPath + PATCH_INI_NAME), StandardCharsets.UTF_8))) {
                    for (int i = 0; true; i++) {
                        String ppath;
                        ppath = props.get("ppatch_" + i, "");
                        if (ppath.isEmpty()) {
                            break;
                        }
                        createPatches(referencePath, destinationPath, ppath, patchPath, fPatchList);
                    }
                } catch (IOException ex) {
                    throw new ExtractException(String.format("Unable to create %s%s.", patchPath, PATCH_INI_NAME));
                }
                checkCancel();
            }

            // step eight: use patch.ini to extract/patch all files
            // read patch.ini file
            Props pprops = new Props();
            URL fnp = findFile(patchPath + PATCH_INI_NAME/*, this*/); // if it's in the JAR or local directory
            if (!pprops.load(fnp)) {
                throw new ExtractException("File " + PATCH_INI_NAME + " not found or error while reading.");
            }
            // copy
            out(String.format("%nExtract files"));
            for (int i = 0; true; i++) {
                // 0: name, 1: crc
                String[] copy = pprops.getArray("extract_" + i, null);
                if (copy == null) {
                    break;
                }
                out(copy[0]);
                String fnDecorated = copy[0].replace('/', '@');
                URL fnc = findFile(patchPath + fnDecorated /*, pprops*/);
                try {
                    copyFile(fnc, destinationPath + copy[0]);
                } catch (Exception ex) {
                    throw new ExtractException(String.format("Unable to copy %s%s to %s%s.", patchPath, getFileName(copy[0]), destinationPath, copy[0]));
                }
                checkCancel();
            }
            // patch
            out(String.format("%nPatch files"));
            for (int i = 0; true; i++) {
                // 0: name, 1: crc
                String[] ppath = pprops.getArray("patch_" + i, null);
                if (ppath == null) {
                    break;
                }
                out(ppath[0]);
                String fnDif = ppath[0].replace('/', '@'); //getFileName(ppath[0]);
                int pos = fnDif.lastIndexOf('.');
                if (pos == -1) {
                    pos = fnDif.length();
                }
                fnDif = fnDif.substring(0, pos) + ".dif";
                URL urlDif = findFile(patchPath + fnDif);
                if (urlDif == null) {
                    throw new ExtractException(String.format("Unable to patch file %s%s.%n", destinationPath, ppath[0]));
                }
                byte[] dif = readFile(urlDif);
                byte[] src = readFile(destinationPath + ppath[0]);
                try {
                    byte[] trg = Diff.patchBuffers(src, dif);
                    // write new file
                    writeFile(destinationPath + ppath[0], trg);
                } catch (DiffException ex) {
                    throw new ExtractException(String.format("Unable to patch file %s%s.%n%s",
                            destinationPath, ppath[0], ex.getMessage()));
                }
                checkCancel();
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
        }
        outputDiag.enableOk();
    }

    /**
     * Get source path (WINLEMM) for extraction.
     * @return source path (WINLEMM) for extraction
     */
    public static String getSourcePath() {
        return sourcePath;
    }

    /**
     * Get destination path (Lemmini resource) for extraction.
     * @return destination path (Lemmini resource) for extraction
     */
    public static String getResourcePath() {
        return destinationPath;
    }

    /**
     * Extract all resources and create patch.ini if referencePath is not null
     * @param frame parent frame
     * @param srcPath WINLEMM directory
     * @param dstPath target (installation) directory. May also be a relative path inside JAR
     * @param refPath the reference path with the original (wanted) files
     * @param pPath the path to store the patch files to
     * @param createPatches create patches if true
     * @throws ExtractException
     */
    public static void extract(final JFrame frame, final String srcPath, final String dstPath,
            final String refPath, final String pPath, final boolean createPatches) throws ExtractException {

        doCreatePatches = createPatches;
        sourcePath = exchangeSeparators(addSeparator(srcPath));
        destinationPath = exchangeSeparators(addSeparator(dstPath));
        if (refPath != null) {
            referencePath = exchangeSeparators(addSeparator(refPath));
        }
        patchPath = exchangeSeparators(addSeparator(pPath));
        crcPath = destinationPath; // OK, this is the wrong path, but this is executed once in a lifetime

        loader = Extract.class.getClassLoader();

        FolderDialog fDiag;
        do {
            fDiag = new FolderDialog(frame, true);
            fDiag.setParameters(sourcePath, destinationPath);
            fDiag.setVisible(true);
            if (!fDiag.getSuccess()) {
                throw new ExtractException("Extraction canceled by user.", true);
            }
            sourcePath = exchangeSeparators(addSeparator(fDiag.getSource()));
            destinationPath = exchangeSeparators(addSeparator(fDiag.getTarget()));
            // check if source path exists
            Path fSrc = Paths.get(sourcePath);
            if (Files.exists(fSrc)) {
                break;
            }
            JOptionPane.showMessageDialog(frame, String.format("Source path %s doesn't exist!", sourcePath), "Error", JOptionPane.ERROR_MESSAGE);
        } while (true);

        // open output dialog
        outputDiag = new OutputDialog(frame, true);

        // start thread
        threadException = null;
        thisThread = new Thread(new Extract());
        thisThread.start();

        outputDiag.setVisible(true);
        while (thisThread.isAlive()) {
            try  {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
            }
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
    private static void extractLevels(final String r, final String destin) throws ExtractException {
        // first extract the levels
        File fRoot = new File(r);
        FilenameFilter ff = new LvlFilter();

        String root = addSeparator(r);
        String destination = addSeparator(destin);
        Path dest = Paths.get(destination);
        try {
            Files.createDirectories(dest);
        } catch (IOException ex) {
        }

        File[] levels = fRoot.listFiles(ff);
        if (levels == null) {
            throw new ExtractException(String.format("Path %s doesn't exist or IO error occurred.", root));
        }
        for (File level : levels) {
            int pos;
            String fIn = root + level.getName();
            String fOut = level.getName().toLowerCase(Locale.ROOT);
            pos = fOut.lastIndexOf(".lvl"); // MUST be there because of file filter
            fOut = destination + fOut.substring(0, pos) + ".ini";
            createdFiles.put(fOut.toLowerCase(Locale.ROOT), null);
            try {
                out(level.getName());
                ExtractLevel.convertLevel(fIn, fOut, false, true);
            } catch (Exception ex) {
                String msg = ex.getMessage();
                if (msg != null && !msg.isEmpty()) {
                    out(ex.getMessage());
                } else {
                    out(ex.toString());
                }
                throw new ExtractException(msg);
            }
        }
    }

    /**
     * Create the DIF files from reference files and the extracted files (development).
     * @param sPath The path with the original (wanted) files
     * @param dPath  The patch with the differing (to be patched) files
     * @param subDir SubDir to create patches for
     * @param pPath  The patch to write the patches to
     * @param fPatchList Writer to create patch.ini
     * @throws ExtractException
     */
    private static void createPatches(final String sPath, final String dPath, final String sDir, final String pPath, final Writer fPatchList) throws ExtractException {
        // add separators and create missing directories
        String patchSourcePath = addSeparator(sPath+sDir);
        File fSource = new File(patchSourcePath);

        String destPath = addSeparator(dPath+sDir);
        Path fDest = Paths.get(destPath);
        try {
            Files.createDirectories(fDest);
        } catch (IOException ex) {
        }

        String out;
        patchPath = addSeparator(pPath);
        Path fPatch = Paths.get(patchPath);
        try {
            Files.createDirectories(fPatch);
        } catch (IOException ex) {
        }

        File[] files = fSource.listFiles();
        if (files == null) {
            throw new ExtractException(String.format("Path %s doesn't exist or IO error occurred.", patchSourcePath));
        }
        Diff.setParameters(512, 4);
        String subDir = addSeparator(sDir);
        String subDirDecorated = subDir.replace('/', '@');

        outerLoop:
        for (File file : files) {
            int pos;
            // ignore directories
            if (file.isDirectory()) {
                continue;
            }
            String fileName = file.getName();
            // check extension
            pos = fileName.lastIndexOf('.');
            if (pos > -1) {
                String ext = fileName.substring(pos + 1);
                for (String ignoreExt1 : ignoreExt) {
                    if (ignoreExt1.equalsIgnoreCase(ext)) {
                        continue outerLoop;
                    }
                }
            }

            String fnIn = patchSourcePath + fileName;
            String fnOut = destPath + fileName;
            String fnPatch = fileName;

            pos = fnPatch.lastIndexOf('.');
            if (pos == -1) {
                pos = fnPatch.length();
            }
            fnPatch = patchPath + subDirDecorated + fnPatch.substring(0, pos).toLowerCase(Locale.ROOT) + ".dif";
            try {
                out(fnIn);
                // read src file
                byte[] src = readFile(fnIn);
                byte[] trg = null;
                // read target file
                boolean fileExists;
                fileExists = createdFiles.containsKey(fnOut.toLowerCase(Locale.ROOT));
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
                    out = subDir + fileName + ", 0x" + Long.toHexString(crc.getValue());
                    fPatchList.write("extract_" + (extractNo++) + " = " + out + "\r\n");
                    // copy missing files to patch dir
                    copyFile(fnIn, patchPath + subDirDecorated + fileName);
                    continue;
                }
                // create diff
                byte[] patch = Diff.diffBuffers(trg, src);
                int crc = Diff.targetCRC; // crc of target buffer
                out = subDir + fileName + ", 0x" + Integer.toHexString(crc);
                if (patch == null) {
                    //out("src and trg are identical");
                    fPatchList.write("check_" + (checkNo++) + " = " + out + "\r\n");
                } else {
                    // apply patch to test whether it's OK
                    Diff.patchBuffers(trg, patch);
                    // write patch file
                    writeFile(fnPatch, patch);
                    fPatchList.write("patch_" + (patchNo++) + " = " + out + "\r\n");
                }
            } catch (Exception ex)  {
                //String msg = ex.getMessage();
                //if (msg == null) {
                //    msg = ex.toString();
                //}
                throw new ExtractException(ex.getMessage());
            }
        }
    }

    /**
     * Create CRCs for resources (development).
     * @param rPath The root path with the files to create CRCs for
     * @param sDir SubDir to create patches for
     * @param fCRCList Writer to create crc.ini
     * @throws ExtractException
     */
    private static void createCRCs(final String rPath, final String sDir, final Writer fCRCList) throws ExtractException {
        // add separators and create missing directories
        String rootPath = addSeparator(rPath + sDir);
        File fSource = new File(rootPath);
        String out;
        File[] files = fSource.listFiles();
        if (files == null) {
            throw new ExtractException(String.format("Path %s doesn't exist or IO error occurred.", rootPath));
        }
        String subDir = addSeparator(sDir);

        outerLoop:
            for (File file : files) {
                int pos;
                // ignore directories
                if (file.isDirectory()) {
                    continue;
                }
                String fileName = file.getName();
                // check extension
                pos = fileName.lastIndexOf('.');
                if (pos > -1) {
                    String ext = fileName.substring(pos + 1);
                    for (String ignoreExt1 : ignoreExt) {
                        if (ignoreExt1.equalsIgnoreCase(ext)) {
                            continue outerLoop;
                        }
                    }
                }
                String fnIn = rootPath + fileName;
                try {
                    out(fnIn);
                    // read src file
                    byte[] src = readFile(fnIn);
                    Adler32 crc32 = new Adler32();
                    crc32.update(src);
                    out = subDir + fileName + ", " + src.length + ", 0x" + Long.toHexString(crc32.getValue());
                    fCRCList.write("crc_" + (crcNo++) + " = " + out + "\r\n");
                } catch (Exception ex)  {
                    //String msg = ex.getMessage();
                    //if (msg == null) {
                    //    msg = ex.toString();
                    //}
                    throw new ExtractException(ex.getMessage());
                }
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
     * Exchange all Windows style file separators ("\") with Unix style seaparators ("/")
     * @param fName file name
     * @return file name with only Unix style separators
     */
    private static String exchangeSeparators(final String fName) {
        return fName.replace('\\', '/');
    }

    /**
     * Get only the name of the file from an absolute path.
     * @param path absolute path of a file
     * @return file name without the path
     */
    private static String getFileName(final String path) {
        int p1 = path.lastIndexOf("/");
        int p2 = path.lastIndexOf("\\");
        if (p2 > p1) {
            p1 = p2;
        }
        if (p1 < 0) {
            p1 = 0;
        } else {
            p1++;
        }
        return path.substring(p1);
    }

    /**
     * Copy a file.
     * @param source URL of source file
     * @param destination full destination file name including path
     * @throws IOException
     */
    private static void copyFile(final URL source, final String destination) throws IOException {
        try (InputStream fSrc = new BufferedInputStream(source.openStream())) {
            Path fDest = Paths.get(destination);
            Files.copy(fSrc, fDest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Copy a file.
     * @param source full source file name including path
     * @param destination full destination file name including path
     * @throws IOException
     */
    private static void copyFile(final String source, final String destination) throws IOException {
        Path fSrc = Paths.get(source);
        Path fDest = Paths.get(destination);
        Files.copy(fSrc, fDest, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Read file into an array of bytes.
     * @param fname file name
     * @return array of bytes
     * @throws ExtractException
     */
    private static byte[] readFile(final String fname) throws ExtractException {
        try {
            return Files.readAllBytes(Paths.get(fname));
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
        byte[] buf;
        try (InputStream f = new BufferedInputStream(fname.openStream())) {
            byte[] buffer = new byte[4096];
            // URLs/InputStreams suck: we can't read a length
            int len;
            List<Byte> lbuf = new ArrayList<>();

            while ((len = f.read(buffer)) != -1) {
                for (int i = 0; i < len; i++) {
                    lbuf.add(buffer[i]);
                }
            }

            // reconstruct byte array from ArrayList
            buf = new byte[lbuf.size()];
            for (int i = 0; i < buf.length; i++) {
                buf[i] = lbuf.get(i);
            }

            return buf;
        } catch (FileNotFoundException ex) {
            throw new ExtractException(String.format("File %s not found.", fname));
        } catch (IOException ex) {
            throw new ExtractException(String.format("IO exception while reading file %s.", fname));
        }
    }

    /**
     * Write array of bytes to file.
     * @param fname file name
     * @param buf array of byte
     * @throws ExtractException
     */
    private static void writeFile(final String fname, final byte[] buf) throws ExtractException {
        try {
            Files.write(Paths.get(fname), buf);
        } catch (IOException ex) {
            throw new ExtractException(String.format("IO exception while writing file %s.", fname));
        }
    }

    /**
     * Find a file.
     * @param fname File name (without absolute path)
     * @return URL of file
     */
    public static URL findFile(final String fname) {
        URL retval = loader.getResource(fname);
        try {
            if (retval == null) {
                retval = Paths.get(fname).toUri().toURL();
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
        if (outputDiag != null) {
            outputDiag.print(String.format("%s%n", s));
        }
    }

    /**
     * Return cancel state of output dialog
     * @throws ExtractException
     */
    private static void checkCancel() throws ExtractException {
        if (outputDiag.isCanceled()) {
            throw new ExtractException("Extraction canceled by user.", true);
        }
    }
}

/**
 * File name filter for level files.
 * @author Volker Oth
 */
class LvlFilter implements FilenameFilter {

    /* (non-Javadoc)
     * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
     */
    @Override
    public boolean accept(final File dir, final String name) {
        return name.toLowerCase().contains(".lvl");
    }
}

