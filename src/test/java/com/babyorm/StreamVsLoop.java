package com.babyorm;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Was curious how streams performed vs loops in the iterate n times case like is used so many times in this project.
 * This showed that streams have a large initialization cost on the first run through, but are generally faster thereafter.
 */
public class StreamVsLoop {

    public static void main(String[] args) {
        int iterations = 24;
        Long[] loopResult = new Long[iterations];
        for (int i = 1; i <= iterations; i++) {
            loopResult[i - 1] = iterateNTimesLoop((int) Math.pow(2, i));
        }
        Long[] streamRangeResult = new Long[iterations];
        for (int i = 1; i <= iterations; i++) {
            streamRangeResult[i - 1] = iterateNTimesStreamRange((int) Math.pow(2, i));
        }

        Long[] difference = IntStream.range(0, iterations).mapToLong(i -> loopResult[i] - streamRangeResult[i]).boxed().toArray(Long[]::new);

        System.out.println(
                jsonObject(
                        jsonProp("loop", jsonArray(toMsStringArray(loopResult))),
                        jsonProp("streamRange", jsonArray(toMsStringArray(streamRangeResult))),
                        jsonProp("diff", jsonArray(toMsStringArray(difference)))
                )
        );

    }

    private static String[] toMsStringArray(Long[] results) {
        return Arrays.stream(results).map(StreamVsLoop::toMsString).toArray(String[]::new);
    }

    private static String toMsString(long nanos) {
        return String.format("%.3fms", (nanos / 10000D));
    }

    static String jsonObject(String... props) {
        return "{" + Stream.of(props).map(Object::toString).collect(Collectors.joining(",")) + "}";
    }

    static String jsonArray(Object... array) {
        return "[" + Stream.of(array).map(o -> o instanceof Number ? o.toString() : "\"" + o + "\"").collect(Collectors.joining(",")) + "]";
    }

    static String jsonProp(String propName, String value) {
        return "\"" + propName + "\": " + value + "";
    }

    static long iterateNTimesLoop(int arg) {
        System.nanoTime();
        long start = System.nanoTime();
        for (int i = 0; i < arg; i++) {
            calc(i);
        }
        return System.nanoTime() - start;
    }

    static long iterateNTimesStreamRange(int arg) {
        System.nanoTime();
        long start = System.nanoTime();
        IntStream.range(0, arg).forEach(StreamVsLoop::calc);
        return System.nanoTime() - start;
    }

    static void calc(int i) {
        if (i + (int) (Math.random() * 1000) > 0) {
            Math.random();
        }
    }

}
