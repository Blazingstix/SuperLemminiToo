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

/**
 *
 * @author Ryan
 */
public interface Resource {
    
    boolean exists();
    
    String getFileName();
    
    String getOriginalPath();
    
    Resource getSibling(String sibling);
    
    InputStream getInputStream() throws IOException;
    
    BufferedReader getBufferedReader() throws IOException;
    
    byte[] readAllBytes() throws IOException;
}
