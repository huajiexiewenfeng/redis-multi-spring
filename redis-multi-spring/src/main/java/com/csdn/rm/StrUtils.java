package com.csdn.rm;

public class StrUtils {
    public static int findNthOccurrence(String str, String toFind, int n) {
        if (n <= 0) {
            return -1; // 无效的次数
        }

        int index = -1;
        int count = 0;

        while ((index = str.indexOf(toFind, index + 1)) != -1) {
            count++;
            if (count == n) {
                return index;
            }
        }

        return -1; // 没有找到第N次出现
    }

    public static String ifEmpty(String str, String value) {
        return str == null || str.isEmpty() ? value : str;
    }
}
