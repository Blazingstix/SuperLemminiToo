package lemmini.extract;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.Adler32;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import lemmini.tools.Props;
import lemmini.tools.ToolBox;

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
    private static Set<Path> createdFiles;
    /** source path (WINLEMM) for extraction */
    private static Path sourcePath;
    /** destination path (Lemmini resource) for extraction */
    private static Path destinationPath;
    /** reference path for creation of DIF files */
    private static Path referencePath;
    /** path of the DIF files */
    private static Path patchPath;
    /** path of the CRC INI (without the file name) */
    private static Path crcPath;
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
        createdFiles = new HashSet<>(); // to monitor the files created without erasing the target dir

        try {
            // read INI file
            Props props = new Props();
            URL fn = findFile(Paths.get(INI_NAME));
            if (fn == null || !props.load(fn)) {
                throw new ExtractException("File " + INI_NAME + " not found or error while reading.");
            }

            ignoreExt = props.getArray("ignore_ext", ignoreExt);

            // prolog_ check CRC
            out(String.format("%nValidating WINLEMM"));
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
                long srcLen = Files.size(sourceFile);
                long reqLen = Long.parseLong(crcbuf[1]);
                if (srcLen != reqLen) {
                    throw new ExtractException(String.format(
                            "CRC error for file %s. (Size is %,d, expected %,d.)%n",
                            sourceFile, srcLen, reqLen));
                }
                byte[] src = readFile(sourceFile);
                Adler32 crc32 = new Adler32();
                crc32.update(src);
                long srcCrc = crc32.getValue();
                long reqCrc = Long.parseLong(crcbuf[2].substring(2), 16);
                if (srcCrc != reqCrc) {
                    throw new ExtractException(String.format(
                            "CRC error for file %s. (CRC is %#010x, expected %#010x.)%n",
                            sourceFile, srcCrc, reqCrc));
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
                extractLevels(sourcePath.resolve(lvls[0]), destinationPath.resolve(lvls[1]));
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
                Path dest = destinationPath.resolve(styles[3]);
                Files.createDirectories(dest);
                // load palette and sprite
                sprite.loadPalette(sourcePath.resolve(styles[2]));
                sprite.loadSPR(sourcePath.resolve(styles[0]));
                mask.loadPalette(sourcePath.resolve(styles[2]));
                mask.loadSPR(sourcePath.resolve(styles[1]));
                mask.createMasks();
                Path[] files = sprite.saveAll(dest.resolve(styles[4]), false);
                Path[] maskFiles = mask.saveAll(dest.resolve(styles[4] + "m"), false);
                createdFiles.addAll(Arrays.asList(files));
                createdFiles.addAll(Arrays.asList(maskFiles));
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
                Path dest = destinationPath.resolve(object[3]);
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

            // step four: create directories
            out(String.format("%nCreate directories"));
            for (int i = 0; true; i++) {
                // 0: path
                String path = props.get("mkdir_" + i, "");
                if (path.isEmpty()) {
                    break;
                }
                out(path);
                Path dest = destinationPath.resolve(path);
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
                Path sourceFile = sourcePath.resolve(copy[0]);
                Path destinationFile = destinationPath.resolve(copy[1]);
                try {
                    copyFile(sourceFile, destinationFile);
                    createdFiles.add(destinationFile);
                } catch (Exception ex) {
                    throw new ExtractException(String.format("Unable to copy %s to %s.", sourceFile, destinationFile));
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
                Path sourceFile = destinationPath.resolve(clone[0]);
                Path destinationFile = destinationPath.resolve(clone[1]);
                try {
                    copyFile(sourceFile, destinationFile);
                    createdFiles.add(destinationFile);
                } catch (Exception ex) {
                    throw new ExtractException(String.format("Unable to clone %s to %s.", sourceFile, destinationFile));
                }
                checkCancel();
            }
            
            Path patchINIPath = patchPath.resolve(PATCH_INI_NAME);

            if (doCreatePatches) {
                // this is not needed by Lemmini, but to create the DIF files (and CRCs)
                if (DO_CREATE_CRC) {
                    // create crc.ini
                    out(String.format("%nCreate crc.ini"));
                    Path crcINIPath = crcPath.resolve(CRC_INI_NAME);
                    try (Writer fCRCList = Files.newBufferedWriter(crcINIPath, StandardCharsets.UTF_8)) {
                        for (int i = 0; true; i++) {
                            String ppath;
                            ppath = props.get("pcrc_" + i, "");
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

                // step seven: create patches and patch.ini
                Files.createDirectories(patchPath);
                out(String.format("%nCreate patch INI"));
                try (Writer fPatchList = Files.newBufferedWriter(patchINIPath, StandardCharsets.UTF_8)) {
                    for (int i = 0; true; i++) {
                        String ppath;
                        ppath = props.get("ppatch_" + i, "");
                        if (ppath.isEmpty()) {
                            break;
                        }
                        createPatches(referencePath, destinationPath, ppath, fPatchList);
                        checkCancel();
                    }
                } catch (IOException ex) {
                    throw new ExtractException(String.format("Unable to create %s.", patchINIPath));
                }
            }

            // step eight: use patch.ini to extract/patch all files
            // read patch.ini file
            Props pprops = new Props();
            URL fnp = findFile(patchINIPath); // if it's in the JAR or local directory
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
                URL fnc = findFile(patchPath.resolve(fnDecorated));
                Path destination = destinationPath.resolve(copy[0]);
                try {
                    copyFile(fnc, destination);
                } catch (Exception ex) {
                    throw new ExtractException(String.format("Unable to copy %s to %s.", patchPath.resolve(getFileName(copy[0])), destination));
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
                URL urlDif = findFile(patchPath.resolve(fnDif));
                Path destination = destinationPath.resolve(ppath[0]);
                if (urlDif == null) {
                    throw new ExtractException(String.format("Unable to patch file %s.%n", destination));
                }
                byte[] dif = readFile(urlDif);
                byte[] src = readFile(destination);
                try {
                    byte[] trg = Diff.patchBuffers(src, dif);
                    // write new file
                    writeFile(destination, trg);
                } catch (DiffException ex) {
                    throw new ExtractException(String.format("Unable to patch file %s.%n%s",
                            destination, ex.getMessage()));
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
        outputDiag.enableOK();
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
     * @param frame parent frame
     * @param srcPath WINLEMM directory
     * @param dstPath target (installation) directory. May also be a relative path inside JAR
     * @param refPath the reference path with the original (wanted) files
     * @param pPath the path to store the patch files to
     * @param createPatches create patches if true
     * @throws ExtractException
     */
    public static void extract(final JFrame frame, final Path srcPath, final Path dstPath,
            final Path refPath, final Path pPath, final boolean createPatches) throws ExtractException {

        doCreatePatches = createPatches;
        sourcePath = srcPath;
        destinationPath = dstPath;
        if (refPath != null) {
            referencePath = refPath;
        }
        patchPath = pPath;
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
            sourcePath = fDiag.getSource();
            destinationPath = fDiag.getTarget();
            // check if source path exists
            if (Files.isDirectory(sourcePath)) {
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
    private static void extractLevels(final Path r, final Path dest) throws ExtractException {
        // first extract the levels
        try {
            Files.createDirectories(dest);
        } catch (IOException ex) {
        }

        try (DirectoryStream<Path> levels = Files.newDirectoryStream(r, new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return Files.isRegularFile(entry) && (entry).getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".lvl");
            }
        })) {
            for (Path level : levels) {
                Path fIn = r.resolve(level.getFileName());
                String fOutStr = level.getFileName().toString().toLowerCase(Locale.ROOT);
                int pos = fOutStr.lastIndexOf(".lvl"); // MUST be there because of file filter
                Path fOut = dest.resolve(fOutStr.substring(0, pos) + ".ini");
                createdFiles.add(fOut);
                out(level.getFileName().toString());
                ExtractLevel.convertLevel(fIn, fOut, false, true);
            }
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

    /**
     * Create the DIF files from reference files and the extracted files (development).
     * @param sPath The path with the original (wanted) files
     * @param dPath  The path with the differing (to be patched) files
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
                    if (pos > -1) {
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
                    if (pos == -1) {
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
                        fPatchList.write(String.format(Locale.ROOT, "extract_%d = %s\r\n", extractNo++, out));
                        // copy missing files to patch dir
                        copyFile(fnIn, patchPath.resolve(subDirDecorated + fileName));
                        continue;
                    }
                    // create diff
                    byte[] patch = Diff.diffBuffers(trg, src);
                    int crc = Diff.targetCRC; // crc of target buffer
                    out = String.format(Locale.ROOT, "%s%s, %#010x", subDir, fileName, crc);
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
                    if (pos > -1) {
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
     * Exchange all Windows style file separators ("\") with Unix style separators ("/")
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
 * Filter for patch files.
 * @author Ryan Sakowski
 */
class PatchFilter implements DirectoryStream.Filter<Path> {
    
    @Override
    public boolean accept(Path entry) throws IOException {
        return Files.isRegularFile(entry);
    }
}

