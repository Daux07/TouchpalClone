import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Builds the keyboard's Italian dictionary by merging two corpora, because neither one
 * alone describes the language people type into a phone.
 *
 * <ul>
 *   <li><b>OpenSubtitles</b> (frequency list from hermitdave/FrequencyWords, CC BY-SA 4.0)
 *       is film and television dialogue: the closest public corpus to conversation. It is
 *       where "ciao", "beh", "boh" and "davvero" live — in the news corpus "ciao" occurs
 *       27 times, here 225.358.</li>
 *   <li><b>Leipzig news</b> (ita_news_2022_100K, CC BY-4.0) supplies the vocabulary that
 *       dialogue lacks — institutions, geography, formal registers — and, crucially, it
 *       is the only one of the two that <b>preserves capitalisation</b>, so it is what
 *       tells us "Roma" is a name and "rosa" is a flower.</li>
 * </ul>
 *
 * The two frequency scales are unrelated (millions of tokens against thousands), so each
 * is converted to <b>occurrences per million</b> before blending. Dialogue is weighted
 * more heavily than news: this is a keyboard for writing messages, not articles.
 *
 * Output: "word weight [P]" per line, sorted by weight descending, where P marks a proper
 * noun — a word capitalised in at least {@link #PROPER_NOUN_SHARE} of its occurrences in
 * the corpus that records case.
 *
 * Run with the source-file launcher (no compile step needed):
 *   java tools/BuildDictionary.java <subtitles.txt> <leipzig-words.txt> <output.txt> <topN>
 */
public class BuildDictionary {

    /** How much of the final ranking comes from dialogue rather than from news. */
    private static final double SUBTITLE_SHARE = 0.70;

    /** Share of capitalised occurrences above which a word is treated as a name. */
    private static final double PROPER_NOUN_SHARE = 0.90;

    /** Below this many occurrences the share is noise, not evidence. */
    private static final long PROPER_NOUN_MIN_FREQUENCY = 30;

    /**
     * Dialogue is transcribed speech, so it carries typos and foreign lines. A word seen
     * only a handful of times in millions of subtitles is not Italian vocabulary.
     */
    private static final long SUBTITLE_MIN_FREQUENCY = 5;

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

    /** Reads "word frequency" lines (OpenSubtitles format, already lowercase). */
    private static Map<String, Long> readSubtitles(Path path) throws IOException {
        Map<String, Long> freq = new HashMap<>();
        try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.trim().split("\\s+");
                if (p.length < 2) continue;
                String word = p[0].toLowerCase(Locale.ITALIAN);
                if (!isItalianWord(word)) continue;
                long f;
                try { f = Long.parseLong(p[1]); } catch (NumberFormatException e) { continue; }
                if (f < SUBTITLE_MIN_FREQUENCY) continue;
                freq.merge(word, f, Long::sum);
            }
        }
        return freq;
    }

    /** Reads Leipzig "rank\tword\tfrequency", keeping the capitalisation evidence. */
    private static void readLeipzig(Path path, Map<String, Long> freq, Map<String, Long> capitalised)
            throws IOException {
        try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\t");
                if (p.length < 3) continue;
                String raw = p[1];
                String word = raw.toLowerCase(Locale.ITALIAN);
                if (!isItalianWord(word)) continue;
                long f;
                try { f = Long.parseLong(p[2].trim()); } catch (NumberFormatException e) { continue; }
                freq.merge(word, f, Long::sum);
                if (Character.isUpperCase(raw.charAt(0))) capitalised.merge(word, f, Long::sum);
            }
        }
    }

    private static long total(Map<String, Long> freq) {
        long sum = 0;
        for (long v : freq.values()) sum += v;
        return sum;
    }

    public static void main(String[] args) throws IOException {
        Path subsPath = Paths.get(args[0]);
        Path leipzigPath = Paths.get(args[1]);
        Path out = Paths.get(args[2]);
        int topN = Integer.parseInt(args[3]);

        Map<String, Long> subs = readSubtitles(subsPath);
        Map<String, Long> news = new HashMap<>();
        Map<String, Long> capitalised = new HashMap<>();
        readLeipzig(leipzigPath, news, capitalised);

        double subsTotal = total(subs);
        double newsTotal = total(news);

        // Per million, so the two corpora can be compared at all, then blended.
        Set<String> words = new HashSet<>(subs.keySet());
        words.addAll(news.keySet());
        Map<String, Long> weight = new HashMap<>();
        for (String w : words) {
            double s = subs.getOrDefault(w, 0L) / subsTotal * 1_000_000;
            double n = news.getOrDefault(w, 0L) / newsTotal * 1_000_000;
            long blended = Math.round(SUBTITLE_SHARE * s + (1 - SUBTITLE_SHARE) * n);
            if (blended > 0) weight.put(w, blended);
        }

        List<Map.Entry<String, Long>> sorted = weight.entrySet().stream()
                .sorted((a, b) -> {
                    int byWeight = Long.compare(b.getValue(), a.getValue());
                    return byWeight != 0 ? byWeight : a.getKey().compareTo(b.getKey());
                })
                .limit(topN)
                .collect(Collectors.toList());

        int properNouns = 0;
        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            w.write("# Dizionario italiano per tastiera — due corpora fusi.\n");
            w.write("# Dialoghi: OpenSubtitles via hermitdave/FrequencyWords (CC BY-SA 4.0), peso 70%.\n");
            w.write("# Prosa: Leipzig ita_news_2022_100K (CC BY-4.0), peso 30% + maiuscole.\n");
            w.write("# Costruito da tools/BuildDictionary.java.\n");
            w.write("# Formato: parola peso(occorrenze per milione) [P], P = nome proprio.\n");
            for (Map.Entry<String, Long> e : sorted) {
                String word = e.getKey();
                long newsTotalForWord = news.getOrDefault(word, 0L);
                long upper = capitalised.getOrDefault(word, 0L);
                boolean proper = newsTotalForWord >= PROPER_NOUN_MIN_FREQUENCY
                        && (double) upper / newsTotalForWord >= PROPER_NOUN_SHARE;
                if (proper) properNouns++;

                w.write(word);
                w.write(' ');
                w.write(Long.toString(e.getValue()));
                if (proper) w.write(" P");
                w.write('\n');
            }
        }

        System.out.println("Dialoghi: " + subs.size() + " parole | prosa: " + news.size());
        System.out.println("Unione: " + words.size() + " | scritte (top " + topN + "): " + sorted.size());
        System.out.println("Nomi propri riconosciuti: " + properNouns);
    }
}
