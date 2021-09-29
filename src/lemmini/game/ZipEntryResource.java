/*
 * Copyright 2015 Ryan Sakowski.
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
import java.nio.charset.UnsupportedCharsetException;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lemmini.tools.ToolBox;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Ryan
 */
public class ZipEntryResource implements Resource {
    
    private final String origPath;
    private final ZipFile zipFile;
    private final ZipEntry zipEntry;
    private final String zipEntryName;
    
    public ZipEntryResource(String origPath, ZipFile zipFile, ZipEntry zipEntry) {
        this.origPath = origPath;
        this.zipFile = zipFile;
        this.zipEntry = zipEntry;
        this.zipEntryName = zipEntry.getName();
    }
    
    public ZipEntryResource(String origPath, ZipFile zipFile, String zipEntryName) {
        this.origPath = origPath;
        this.zipFile = zipFile;
        this.zipEntry = zipFile.getEntry(zipEntryName);
        this.zipEntryName = zipEntryName;
    }
    
    @Override
    public boolean exists() {
        return zipEntry != null;
    }

    @Override
    public String getFileName() {
        return FilenameUtils.getName(zipEntryName);
    }

    @Override
    public String getOriginalPath() {
        return origPath;
    }
    
    @Override
    public ZipEntryResource getSibling(String sibling) {
        String newOrigPath = FilenameUtils.getPath(origPath) + sibling;
        String newZipEntryName = FilenameUtils.getPath(zipEntryName) + sibling;
        return new ZipEntryResource(newOrigPath, zipFile, newZipEntryName);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return zipFile.getInputStream(zipEntry);
    }

    @Override
    public BufferedReader getBufferedReader() throws IOException {
        InputStream in = null;
        try {
            in = getInputStream();
            return ToolBox.getBufferedReader(in);
        } catch (IOException | SecurityException | UnsupportedCharsetException ex) {
            if (in != null) {
                in.close();
            }
            throw ex;
        }
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        try (InputStream in = getInputStream()) {
            return IOUtils.toByteArray(in);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ZipEntryResource)) {
            return false;
        }
        
        ZipEntryResource res2 = (ZipEntryResource) o;
        
        return zipFile.equals(res2.zipFile)
                && zipEntry.getName().equals(res2.zipEntry.getName());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.zipFile);
        hash = 67 * hash + Objects.hashCode(this.zipEntry);
        return hash;
    }
}
