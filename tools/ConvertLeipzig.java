import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Converts a Leipzig Corpora "*-words.txt" file (lines: rank\tword\tfrequency)
 * into the keyboard's dictionary format ("word frequency", one per line, sorted
 * by frequency desc). Keeps only lowercase Italian alphabetic words, merges
 * case variants by summing frequencies.
 *
 * Run with the source-file launcher (no compile step needed):
 *   java tools/ConvertLeipzig.java <input-words.txt> <output.txt> <topN>
 */
public class ConvertLeipzig {

    // a-z plus the Italian accented vowels the keypad folds for lookup.
    private static boolean isItalianWord(String w) {
        if (w.isEmpty() || w.length() > 20) return false;
        for (int i = 0; i < w.length(); i++) {
            char c = w.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z')
                    || c == 'à' || c == 'á' || c == 'è' || c == 'é'
                    || c == 'ì' || c == 'í' || c == 'ò' || c == 'ó'
                    || c == 'ù' || c == 'ú';
            if (!ok) return false;
        }
        return true;
    }

    public static void main(String[] args) throws IOException {
        Path in = Paths.get(args[0]);
        Path out = Paths.get(args[1]);
        int topN = Integer.parseInt(args[2]);

        Map<String, Long> freq = new HashMap<>();
        long readLines = 0, kept = 0;
        try (BufferedReader r = Files.newBufferedReader(in, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                readLines++;
                String[] p = line.split("\t");
                if (p.length < 3) continue;
                String word = p[1].toLowerCase(Locale.ITALIAN);
                if (!isItalianWord(word)) continue;
                long f;
                try { f = Long.parseLong(p[2].trim()); } catch (NumberFormatException e) { continue; }
                freq.merge(word, f, Long::sum);
                kept++;
            }
        }

        List<Map.Entry<String, Long>> sorted = freq.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .collect(Collectors.toList());

        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            w.write("# Dizionario italiano — Leipzig Corpora Collection (CC BY-4.0)\n");
            w.write("# Fonte: ita_news_2022_100K, Wortschatz Universität Leipzig.\n");
            w.write("# Convertito da tools/ConvertLeipzig.java. Formato: parola frequenza.\n");
            for (Map.Entry<String, Long> e : sorted) {
                w.write(e.getKey());
                w.write(' ');
                w.write(Long.toString(e.getValue()));
                w.write('\n');
            }
        }

        System.out.println("Righe lette: " + readLines);
        System.out.println("Token validi: " + kept + " | parole uniche: " + freq.size());
        System.out.println("Scritte (top " + topN + "): " + Math.min(topN, sorted.size()));
    }
}
