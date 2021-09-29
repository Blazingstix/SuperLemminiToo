package lemmini.tools;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

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
 * Property class to ease use of INI files to save/load properties.
 *
 * @author Volker Oth
 */
public class Props {

    /** extended hash to store properties */
    private final Properties hash;
    /** header string */
    private String header;

    /**
     * Constructor
     */
    public Props() {
        hash = new Properties();
        header = new String();
    }

    /**
     * Set the property file header
     * @param h String containing Header information
     */
    public void setHeader(final String h) {
        header = h;
    }

    /**
     * Clear all properties
     */
    public void clear() {
        hash.clear();
    }

    /**
     * Remove key
     * @param key Name of key
     */
    public void remove(final String key) {
        hash.remove(key);
    }

    /**
     * Set string property
     * @param key Name of the key to set value for
     * @param value Value to set
     */
    public void set(final String key, final String value) {
        hash.setProperty(key, value);
    }

    /**
     * Set integer property
     * @param key Name of the key to set value for
     * @param value Value to set
     */
    public void setInt(final String key, final int value) {
        hash.setProperty(key, Integer.toString(value));
    }

    /**
     * Set double property
     * @param key Name of the key to set value for
     * @param value Value to set
     */
    public void setDouble(final String key, final double value) {
        hash.setProperty(key, Double.toString(value));
    }

    /**
     * Set boolean property
     * @param key Name of the key to set value for
     * @param value Value to set
     */
    public void setBoolean(final String key, final boolean value) {
        hash.setProperty(key, Boolean.toString(value));
    }

    /**
     * Get string property
     * @param key Name of the key to get value for
     * @param def Default value in case key is not found
     * @return Value of key as String
     */
    public String get(final String key, final String def) {
        return hash.getProperty(key, def);
    }

    /**
     * Get string array property
     * @param key Name of the key to get value for
     * @param def Default value in case key is not found
     * @return Value of key as array of strings
     */
    public String[] getArray(final String key, final String[] def) {
        String s = hash.getProperty(key);
        if (s == null) {
            return def;
        }
        String[] members = s.split(",");
        // remove trailing and leading spaces
        for (int i = 0; i < members.length; i++) {
            members[i] = members[i].trim();
        }

        return members;
    }

    /**
     * Get integer property
     * @param key Name of the key to get value for
     * @param def Default value in case key is not found
     * @return Value of key as int
     */
    public int getInt(final String key, final int def) {
        String s = hash.getProperty(key);
        if (s == null) {
            return def;
        }
        return ToolBox.parseInt(s.trim());
    }

    /**
     * Get integer array property
     * @param key Name of the key to get value for
     * @param def Default value in case key is not found
     * @return Value of key as array of ints
     */
    public int[] getIntArray(final String key, final int[] def) {
        String s = hash.getProperty(key);
        if (s == null) {
            return def;
        }
        String[] members = s.split(",");

        int[] ret;
        ret = new int[members.length];
        for (int i = 0; i < members.length; i++) {
            ret[i] = ToolBox.parseInt(members[i].trim());
        }

        return ret;
    }

    /**
     * Get double property
     * @param key Name of the key to get value for
     * @param def Default value in case key is not found
     * @return value of key as double
     */
    public double getDouble(final String key, final double def) {
        String s = hash.getProperty(key);
        if (s == null) {
            return def;
        }
        return Double.parseDouble(s);
    }

    /**
     * Get double array property
     * @param key Name of the key to get value for
     * @param def Default value in case key is not found
     * @return Value of key as array of doubles
     */
    public double[] getDoubleArray(final String key, final double[] def) {
        String s = hash.getProperty(key);
        if (s == null) {
            return def;
        }
        String[] members = s.split(",");

        double[] ret;
        ret = new double[members.length];
        for (int i = 0; i < members.length; i++) {
            ret[i] = Double.parseDouble(members[i]);
        }

        return ret;
    }

    /**
     * Get boolean property
     * @param key Name of the key to get value for
     * @param def Default value in case key is not found
     * @return Value of key as boolean
     */
    public boolean getBoolean(final String key, final boolean def) {
        String s = hash.getProperty(key);
        if (s == null) {
            return def;
        }
        return Boolean.parseBoolean(s.trim());
    }
    
    /**
     * Get boolean array property
     * @param key Name of the key to get value for
     * @param def Default value in case key is not found
     * @return Value of key as array of booleans
     */
    public boolean[] getBooleanArray(final String key, final boolean[] def) {
        String s = hash.getProperty(key);
        if (s == null) {
            return def;
        }
        String[] members = s.split(",");

        boolean[] ret;
        ret = new boolean[members.length];
        for (int i = 0; i < members.length; i++) {
            ret[i] = Boolean.parseBoolean(members[i].trim());
        }

        return ret;
    }

    /**
     * Save property file
     * @param fname File name of property file
     * @return True if OK, false if exception occurred
     */
    public boolean save(final Path fname) {
        try (Writer w = Files.newBufferedWriter(fname, StandardCharsets.UTF_8)) {
            hash.store(w, header);
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Load property file
     * @param fname Name of property file
     * @return True if OK, false if exception occurred
     */
    public boolean load(final Path fname) {
        try (Reader r = ToolBox.getBufferedReader(fname)) {
            hash.load(r);
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Load property file
     * @param file URL of property file
     * @return True if OK, false if exception occurred
     */
    public boolean load(final URL file) {
        try (Reader r = ToolBox.getBufferedReader(file)) {
            hash.load(r);
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Load property file
     * @param r Reader for property file
     * @return True if OK, false if exception occurred
     */
    public boolean load(final Reader r) {
        try {
            hash.load(r);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
