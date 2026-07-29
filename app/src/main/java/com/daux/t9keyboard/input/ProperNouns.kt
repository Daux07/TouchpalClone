package com.daux.t9keyboard.input

/**
 * Words that are written with a capital wherever they appear.
 *
 * **Deliberately narrow.** The corpus cannot help here: `tools/ConvertLeipzig.java`
 * lowercases every word and sums the case variants, so the dictionary no longer knows
 * that "roma" was "Roma". The proper fix is to regenerate it keeping, per word, the share
 * of occurrences that were capitalised — evidence instead of a hand-written list — and
 * that is the planned route.
 *
 * Until then this list holds only names that are **not also ordinary Italian words**.
 * First names are left out entirely for that reason: Rosa, Viola, Bianca, Vera and Marco
 * would capitalise a flower, a colour and a currency. A wrong capital is more annoying
 * than a missing one, so the list errs towards missing.
 *
 * Months and weekdays are lowercase in Italian and simply are not here.
 */
object ProperNouns {

    private val CITIES = setOf(
        "roma", "milano", "napoli", "torino", "palermo", "genova", "bologna", "firenze",
        "bari", "catania", "venezia", "verona", "messina", "padova", "trieste", "brescia",
        "parma", "modena", "perugia", "livorno", "cagliari", "foggia", "salerno", "rimini",
        "siracusa", "pescara", "bergamo", "vicenza", "bolzano", "novara", "ancona",
        "udine", "lecce", "pisa", "siena", "arezzo", "ravenna", "ferrara", "sassari",
        "trento", "aosta", "campobasso", "catanzaro", "cosenza", "taranto", "brindisi",
        "pordenone", "treviso", "varese", "cremona", "mantova", "piacenza", "reggio",
        "londra", "parigi", "berlino", "madrid", "barcellona", "vienna", "amsterdam",
        "bruxelles", "lisbona", "atene", "praga", "budapest", "varsavia", "mosca",
        "istanbul", "lussemburgo", "monaco", "zurigo", "ginevra"
    )

    private val PLACES = setOf(
        "italia", "francia", "germania", "spagna", "portogallo", "grecia", "svizzera",
        "austria", "belgio", "olanda", "danimarca", "svezia", "norvegia", "finlandia",
        "polonia", "russia", "ucraina", "turchia", "egitto", "marocco", "tunisia",
        "algeria", "brasile", "argentina", "messico", "canada", "giappone", "cina",
        "india", "australia", "irlanda", "islanda", "croazia", "slovenia", "romania",
        "bulgaria", "serbia", "albania", "europa", "africa", "asia", "america", "oceania",
        "lombardia", "toscana", "sicilia", "sardegna", "puglia", "calabria", "campania",
        "piemonte", "liguria", "lazio", "umbria", "abruzzo", "molise", "basilicata",
        "friuli", "trentino", "sardo", "mediterraneo", "adriatico", "tirreno", "alpi",
        "appennini", "tevere", "arno", "sicilia"
    )

    private val HOLIDAYS = setOf(
        "natale", "pasqua", "pasquetta", "capodanno", "ferragosto", "epifania",
        "befana", "quaresima", "avvento", "halloween"
    )

    private val ALL = CITIES + PLACES + HOLIDAYS

    fun isProperNoun(word: String): Boolean = word.lowercase() in ALL

    /** [word] as it should appear in the text: capitalised when it is a proper noun. */
    fun display(word: String): String =
        if (isProperNoun(word)) word.replaceFirstChar { it.uppercaseChar() } else word
}
