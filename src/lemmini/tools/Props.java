package lemmini.tools;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Properties;
import lemmini.game.Core;
import lemmini.game.Resource;

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
     * Get string property from the first Props object that contains it
     * @param pCollection Collection of Props objects to search
     * @param key Name of the key to get value for
     * @param def Default value in case key is not found in any Props objects
     * @return Value of key as String
     */
    public static String get(final Collection<? extends Props> pCollection, final String key, final String def) {
        for (Props p : pCollection) {
            if (p.containsKey(key)) {
                return p.get(key, def);
            }
        }
        return def;
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
     * Get string array property from the first Props object that contains it
     * @param pCollection Collection of Props objects to search
     * @param key Name of the key to get value for
     * @param def Default value in case key is not found in any Props objects
     * @return Value of key as array of strings
     */
    public static String[] getArray(final Collection<? extends Props> pCollection, final String key, final String[] def) {
        for (Props p : pCollection) {
            if (p.containsKey(key)) {
                return p.getArray(key, def);
            }
        }
        return def;
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
     * Get integer property from the first Props object that contains it
     * @param pCollection Collection of Props objects to search
     * @param key Name of the key to get value for
     * @param def Default value in case key is not found in any Props objects
     * @return Value of key as int
     */
    public static int getInt(final Collection<? extends Props> pCollection, final String key, final int def) {
        for (Props p : pCollection) {
            if (p.containsKey(key)) {
                return p.getInt(key, def);
            }
        }
        return def;
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
     * Get integer array property from the first Props object that contains it
     * @param pCollection Collection of Props objects to search
     * @param key Name of the key to get value for
     * @param def Default value in case key is not found in any Props objects
     * @return Value of key as array of ints
     */
    public static int[] getIntArray(final Collection<? extends Props> pCollection, final String key, final int[] def) {
        for (Props p : pCollection) {
            if (p.containsKey(key)) {
                return p.getIntArray(key, def);
            }
        }
        return def;
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
        s = s.trim();
        if (s.equalsIgnoreCase("Infinity")) {
            s = "Infinity";
        } else if (s.equalsIgnoreCase("+Infinity")) {
            s = "+Infinity";
        } else if (s.equalsIgnoreCase("-Infinity")) {
            s = "-Infinity";
        }
        return Double.parseDouble(s);
    }

    /**
     * Get double property from the first Props object that contains it
     * @param pCollection Collection of Props objects to search
     * @param key Name of the key to get value for
     * @param def Default value in case key is not found in any Props objects
     * @return value of key as double
     */
    public static double getDouble(final Collection<? extends Props> pCollection, final String key, final double def) {
        for (Props p : pCollection) {
            if (p.containsKey(key)) {
                return p.getDouble(key, def);
            }
        }
        return def;
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
            members[i] = members[i].trim();
            if (members[i].equalsIgnoreCase("Infinity")) {
                members[i] = "Infinity";
            } else if (members[i].equalsIgnoreCase("+Infinity")) {
                members[i] = "+Infinity";
            } else if (members[i].equalsIgnoreCase("-Infinity")) {
                members[i] = "-Infinity";
            }
            ret[i] = Double.parseDouble(members[i]);
        }

        return ret;
    }

    /**
     * Get double array property from the first Props object that contains it
     * @param pCollection Collection of Props objects to search
     * @param key Name of the key to get value for
     * @param def Default value in case key is not found in any Props objects
     * @return Value of key as array of doubles
     */
    public static double[] getDoubleArray(final Collection<? extends Props> pCollection, final String key, final double[] def) {
        for (Props p : pCollection) {
            if (p.containsKey(key)) {
                return p.getDoubleArray(key, def);
            }
        }
        return def;
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
     * Get boolean property from the first Props object that contains it
     * @param pCollection Collection of Props objects to search
     * @param key Name of the key to get value for
     * @param def Default value in case key is not found in any Props objects
     * @return Value of key as boolean
     */
    public static boolean getBoolean(final Collection<? extends Props> pCollection, final String key, final boolean def) {
        for (Props p : pCollection) {
            if (p.containsKey(key)) {
                return p.getBoolean(key, def);
            }
        }
        return def;
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
     * Get boolean array property from the first Props object that contains it
     * @param pCollection Collection of Props objects to search
     * @param key Name of the key to get value for
     * @param def Default value in case key is not found in any Props objects
     * @return Value of key as array of booleans
     */
    public static boolean[] getBooleanArray(final Collection<? extends Props> pCollection, final String key, final boolean[] def) {
        for (Props p : pCollection) {
            if (p.containsKey(key)) {
                return p.getBooleanArray(key, def);
            }
        }
        return def;
    }
    
    public boolean containsKey(final String key) {
        return hash.containsKey(key);
    }

    /**
     * Save property file
     * @param fname File name of property file
     * @return True if OK, false if exception occurred
     */
    public boolean save(final Path fname) {
        try (Writer w = Files.newBufferedWriter(fname)) {
            return save(w);
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Save property file
     * @param fname File name of property file
     * @return True if OK, false if exception occurred
     */
    public boolean save(final String fname) {
        try (Writer w = Core.resourceTree.newBufferedWriter(fname)) {
            return save(w);
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Save property file
     * @param w Writer to property file
     * @return True if OK, false if exception occurred
     */
    public boolean save(final Writer w) {
        try {
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
        } catch (IOException | NullPointerException e) {
            return false;
        }
    }

    /**
     * Load property file
     * @param fname Name of property file
     * @return True if OK, false if exception occurred
     */
    public boolean load(final String fname) {
        try (Reader r = ToolBox.getBufferedReader(Core.resourceTree.getPath(fname))) {
            hash.load(r);
            return true;
        } catch (IOException | NullPointerException e) {
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
        } catch (IOException | NullPointerException e) {
            return false;
        }
    }
    
    /**
     * Load property file
     * @param res property file resource
     * @return True if OK, false if exception occurred
     */
    public boolean load(final Resource res) {
        try (Reader r = res.getBufferedReader()) {
            hash.load(r);
            return true;
        } catch (IOException | NullPointerException e) {
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
        } catch (IOException | NullPointerException e) {
            return false;
        }
    }
}
