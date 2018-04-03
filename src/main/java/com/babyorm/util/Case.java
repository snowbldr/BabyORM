package com.babyorm.util;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Case {
    CAMEL_CASE("looksLikeThis","", s->Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase()),
    PASCAL_CASE("LooksLikeThis","", CAMEL_CASE.wordTransform),
    KEBAB_CASE("looks-like-this","-", String::toLowerCase),
    SNAKE_CASE("looks_like_this","_", String::toLowerCase),
    UPPER_KEBAB_CASE("LOOKS-LIKE-THIS","-", String::toUpperCase),
    UPPER_SNAKE_CASE("LOOKS_LIKE_THIS","_", String::toUpperCase),
    CHOO_CHOO_TRAIN("looks#like#this","#", String::toLowerCase),
    UPPER_CHOO_CHOO_TRAIN("LOOKS#LIKE#THIS","#", String::toUpperCase);

    private String looksLikeThis, joiner;
    private Function<String,String> wordTransform;

    Case(String looksLikeThis, String joiner, Function<String,String> wordTransform) {
        this.looksLikeThis = looksLikeThis;
        this.joiner = joiner;
        this.wordTransform = wordTransform;
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
