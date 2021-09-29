package lemmini.extract;

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
 * Generic exception class for extraction errors.
 *
 * @author Volker Oth
 */

public class ExtractException extends Exception {
    
    private static final long serialVersionUID = 0x00000001L;
    
    private final boolean canceledByUser;

    /**
     * Constructor.
     */
    public ExtractException() {
        canceledByUser = false;
    }

    /**
     * Constructor.
     * @param s Exception string
     */
    public ExtractException(final String s) {
        super(s);
        canceledByUser = false;
    }
    
    /**
     * Constructor.
     * @param s Exception string
     * @param c whether the operation was canceled by the user
     */
    public ExtractException(final String s, final boolean c) {
        super(s);
        canceledByUser = c;
    }
    
    public boolean isCanceledByUser() {
        return canceledByUser;
    }
}
