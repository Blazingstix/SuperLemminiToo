/*
 * Copyright 2016 Ryan Sakowski.
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
package lemmini.tools;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Provides access to a directory tree in a case-insensitive manner.
 * @author Ryan Sakowski
 */
public class CaseInsensitiveFileTree {
    
    private static final FileNameComparator FILE_NAME_COMPARATOR = new FileNameComparator();
    
    private final Path root;
    private final Map<String, List<Path>> files = new LinkedHashMap<>(1024);
    
    public CaseInsensitiveFileTree(Path directory) throws IOException {
        this(directory, Integer.MAX_VALUE);
    }
    
    public CaseInsensitiveFileTree(Path directory, int maxDepth) throws IOException {
        root = directory;
        refresh(maxDepth);
    }
    
    /**
     * Clears the file-name cache and re-scans the directory tree.
     * @param maxDepth
     * @throws IOException 
     */
    public final void refresh(int maxDepth) throws IOException {
        files.clear();
        Map<String, List<Path>> filesTemp = new TreeMap<>(FILE_NAME_COMPARATOR);
        if (Files.exists(root)) {
            try (Stream<Path> fileStream = Files.walk(root, maxDepth)) {
                fileStream.forEach(path -> {
                    String relativePathStr = pathToString(root.relativize(path));
                    List<Path> pathVariants = filesTemp.computeIfAbsent(relativePathStr,  s -> new ArrayList<>(1));
                    pathVariants.add(path);
                });
            }
        }
        files.putAll(filesTemp);
    }
    
    public Path getRoot() {
        return root;
    }
    
    /**
     * Returns a Path that matches the given file name.
     * @param fileName the name of the file to look for
     * @return a Path object that matches the given file name
     */
    public Path getPath(String fileName) {
        String normalizedFileName = normalize(fileName);
        Path possiblePath = getPath1(normalizedFileName);
        if (possiblePath == null) {
            if (normalizedFileName.equals("/")) {
                return root;
            } else {
                return root.resolve(getPath(ToolBox.getParent(fileName))).resolve(ToolBox.getFileName(fileName));
            }
        } else {
            return possiblePath;
        }
    }
    
    private Path getPath1(String normalizedFileName) {
        List<Path> fileVariants = files.getOrDefault(normalizedFileName, Collections.emptyList());
        for (Iterator<Path> it = fileVariants.iterator(); it.hasNext(); ) {
            Path path = it.next();
            if (Files.notExists(path)) {
                it.remove();
            } else {
                return path;
            }
        }
        files.remove(normalizedFileName);
        return null;
    }
    
    /**
     * Returns a List of Paths that match the given file name.
     * @param fileName the name of the file to look for
     * @return List of Path objects
     */
    public List<Path> getAllPaths(String fileName) {
        return Collections.unmodifiableList(files.getOrDefault(normalize(fileName), Collections.emptyList()));
    }
    
    /**
     * Returns a List of Paths that match the given regular expression.
     * @param regex
     * @return List of Path objects
     */
    public List<Path> getAllPathsRegex(String regex) {
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        return files.entrySet().stream()
                .filter(entry -> p.matcher(entry.getKey()).matches())
                .map(Map.Entry::getValue)
                .collect(() -> new ArrayList<Path>(512), List::addAll, List::addAll);
    }
    
    public boolean exists(String fileName) {
        return !files.getOrDefault(normalize(fileName), Collections.emptyList()).isEmpty();
    }
    
    /**
     * Attempts to open an OutputStream on the given file.
     * @param fileName name of file to open
     * @param options
     * @return
     * @throws IOException if an I/O error occurs
     */
    public OutputStream newOutputStream(String fileName, OpenOption... options) throws IOException {
        String normalizedFileName = normalize(fileName);
        if (isDirectory(normalizedFileName)) {
            // OutputStreams cannot be opened on directories
            throw new IOException("Not a valid file name: " + fileName);
        }
        Path possiblePath = getPath1(normalizedFileName);
        if (possiblePath == null) {
            // no matching file exists; maybe create one in a parent directory
            // that matches the one in the given file name
            String parent = ToolBox.getParent(normalizedFileName);
            possiblePath = getPath1(parent);
            if (possiblePath == null) {
                // no matching parent directory exists
                throw new IOException("Directory does not exist: " + ToolBox.getParent(fileName));
            } else {
                // matching parent directory exists
                Path path = possiblePath.resolve(ToolBox.getFileName(fileName));
                OutputStream out = Files.newOutputStream(path, options);
                try {
                    List<Path> pathVariants = files.computeIfAbsent(normalizedFileName,  s -> new ArrayList<>(1));
                    pathVariants.add(path);
                    return out;
                } catch (Exception ex) {
                    IOUtils.closeQuietly(out);
                    throw ex;
                }
            }
        } else {
            // matching file exists; open an OutputStream on it
            return Files.newOutputStream(possiblePath, options);
        }
    }
    
    /**
     * Opens a BufferedWriter on the given file using the given encoding.
     * @param fileName
     * @param cs
     * @param options
     * @return
     * @throws IOException 
     */
    public BufferedWriter newBufferedWriter(String fileName, Charset cs, OpenOption... options) throws IOException {
        String normalizedFileName = normalize(fileName);
        if (isDirectory(normalizedFileName)) {
            // BufferedWriters cannot be opened on directories
            throw new IOException("Not a valid file name: " + fileName);
        }
        Path possiblePath = getPath1(normalizedFileName);
        if (possiblePath == null) {
            // no matching file exists; maybe create one in a parent directory
            // that matches the one in the given file name
            String parent = ToolBox.getParent(normalizedFileName);
            possiblePath = getPath1(parent);
            if (possiblePath == null) {
                // no matching parent directory exists
                throw new IOException("Directory does not exist: " + ToolBox.getParent(fileName));
            } else {
                // matching parent directory exists
                Path path = possiblePath.resolve(ToolBox.getFileName(fileName));
                BufferedWriter w = Files.newBufferedWriter(path, cs, options);
                try {
                    List<Path> pathVariants = files.computeIfAbsent(normalizedFileName,  s -> new ArrayList<>(1));
                    pathVariants.add(path);
                    return w;
                } catch (Exception ex) {
                    IOUtils.closeQuietly(w);
                    throw ex;
                }
            }
        } else {
            return Files.newBufferedWriter(possiblePath, cs, options);
        }
    }
    
    /**
     * Opens a BufferedWriter on the given file using the UTF-8 encoding.
     * @param fileName
     * @param options
     * @return
     * @throws IOException 
     */
    public BufferedWriter newBufferedWriter(String fileName, OpenOption... options) throws IOException {
        return newBufferedWriter(fileName, StandardCharsets.UTF_8, options);
    }
    
    public Path createDirectories(String fileName, FileAttribute<?>... attrs) throws IOException {
        String normalizedFileName = normalize(fileName);
        if (!isDirectory(normalizedFileName)) {
            throw new IOException("Not a valid directory name: " + fileName);
        }
        Path directory = getPath1(normalizedFileName);
        if (directory == null) {
            if (normalizedFileName.equals("/")) {
                directory = root;
            } else {
                Path parent = createDirectories(ToolBox.getParent(normalizedFileName), attrs);
                directory = parent.resolve(ToolBox.getFileName(fileName));
            }
            Files.createDirectories(directory);
            List<Path> directoryVariants = files.computeIfAbsent(normalizedFileName, s -> new ArrayList<>(1));
            directoryVariants.add(directory);
        }
        return directory;
    }
    
    /**
     * Deletes all files and directories that match the given name. If a
     * directory name is given, then all files and directories in that directory
     * are also deleted.
     * @param fileName
     * @throws IOException 
     */
    public void delete(String fileName) throws IOException {
        String normalizedFileName = normalize(fileName);
        if (isDirectory(normalizedFileName)) {
            for (Iterator<Map.Entry<String, List<Path>>> it = files.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, List<Path>> entry = it.next();
                String entryName = entry.getKey();
                if (!entryName.startsWith(normalizedFileName) || isDirectory(entryName)) {
                    continue;
                }
                List<Path> fileVariants = entry.getValue();
                for (Iterator<Path> it2 = fileVariants.iterator(); it2.hasNext(); ) {
                    Path path = it2.next();
                    Files.deleteIfExists(path);
                    it2.remove();
                }
                it.remove();
            }
            List<Map.Entry<String, List<Path>>> directoryList = files.entrySet().stream()
                    .filter(entry -> {
                        String entryName = entry.getKey();
                        return isDirectory(entryName) && entryName.startsWith(normalizedFileName);
                    }).sorted((entry1, entry2) -> {
                        String entryName1 = entry1.getKey();
                        String entryName2 = entry2.getKey();
                        int slashCount1 = StringUtils.countMatches(entryName1, '/');
                        int slashCount2 = StringUtils.countMatches(entryName2, '/');
                        if (slashCount1 != slashCount2) {
                            return slashCount2 - slashCount1;
                        } else {
                            return entryName1.compareTo(entryName2);
                        }
                    }).collect(Collectors.toList());
            for (Map.Entry<String, List<Path>> entry : directoryList) {
                String entryName = entry.getKey();
                List<Path> fileVariants = entry.getValue();
                for (Iterator<Path> it2 = fileVariants.iterator(); it2.hasNext(); ) {
                    Path path = it2.next();
                    Files.deleteIfExists(path);
                    it2.remove();
                }
                files.remove(entryName);
            }
        } else {
            for (Path path : files.getOrDefault(normalizedFileName, Collections.emptyList())) {
                Files.deleteIfExists(path);
            }
            files.remove(normalizedFileName);
        }
    }
    
    /**
     * Deletes all empty directories that match the given name.
     * @param fileName
     * @throws IOException 
     */
    public void deleteIfEmpty(String fileName) throws IOException {
        String normalizedFileName = normalize(fileName);
        if (isDirectory(normalizedFileName)) {
            List<Path> fileVariants = files.get(normalizedFileName);
            if (fileVariants != null) {
                for (Iterator<Path> it = fileVariants.iterator(); it.hasNext(); ) {
                    Path path = it.next();
                    try {
                        Files.deleteIfExists(path);
                        it.remove();
                    } catch (DirectoryNotEmptyException ex) {
                    }
                }
                if (fileVariants.isEmpty()) {
                    files.remove(normalizedFileName);
                }
            }
        }
    }
    
    private static String pathToString(Path path) {
        StringBuilder sb = new StringBuilder(64);
        for (Path name : path) {
            sb.append(name.toString().toLowerCase(Locale.ROOT)).append('/');
        }
        if (!Files.isDirectory(path)) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }
    
    private static String normalize(String fileName) {
        if (fileName.isEmpty()) {
            return "/";
        } else {
            return FilenameUtils.normalize(fileName, true).toLowerCase(Locale.ROOT);
        }
    }
    
    private static boolean isDirectory(String fileName) {
        return fileName.endsWith("/");
    }
    
    private static class FileNameComparator implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            if ((o1.isEmpty() && !o2.isEmpty()) 
                    || (o1.equals("/") && !o2.equals("/"))) {
                return -1;
            } else if ((!o1.isEmpty() && o2.isEmpty())
                    || (!o1.equals("/") && o2.equals("/"))) {
                return 1;
            }
            
            int length1 = o1.length();
            int length2 = o2.length();
            int minLength = Math.min(length1, length2);
            
            for (int i = 0; i < minLength; i++) {
                char c1 = o1.charAt(i);
                char c2 = o2.charAt(i);
                if (c1 == '/' && c2 != '/') {
                    if (o2.indexOf('/', i) >= 0) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else if (c1 != '/' && c2 == '/') {
                    if (o1.indexOf('/', i) >= 0) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else if (c1 != c2) {
                    int slashPos1 = o1.indexOf('/', i);
                    int slashPos2 = o2.indexOf('/', i);
                    if (slashPos1 < 0 && slashPos2 >= 0) {
                        return -1;
                    } else if (slashPos1 >= 0 && slashPos2 < 0) {
                        return 1;
                    } else {
                        return c1 - c2;
                    }
                }
            }
            
            return length1 - length2;
        }
    }
}
