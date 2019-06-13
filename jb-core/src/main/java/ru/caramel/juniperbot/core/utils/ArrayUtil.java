/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.caramel.juniperbot.core.utils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unchecked")
public class ArrayUtil {

    public static <T> T deepCopy(Object objectArray) {
        Class arrayClass = objectArray.getClass();

        if (boolean[].class.equals(arrayClass)) {
            boolean[] array = (boolean[]) objectArray;
            return (T) Arrays.copyOf(array, array.length);
        } else if (byte[].class.equals(arrayClass)) {
            byte[] array = (byte[]) objectArray;
            return (T) Arrays.copyOf(array, array.length);
        } else if (short[].class.equals(arrayClass)) {
            short[] array = (short[]) objectArray;
            return (T) Arrays.copyOf(array, array.length);
        } else if (int[].class.equals(arrayClass)) {
            int[] array = (int[]) objectArray;
            return (T) Arrays.copyOf(array, array.length);
        } else if (long[].class.equals(arrayClass)) {
            long[] array = (long[]) objectArray;
            return (T) Arrays.copyOf(array, array.length);
        } else if (float[].class.equals(arrayClass)) {
            float[] array = (float[]) objectArray;
            return (T) Arrays.copyOf(array, array.length);
        } else if (double[].class.equals(arrayClass)) {
            double[] array = (double[]) objectArray;
            return (T) Arrays.copyOf(array, array.length);
        } else if (char[].class.equals(arrayClass)) {
            char[] array = (char[]) objectArray;
            return (T) Arrays.copyOf(array, array.length);
        } else {
            Object[] array = (Object[]) objectArray;
            return (T) Arrays.copyOf(array, array.length);
        }
    }

    public static Object[] wrapArray(Object objectArray) {
        Class arrayClass = objectArray.getClass();

        if (boolean[].class.equals(arrayClass)) {
            boolean[] fromArray = (boolean[]) objectArray;
            Boolean[] array = new Boolean[fromArray.length];
            for (int i = 0; i < fromArray.length; i++) {
                array[i] = fromArray[i];
            }
            return array;
        } else if (byte[].class.equals(arrayClass)) {
            byte[] fromArray = (byte[]) objectArray;
            Byte[] array = new Byte[fromArray.length];
            for (int i = 0; i < fromArray.length; i++) {
                array[i] = fromArray[i];
            }
            return array;
        } else if (short[].class.equals(arrayClass)) {
            short[] fromArray = (short[]) objectArray;
            Short[] array = new Short[fromArray.length];
            for (int i = 0; i < fromArray.length; i++) {
                array[i] = fromArray[i];
            }
            return array;
        } else if (int[].class.equals(arrayClass)) {
            int[] fromArray = (int[]) objectArray;
            Integer[] array = new Integer[fromArray.length];
            for (int i = 0; i < fromArray.length; i++) {
                array[i] = fromArray[i];
            }
            return array;
        } else if (long[].class.equals(arrayClass)) {
            long[] fromArray = (long[]) objectArray;
            Long[] array = new Long[fromArray.length];
            for (int i = 0; i < fromArray.length; i++) {
                array[i] = fromArray[i];
            }
            return array;
        } else if (float[].class.equals(arrayClass)) {
            float[] fromArray = (float[]) objectArray;
            Float[] array = new Float[fromArray.length];
            for (int i = 0; i < fromArray.length; i++) {
                array[i] = fromArray[i];
            }
            return array;
        } else if (double[].class.equals(arrayClass)) {
            double[] fromArray = (double[]) objectArray;
            Double[] array = new Double[fromArray.length];
            for (int i = 0; i < fromArray.length; i++) {
                array[i] = fromArray[i];
            }
            return array;
        } else if (char[].class.equals(arrayClass)) {
            char[] fromArray = (char[]) objectArray;
            Character[] array = new Character[fromArray.length];
            for (int i = 0; i < fromArray.length; i++) {
                array[i] = fromArray[i];
            }
            return array;
        } else {
            return (Object[]) objectArray;
        }
    }

    public static <T> T unwrapArray(Object[] objectArray, Class<T> arrayClass) {

        if (boolean[].class.equals(arrayClass)) {
            boolean[] array = new boolean[objectArray.length];
            for (int i = 0; i < objectArray.length; i++) {
                array[i] = objectArray[i] != null ? (Boolean) objectArray[i] : Boolean.FALSE;
            }
            return (T) array;
        } else if (byte[].class.equals(arrayClass)) {
            byte[] array = new byte[objectArray.length];
            for (int i = 0; i < objectArray.length; i++) {
                array[i] = objectArray[i] != null ? (Byte) objectArray[i] : 0;
            }
            return (T) array;
        } else if (short[].class.equals(arrayClass)) {
            short[] array = new short[objectArray.length];
            for (int i = 0; i < objectArray.length; i++) {
                array[i] = objectArray[i] != null ? (Short) objectArray[i] : 0;
            }
            return (T) array;
        } else if (int[].class.equals(arrayClass)) {
            int[] array = new int[objectArray.length];
            for (int i = 0; i < objectArray.length; i++) {
                array[i] = objectArray[i] != null ? (Integer) objectArray[i] : 0;
            }
            return (T) array;
        } else if (long[].class.equals(arrayClass)) {
            long[] array = new long[objectArray.length];
            for (int i = 0; i < objectArray.length; i++) {
                array[i] = objectArray[i] != null ? (Long) objectArray[i] : 0L;
            }
            return (T) array;
        } else if (float[].class.equals(arrayClass)) {
            float[] array = new float[objectArray.length];
            for (int i = 0; i < objectArray.length; i++) {
                array[i] = objectArray[i] != null ? (Float) objectArray[i] : 0f;
            }
            return (T) array;
        } else if (double[].class.equals(arrayClass)) {
            double[] array = new double[objectArray.length];
            for (int i = 0; i < objectArray.length; i++) {
                array[i] = objectArray[i] != null ? (Double) objectArray[i] : 0d;
            }
            return (T) array;
        } else if (char[].class.equals(arrayClass)) {
            char[] array = new char[objectArray.length];
            for (int i = 0; i < objectArray.length; i++) {
                array[i] = objectArray[i] != null ? (Character) objectArray[i] : 0;
            }
            return (T) array;
        } else if (String[].class.equals(arrayClass)) {
            String[] array = new String[objectArray.length];
            for (int i = 0; i < objectArray.length; i++) {
                array[i] = objectArray[i] != null ? (String) objectArray[i] : null;
            }
            return (T) array;
        } else {
            return (T) objectArray;
        }
    }

    public static <T> T fromString(String string, Class<T> arrayClass) {
        String stringArray = string.replaceAll("[\\[\\]]", "");
        String[] tokens = stringArray.split(",");

        int length = tokens.length;

        if (boolean[].class.equals(arrayClass)) {
            boolean[] array = new boolean[length];
            for (int i = 0; i < tokens.length; i++) {
                array[i] = Boolean.valueOf(tokens[i]);
            }
            return (T) array;
        } else if (byte[].class.equals(arrayClass)) {
            byte[] array = new byte[length];
            for (int i = 0; i < tokens.length; i++) {
                array[i] = Byte.valueOf(tokens[i]);
            }
            return (T) array;
        } else if (short[].class.equals(arrayClass)) {
            short[] array = new short[length];
            for (int i = 0; i < tokens.length; i++) {
                array[i] = Short.valueOf(tokens[i]);
            }
            return (T) array;
        } else if (int[].class.equals(arrayClass)) {
            int[] array = new int[length];
            for (int i = 0; i < tokens.length; i++) {
                array[i] = Integer.valueOf(tokens[i]);
            }
            return (T) array;
        } else if (long[].class.equals(arrayClass)) {
            long[] array = new long[length];
            for (int i = 0; i < tokens.length; i++) {
                array[i] = Long.valueOf(tokens[i]);
            }
            return (T) array;
        } else if (float[].class.equals(arrayClass)) {
            float[] array = new float[length];
            for (int i = 0; i < tokens.length; i++) {
                array[i] = Float.valueOf(tokens[i]);
            }
            return (T) array;
        } else if (double[].class.equals(arrayClass)) {
            double[] array = new double[length];
            for (int i = 0; i < tokens.length; i++) {
                array[i] = Double.valueOf(tokens[i]);
            }
            return (T) array;
        } else if (char[].class.equals(arrayClass)) {
            char[] array = new char[length];
            for (int i = 0; i < tokens.length; i++) {
                array[i] = tokens[i].length() > 0 ? tokens[i].charAt(0) : Character.MIN_VALUE;
            }
            return (T) array;
        } else {
            return (T) tokens;
        }
    }

    public static boolean isEquals(Object firstArray, Object secondArray) {
        if (firstArray.getClass() != secondArray.getClass()) {
            return false;
        }
        Class arrayClass = firstArray.getClass();

        if (boolean[].class.equals(arrayClass)) {
            return Arrays.equals((boolean[]) firstArray, (boolean[]) secondArray);
        } else if (byte[].class.equals(arrayClass)) {
            return Arrays.equals((byte[]) firstArray, (byte[]) secondArray);
        } else if (short[].class.equals(arrayClass)) {
            return Arrays.equals((short[]) firstArray, (short[]) secondArray);
        } else if (int[].class.equals(arrayClass)) {
            return Arrays.equals((int[]) firstArray, (int[]) secondArray);
        } else if (long[].class.equals(arrayClass)) {
            return Arrays.equals((long[]) firstArray, (long[]) secondArray);
        } else if (float[].class.equals(arrayClass)) {
            return Arrays.equals((float[]) firstArray, (float[]) secondArray);
        } else if (double[].class.equals(arrayClass)) {
            return Arrays.equals((double[]) firstArray, (double[]) secondArray);
        } else if (char[].class.equals(arrayClass)) {
            return Arrays.equals((char[]) firstArray, (char[]) secondArray);
        } else {
            return Arrays.equals((Object[]) firstArray, (Object[]) secondArray);
        }
    }

    public static <T> T[] reverse(Class<T[]> clazz, T[] source, Collection<T> collection) {
        if (source == null || source.length == 0) {
            T[] array = (T[]) Array.newInstance(clazz.getComponentType(), collection.size());
            return collection.toArray(array);
        }
        Set<T> result = new HashSet<>(collection);
        result.removeAll(Arrays.asList(source));
        T[] array = (T[]) Array.newInstance(clazz.getComponentType(), result.size());
        return result.toArray(array);
    }

    public static boolean containsIgnoreCase(String[] array, String value) {
        if (array == null || array.length == 0 || value == null) {
            return false;
        }
        for (String item : array) {
            if (value.equalsIgnoreCase(item)) {
                return true;
            }
        }
        return false;
    }
}