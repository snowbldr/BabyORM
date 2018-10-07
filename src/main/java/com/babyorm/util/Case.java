package com.babyorm.util;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Case {
    CAMEL_CASE("looksLikeThis","", s->Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(), true),
    PASCAL_CASE("LooksLikeThis","", CAMEL_CASE.wordTransform, true),
    KEBAB_CASE("looks-like-this","-", String::toLowerCase, true),
    SNAKE_CASE("looks_like_this","_", String::toLowerCase, true),
    UPPER_KEBAB_CASE("LOOKS-LIKE-THIS","-", String::toUpperCase, true),
    UPPER_SNAKE_CASE("LOOKS_LIKE_THIS","_", String::toUpperCase, true),
    CHOO_CHOO_TRAIN("looks#like#this","#", String::toLowerCase, true),
    UPPER_CHOO_CHOO_TRAIN("LOOKS#LIKE#THIS","#", String::toUpperCase, true),
    ALL_LOWER("lookslikethis","",String::toLowerCase, false),
    ALL_CAPS("LOOKSLIKETHIS","",String::toUpperCase, false);

    private String looksLikeThis, joiner;
    private boolean wordPreserving;
    private Function<String,String> wordTransform;

    public boolean isWordPreserving() {
        return wordPreserving;
    }

    Case(String looksLikeThis, String joiner, Function<String,String> wordTransform, boolean wordPreserving) {
        this.looksLikeThis = looksLikeThis;
        this.joiner = joiner;
        this.wordTransform = wordTransform;
        this.wordPreserving = wordPreserving;
    }

    public String whatDoesItLookLike(){
        return looksLikeThis;
    }

    public static String convert(String input, Case to){
        if(input == null) return null;
        String[] words = input.matches(".*[-_#].*") ? input.split("[-_#]") : input.split("(?=[A-Z][a-z])");
        String result = Arrays.stream(words).map(to.wordTransform).collect(Collectors.joining(to.joiner));
        return to == CAMEL_CASE ? Character.toLowerCase(result.charAt(0)) + result.substring(1) : result;
    }
}
