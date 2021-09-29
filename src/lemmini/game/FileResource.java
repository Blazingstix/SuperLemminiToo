/*
 * Copyright 2015 Ryan.
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
package lemmini.game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import lemmini.tools.CaseInsensitiveFileTree;
import lemmini.tools.ToolBox;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author Ryan
 */
public class FileResource implements Resource {
    
    private final String origPath;
    private final String realPath;
    private final CaseInsensitiveFileTree tree;
    private final Path file;
    
    public FileResource(Path file) throws IOException {
        this.origPath = FilenameUtils.separatorsToUnix(file.toString());
        this.realPath = origPath;
        this.tree = new CaseInsensitiveFileTree(file.getParent(), 1);
        this.file = file;
    }
    
    public FileResource(String origPath, CaseInsensitiveFileTree tree) {
        this(origPath, origPath, tree, tree.getPath(origPath));
    }
    
    public FileResource(String origPath, String realPath, CaseInsensitiveFileTree tree) {
        this(origPath, realPath, tree, tree.getPath(realPath));
    }
    
    private FileResource(String origPath, String realPath,
            CaseInsensitiveFileTree tree, Path file) {
        this.origPath = origPath;
        this.realPath = realPath;
        this.tree = tree;
        this.file = file;
    }
    
    @Override
    public boolean exists() {
        return Files.isRegularFile(file);
    }

    @Override
    public String getFileName() {
        return file.getFileName().toString();
    }

    @Override
    public String getOriginalPath() {
        return origPath;
    }
    
    @Override
    public FileResource getSibling(String sibling) {
        String newOrigPath = ToolBox.getParent(origPath) + sibling;
        String newRealPath = ToolBox.getParent(realPath) + sibling;
        Path newFile = tree.getPath(sibling);
        return new FileResource(newOrigPath, newRealPath, tree, newFile);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(file);
    }

    @Override
    public BufferedReader getBufferedReader() throws IOException {
        return ToolBox.getBufferedReader(file);
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        return Files.readAllBytes(file);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof FileResource)) {
            return false;
        }
        
        FileResource res2 = (FileResource) o;
        
        return file.equals(res2.file);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + Objects.hashCode(this.file);
        return hash;
    }
}
