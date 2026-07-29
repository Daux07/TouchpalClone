import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Converts a Leipzig Corpora "*-words.txt" file (lines: rank\tword\tfrequency)
 * into the keyboard's dictionary format, one entry per line, sorted by frequency
 * descending. Keeps only Italian alphabetic words and merges case variants.
 *
 * Output format: "word frequency" plus a trailing " P" on words the corpus says are
 * proper nouns — see below.
 *
 * PROPER NOUNS. The case variants are merged, but not before measuring them: for each
 * word we keep the share of occurrences that were capitalised. Above {@link
 * #PROPER_NOUN_SHARE} the word is one that Italian writes with a capital wherever it
 * falls, and the keyboard capitalises it by itself.
 *
 * The measurement is what makes this trustworthy where a hand-written list is not. In
 * this corpus "Roma" is 748 against one "roma", and "Milano" and "Pasqua" are at 100% —
 * while "rosa" is 17% capitalised, "viola" 27% and "bianca" 44%, so the flower, the
 * colour and the adjective are left alone even though each is also a first name. The
 * sentence-initial capitals that inflate every word reach only ~20% ("il" 18%, "quando"
 * 19%), comfortably below the line; months and weekdays sit at 5%.
 *
 * {@link #PROPER_NOUN_MIN_FREQUENCY} keeps a word that appears three times, twice
 * capitalised, from counting as evidence of anything.
 *
 * Run with the source-file launcher (no compile step needed):
 *   java tools/ConvertLeipzig.java <input-words.txt> <output.txt> <topN>
 */
public class ConvertLeipzig {

    /** Share of capitalised occurrences above which a word is treated as a name. */
    private static final double PROPER_NOUN_SHARE = 0.90;

    /** Below this many occurrences the share is noise, not evidence. */
    private static final long PROPER_NOUN_MIN_FREQUENCY = 30;

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

    private static boolean startsCapitalised(String w) {
        if (w.isEmpty()) return false;
        char c = w.charAt(0);
        return Character.isUpperCase(c);
    }

    public static void main(String[] args) throws IOException {
        Path in = Paths.get(args[0]);
        Path out = Paths.get(args[1]);
        int topN = Integer.parseInt(args[2]);

        Map<String, Long> freq = new HashMap<>();
        Map<String, Long> capitalised = new HashMap<>();
        long readLines = 0, kept = 0;
        try (BufferedReader r = Files.newBufferedReader(in, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                readLines++;
                String[] p = line.split("\t");
                if (p.length < 3) continue;
                String raw = p[1];
                String word = raw.toLowerCase(Locale.ITALIAN);
                if (!isItalianWord(word)) continue;
                long f;
                try { f = Long.parseLong(p[2].trim()); } catch (NumberFormatException e) { continue; }
                freq.merge(word, f, Long::sum);
                if (startsCapitalised(raw)) capitalised.merge(word, f, Long::sum);
                kept++;
            }
        }

        List<Map.Entry<String, Long>> sorted = freq.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .collect(Collectors.toList());

        int properNouns = 0;
        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            w.write("# Dizionario italiano — Leipzig Corpora Collection (CC BY-4.0)\n");
            w.write("# Fonte: ita_news_2022_100K, Wortschatz Universität Leipzig.\n");
            w.write("# Convertito da tools/ConvertLeipzig.java.\n");
            w.write("# Formato: parola frequenza [P], dove P = nome proprio (scritto\n");
            w.write("# maiuscolo in almeno il 90% delle occorrenze del corpus).\n");
            for (Map.Entry<String, Long> e : sorted) {
                String word = e.getKey();
                long total = e.getValue();
                long upper = capitalised.getOrDefault(word, 0L);
                boolean proper = total >= PROPER_NOUN_MIN_FREQUENCY
                        && (double) upper / total >= PROPER_NOUN_SHARE;
                if (proper) properNouns++;

                w.write(word);
                w.write(' ');
                w.write(Long.toString(total));
                if (proper) w.write(" P");
                w.write('\n');
            }
        }

        System.out.println("Righe lette: " + readLines);
        System.out.println("Token validi: " + kept + " | parole uniche: " + freq.size());
        System.out.println("Scritte (top " + topN + "): " + Math.min(topN, sorted.size()));
        System.out.println("Nomi propri riconosciuti: " + properNouns);
    }
}
