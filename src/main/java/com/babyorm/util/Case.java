package com.babyorm.util;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum Case {
    CAMEL_CASE("looksLikeThis",""),
    PASCAL_CASE("LooksLikeThis",""),
    KEBAB_CASE("looks-like-this","-"),
    SNAKE_CASE("looks_like_this","_"),
    UPPER_KEBAB_CASE("LOOKS-LIKE-THIS","-"),
    UPPER_SNAKE_CASE("LOOKS_LIKE_THIS","_");

    private static final Pattern WORD_START_PATTERN = Pattern.compile("(-[^-])|(_[^_])|(A-Z)");
    private String looksLikeThis, joiner;

    Case(String looksLikeThis, String joiner) {
        this.looksLikeThis = looksLikeThis;
        this.joiner = joiner;
    }

    public String whatDoesItLookLike(){
        return looksLikeThis;
    }

    public static String convert(String input, Case to){
        if(input == null){
            return null;
        }
        String result = Arrays.stream(getWords(input)).map(s -> {
            switch (to) {
                case UPPER_KEBAB_CASE:
                case UPPER_SNAKE_CASE:
                    return s.toUpperCase();
                case CAMEL_CASE:
                case PASCAL_CASE:
                    return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
                default:
                    return s.toLowerCase();
            }
        }).collect(Collectors.joining(to.joiner));
        return to == CAMEL_CASE ? Character.toLowerCase(result.charAt(0)) + result.substring(1) : result;
    }

    private static String[] getWords(String input){
        if(input.contains("_")){
            return input.split("_");
        } else if(input.contains("-")){
            return input.split("-");
        } else {
            return input.split("(?=[A-Z][a-z])");
        }
    }
}
