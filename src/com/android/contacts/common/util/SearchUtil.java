/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.contacts.common.util;

import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.google.common.annotations.VisibleForTesting;

import java.util.HashMap;

/**
 * Methods related to search.
 */
public class SearchUtil {

    // the prefix and suffix of the needed highlight words.
    public final static String PREFIX_INDEX = "pre_index";
    public final static String SUFFIX_INDEX = "suf_index";

    public static class MatchedLine {

        public int startIndex = -1;
        public String line;

        @Override
        public String toString() {
            return "MatchedLine{" +
                    "line='" + line + '\'' +
                    ", startIndex=" + startIndex +
                    '}';
        }
    }

    /**
     * Given a string with lines delimited with '\n', finds the matching line to the given
     * substring.
     *
     * @param contents The string to search.
     * @param substring The substring to search for.
     * @return A MatchedLine object containing the matching line and the startIndex of the substring
     * match within that line.
     */
    public static MatchedLine findMatchingLine(String contents, String substring) {
        final MatchedLine matched = new MatchedLine();

        // Snippet may contain multiple lines separated by "\n".
        // Locate the lines of the content that contain the substring.
        int index;
        if (SearchUtil.isPhoneNumber(contents)) {
            Integer i = SearchUtil.numberContains(contents, substring).get(PREFIX_INDEX);
            if (i == null) {
                index = -1;
            } else {
                index = i;
            }

        } else {
            index = SearchUtil.contains(contents, substring);
        }
        if (index != -1) {
            // Match found.  Find the corresponding line.
            int start = index - 1;
            while (start > -1) {
                if (contents.charAt(start) == '\n') {
                    break;
                }
                start--;
            }
            int end = index + 1;
            while (end < contents.length()) {
                if (contents.charAt(end) == '\n') {
                    break;
                }
                end++;
            }
            matched.line = contents.substring(start + 1, end);
            matched.startIndex = index - (start + 1);
        }
        return matched;
    }

    /**
     * Similar to String.contains() with two main differences:
     * <p>
     * 1) Only searches token prefixes.  A token is defined as any combination of letters or
     * numbers.
     * <p>
     * 2) Returns the starting index where the substring is found.
     *
     * @param value The string to search.
     * @param substring The substring to look for.
     * @return The starting index where the substring is found. {@literal -1} if substring is not
     *         found in value.
     */
    @VisibleForTesting
    static int contains(String value, String substring) {
        if (value.length() < substring.length()) {
            return -1;
        }

        // i18n support
        // Generate the code points for the substring once.
        // There will be a maximum of substring.length code points.  But may be fewer.
        // Since the array length is not an accurate size, we need to keep a separate variable.
        final int[] substringCodePoints = new int[substring.length()];
        int substringLength = 0;  // may not equal substring.length()!!
        for (int i = 0; i < substring.length(); ) {
            final int codePoint = Character.codePointAt(substring, i);
            substringCodePoints[substringLength] = codePoint;
            substringLength++;
            i += Character.charCount(codePoint);
        }

        for (int i = 0; i < value.length(); i ++) {
            int numMatch = 0;
            for (int j = i; j < value.length() && numMatch < substringLength; ++numMatch) {
                int valueCp = Character.toLowerCase(value.codePointAt(j));
                int substringCp = substringCodePoints[numMatch];
                if (valueCp != substringCp) {
                    break;
                }
                j += Character.charCount(valueCp);
            }
            if (numMatch == substringLength) {
                return i;
            }
        }
        return -1;
    }

    /**
     *
     * @param value the String to search.
     * @param substring the substring to look for.
     * @return return HashMap with the start and end index.
     */
    public static HashMap<String,Integer> numberContains(String value, String substring) {
        substring = PhoneNumberUtils.normalizeNumber(substring);
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        if (value.length() < substring.length()) {
            return map;
        }

        // i18n support
        // Generate the code points for the substring once.
        // There will be a maximum of substring.length code points.  But may be fewer.
        // Since the array length is not an accurate size, we need to keep a separate variable.
        final int[] substringCodePoints = new int[substring.length()];
        int substringLength = 0;  // may not equal substring.length()!!
        for (int i = 0; i < substring.length(); ) {
            final int codePoint = Character.codePointAt(substring, i);
            substringCodePoints[substringLength] = codePoint;
            substringLength++;
            i += Character.charCount(codePoint);
        }

        for (int i = 0; i < value.length(); i++) {
            int valueCp = Character.toLowerCase(value.codePointAt(i));
            while (!Character.isDigit(valueCp) && i < value.length()) {
                i++;
                valueCp = Character.toLowerCase(value.codePointAt(i));
            }
            int j;
            int numMatch = 0;
            for (j = i; j < value.length() && numMatch < substringLength; ++numMatch) {
                valueCp = Character.toLowerCase(value.codePointAt(j));
                int substringCp = substringCodePoints[numMatch];
                while (!Character.isDigit(valueCp) && j < value.length()) {
                    j++;
                    valueCp = Character.toLowerCase(value.codePointAt(j));
                }
                if (valueCp != substringCp) {
                    break;
                }
                j += Character.charCount(valueCp);
            }
            if (numMatch == substringLength) {
                map.put(PREFIX_INDEX, i);
                map.put(SUFFIX_INDEX, j);
                return map;
            }
        }
        return map;
    }

    /**
     * Find the start of the next token.  A token is composed of letters and numbers. Any other
     * character are considered delimiters.
     *
     * @param line The string to search for the next token.
     * @param startIndex The index to start searching.  0 based indexing.
     * @return The index for the start of the next token.  line.length() if next token not found.
     */
    @VisibleForTesting
    static int findNextTokenStart(String line, int startIndex) {
        int index = startIndex;

        // If already in token, eat remainder of token.
        while (index <= line.length()) {
            if (index == line.length()) {
                // No more tokens.
                return index;
            }
            final int codePoint = line.codePointAt(index);
            if (!Character.isLetterOrDigit(codePoint)) {
                break;
            }
            index += Character.charCount(codePoint);
        }

        // Out of token, eat all consecutive delimiters.
        while (index <= line.length()) {
            if (index == line.length()) {
                return index;
            }
            final int codePoint = line.codePointAt(index);
            if (Character.isLetterOrDigit(codePoint)) {
                break;
            }
            index += Character.charCount(codePoint);
        }

        return index;
    }

    /**
     * Anything other than letter and numbers are considered delimiters.  Remove start and end
     * delimiters since they are not relevant to search.
     *
     * @param query The query string to clean.
     * @return The cleaned query. Empty string if all characters are cleaned out.
     */
    public static String cleanStartAndEndOfSearchQuery(String query) {
        int start = 0;
        while (start < query.length()) {
            int codePoint = query.codePointAt(start);
            if (Character.isLetterOrDigit(codePoint)) {
                break;
            }
            start += Character.charCount(codePoint);
        }

        if (start == query.length()) {
            // All characters are delimiters.
            return "";
        }

        int end = query.length() - 1;
        while (end > -1) {
            if (Character.isLowSurrogate(query.charAt(end))) {
                // Assume valid i18n string.  There should be a matching high surrogate before it.
                end--;
            }
            int codePoint = query.codePointAt(end);
            if (Character.isLetterOrDigit(codePoint)) {
                break;
            }
            end--;
        }

        // end is a letter or digit.
        return query.substring(start, end + 1);
    }

    public static boolean isPhoneNumber(String query) {
        if (TextUtils.isEmpty(query)) {
            return false;
        }
        // Assume a phone number if it has at least 1 digit.
        return countPhoneNumberDigits(query) > 0;
    }

    private static int countPhoneNumberDigits(String query) {
        int numDigits = 0;
        int len = query.length();
        for (int i = 0; i < len; i++) {
            char c = query.charAt(i);
            if (Character.isDigit(c)) {
                numDigits ++;
            } else if (c == '*' || c == '#' || c == 'N' || c == '.' || c == ';'
                    || c == '-' || c == '(' || c == ')' || c == ' ') {
                // Carry on.
            } else if (c == '+' && numDigits == 0) {
                // Plus sign before any digits is OK.
            } else {
                return 0;  // Not a phone number.
            }
        }
        return numDigits;
    }
}
