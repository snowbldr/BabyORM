package com.babyorm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Case {
    CAMEL_CASE("looksLikeThis"),
    PASCAL_CASE("LooksLikeThis"),
    SNAKE_CASE("looks-like-this");

    private static final Pattern WORD_START_PATTERN = Pattern.compile("(-[a-z])|(_a-z)|(A-Z)");
    private String looksLikeThis;

    Case(String looksLikeThis) {
        this.looksLikeThis = looksLikeThis;
    }

    public String whatDoesItLookLike(){
        return looksLikeThis;
    }

    public String convert(String input, Case to){
        if(input == null){
            return null;
        }
        StringBuilder sb = new StringBuilder();
        Matcher matcher = WORD_START_PATTERN.matcher(input);
        int lastStart = 0;
        while(matcher.find()){
            if(matcher.start() == 0){
                continue;
            }
            String word = input.substring(lastStart, matcher.start());
            switch (to){
                case CAMEL_CASE:

            }
        }
        return sb.toString();
    }
}
