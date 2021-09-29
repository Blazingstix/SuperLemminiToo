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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Provides access to a zip file in a case-insensitive manner.
 * @author Ryan Sakowski
 */
public class CaseInsensitiveZipFile extends ZipFile {
    
    private final Map<String, String> nameRemap = new HashMap<>(4096);
    
    public CaseInsensitiveZipFile(String name) throws IOException {
        super(name);
        createNameRemap();
    }
    
    public CaseInsensitiveZipFile(File file, int mode) throws IOException {
        super(file, mode);
        createNameRemap();
    }
    
    public CaseInsensitiveZipFile(File file) throws ZipException, IOException {
        super(file);
        createNameRemap();
    }
    
    public CaseInsensitiveZipFile(File file, int mode, Charset charset) throws IOException {
        super(file, mode, charset);
        createNameRemap();
    }
    
    public CaseInsensitiveZipFile(String name, Charset charset) throws IOException {
        super(name, charset);
        createNameRemap();
    }
    
    public CaseInsensitiveZipFile(File file, Charset charset) throws IOException {
        super(file, charset);
        createNameRemap();
    }
    
    private void createNameRemap() {
        stream().map(ZipEntry::getName).forEachOrdered(entryName -> {
            nameRemap.putIfAbsent(normalize(entryName), entryName);
        });
    }
    
    @Override
    public ZipEntry getEntry(String name) {
        String remappedName;
        if (name == null) {
            remappedName = null;
        } else {
            remappedName = nameRemap.getOrDefault(normalize(name), name);
        }
        return super.getEntry(remappedName);
    }
    
    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
