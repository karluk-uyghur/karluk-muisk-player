package com.example.data.media

import java.text.Normalizer
import java.util.Locale

/**
 * Filters out DJ mixes, remixes, instrumental tracks, and musical-instrument / non-Nasheed music
 * so the player focuses strictly on Nasheeds, vocal melodies, and recitations.
 */
object AudioContentFilter {

    private val DIACRITICS_REGEX = Regex("\\p{M}+")

    private val NO_MUSIC_PHRASES = listOf(
        "no music",
        "without music",
        "no instruments",
        "without instruments",
        "vocals only",
        "vocal only",
        "voice only",
        "a capella",
        "a cappella",
        "acapella",
        "acappella",
        "duff only",
        "daf only",
        "مۇزىكىسىز",
        "چالغۇسىز",
        "پەقەت ئاۋاز",
        "بدون موسيقى",
        "بدون موسيقي",
        "بدون ايقاع",
        "بدون إيقاع",
        "بدون معازف",
        "بدون آلات",
        "muziksiz",
        "calgisiz",
        "enstrumansiz"
    )

    private val NASHEED_ALLOW_TOKENS = listOf(
        "nasheed",
        "nasheeds",
        "nashid",
        "nasyid",
        "neshid",
        "nesid",
        "neside",
        "anashid",
        "anasheed",
        "ilahi",
        "ilahiler",
        "ezan",
        "adhan",
        "azan",
        "quran",
        "qur'an",
        "kuran",
        "tilawat",
        "tilavet",
        "qiraat",
        "naat",
        "munajat",
        "zikr",
        "dhikr",
        "salawat",
        "salavat",
        "hamd",
        "ahang",
        "نەشىد",
        "نەشىدلەر",
        "ئاھاڭ",
        "قۇرئان",
        "تىلاۋەت",
        "مۇناجات",
        "زېكىر",
        "دۇئا",
        "ئەزان",
        "نائات",
        "نشيد",
        "أناشيد",
        "اناشيد",
        "أنشودة",
        "انشودة",
        "قرآن",
        "تلاوة",
        "ابتهال",
        "مديح",
        "ذكر",
        "أذان"
    )

    // Always reject regardless of Nasheed words
    private val HARD_DJ_AND_INSTRUMENTAL_PHRASES = listOf(
        "dj",
        "deejay",
        "remix",
        "rmx",
        "club mix",
        "extended mix",
        "radio edit",
        "party mix",
        "mashup",
        "megamix",
        "bootleg",
        "nightcore",
        "bass boosted",
        "bassboost",
        "8d audio",
        "edm",
        "techno",
        "trance",
        "deep house",
        "tech house",
        "electro house",
        "progressive house",
        "house music",
        "electro",
        "dubstep",
        "trap music",
        "trap beat",
        "type beat",
        "drum and bass",
        "dnb",
        "hardstyle",
        "psytrance",
        "phonk",
        "synthwave",
        "hip hop",
        "hip-hop",
        "hiphop",
        "rap",
        "r&b",
        "rnb",
        "heavy metal",
        "death metal",
        "jazz",
        "blues",
        "reggae",
        "disco",
        "funk",
        "salsa",
        "tango",
        "waltz",
        "dance music",
        "lofi",
        "lo-fi",
        "chillhop",
        "beatbox",
        "instrumental",
        "karaoke",
        "minus one",
        "backing track",
        "soundtrack",
        "ost",
        "bgm",
        "oyun havasi",
        "halay",
        "enstrumantal",
        "enstruman",
        "calgi",
        "calgili",
        "piano",
        "piano solo",
        "guitar",
        "guitar solo",
        "violin",
        "cello",
        "flute",
        "saxophone",
        "sax",
        "trumpet",
        "accordion",
        "synthesizer",
        "synth",
        "drum solo",
        "drums",
        "percussion",
        "orchestra",
        "symphony",
        "concerto",
        "sonata",
        "overture",
        "ukulele",
        "banjo",
        "mandolin",
        "clarinet",
        "harpsichord",
        "baglama",
        "keman",
        "gitar",
        "piyano",
        "bateri",
        "davul",
        "zurna",
        "darbuka",
        "ney solo",
        "kanun",
        "saz",
        "دىجى",
        "دىجېي",
        "دى جى",
        "رېمىكىس",
        "چالغۇ",
        "چالغۇلۇق",
        "ساز",
        "دۇتتار",
        "راۋاب",
        "غىجەك",
        "چاڭ",
        "تەمبۇر",
        "داپ",
        "سۇناي",
        "ناغرا",
        "پىئانىنو",
        "گىتتار",
        "ئىسكىرىپكا",
        "دي جي",
        "ريمكس",
        "ريميكس",
        "توزيع موسيقي",
        "مهرجان",
        "مهرجانات",
        "دبكة",
        "عزف",
        "آلات موسيقية",
        "الات موسيقية",
        "بيانو",
        "جيتار",
        "قيتار",
        "كمان",
        "عود",
        "قانون",
        "أورغ",
        "اورغ",
        "طبل",
        "راب",
        "هيب هوب"
    )

    // Music / song / general genre tokens rejected unless explicitly negated ("no music") or benign Nasheed tag
    private val GENERAL_MUSIC_TOKENS = listOf(
        "music",
        "musical",
        "muzik",
        "muzika",
        "muzikli",
        "pop",
        "pop music",
        "k-pop",
        "kpop",
        "j-pop",
        "jpop",
        "rock",
        "hard rock",
        "punk",
        "sarki",
        "sarkisi",
        "turku",
        "arabesk",
        "مۇزىكا",
        "مۇزىكىلىق",
        "ناخشا",
        "ناخشىسى",
        "موسيقى",
        "موسيقي",
        "موسيقية",
        "أغنية",
        "اغنية",
        "أغاني",
        "اغاني",
        "طرب"
    )

    // Unambiguous genres that indicate commercial/popular music when present in the genre field
    private val MUSIC_GENRE_VALUES = setOf(
        "pop", "rock", "metal", "hip hop", "hip-hop", "rap", "r&b", "rnb",
        "electronic", "edm", "dance", "house", "techno", "trance", "dubstep",
        "jazz", "blues", "country", "reggae", "disco", "funk", "classical",
        "instrumental", "soundtrack", "latin", "salsa", "club", "dj", "remix",
        "folk", "indie", "alternative", "soul", "punk", "trap", "phonk", "lofi",
        "arabesk", "turku", "muzik", "sarki"
    )

    private val GENERIC_SYSTEM_FOLDER_NAMES = setOf(
        "music", "audio", "download", "downloads", "documents", "device",
        "external", "saf_imports", "recordings", "podcasts", "0", "emulated", "storage"
    )

    private val regexCache = HashMap<String, Regex>()

    fun isExcludedMusicTrack(
        title: String,
        artist: String = "",
        album: String = "",
        genre: String? = null,
        displayName: String = "",
        folderPath: String? = null
    ): Boolean {
        val cleanFolderSubnames = extractSignificantFolderSegments(folderPath)
        val combinedPrimary = buildString {
            append(title).append(' ')
            append(displayName).append(' ')
            append(artist).append(' ')
            append(album).append(' ')
            if (!genre.isNullOrBlank()) append(genre).append(' ')
            append(cleanFolderSubnames)
        }

        val normAll = normalizeText(combinedPrimary)
        if (normAll.isBlank()) return false

        val normTitleAndFile = normalizeText("$title $displayName")
        val normGenre = normalizeText(genre.orEmpty())

        // 1. Check strict DJ / remix / electronic / instrument blockers across all metadata
        if (containsAnyToken(normAll, HARD_DJ_AND_INSTRUMENTAL_PHRASES)) {
            return true
        }

        // 2. Reject explicit non-Nasheed music genres
        if (normGenre.isNotBlank() && isMusicGenre(normGenre)) {
            // Only keep if title/file explicitly states vocal-only or nasheed
            val hasExplicitNasheed = containsAnyToken(normTitleAndFile, NASHEED_ALLOW_TOKENS)
            val hasNoMusicPhrase = containsAnyPhrase(normAll, NO_MUSIC_PHRASES)
            if (!hasExplicitNasheed && !hasNoMusicPhrase) {
                return true
            }
        }

        // 3. Respect explicit "no music / vocals only / مۇزىكىسىز / بدون موسيقى" phrases
        if (containsAnyPhrase(normAll, NO_MUSIC_PHRASES)) {
            return false
        }

        // 4. Reject tracks explicitly marked as music, pop/rock songs, etc.
        if (containsAnyToken(normTitleAndFile, GENERAL_MUSIC_TOKENS)) {
            return true
        }

        val normArtistAlbum = normalizeText("$artist $album $cleanFolderSubnames")
        if (containsAnyToken(normArtistAlbum, GENERAL_MUSIC_TOKENS)) {
            // Allow if title/file explicitly names a Nasheed / Quran / Ilahi / Ahang
            val hasNasheedInTitle = containsAnyToken(normTitleAndFile, NASHEED_ALLOW_TOKENS)
            if (!hasNasheedInTitle) {
                return true
            }
        }

        return false
    }

    private fun isMusicGenre(normGenre: String): Boolean {
        if (normGenre in MUSIC_GENRE_VALUES) return true
        return MUSIC_GENRE_VALUES.any { token -> containsWord(normGenre, token) }
    }

    private fun extractSignificantFolderSegments(folderPath: String?): String {
        if (folderPath.isNullOrBlank() || folderPath.startsWith("content://")) return ""
        return folderPath
            .split('/', '\\')
            .map { it.trim() }
            .filter { segment ->
                segment.isNotBlank() && normalizeText(segment) !in GENERIC_SYSTEM_FOLDER_NAMES
            }
            .joinToString(" ")
    }

    internal fun normalizeText(input: String): String {
        if (input.isBlank()) return ""
        val turkishNormalized = input
            .replace('İ', 'i')
            .replace('I', 'i')
            .replace('ı', 'i')
            .replace('Ş', 's').replace('ş', 's')
            .replace('Ğ', 'g').replace('ğ', 'g')
            .replace('Ü', 'u').replace('ü', 'u')
            .replace('Ö', 'o').replace('ö', 'o')
            .replace('Ç', 'c').replace('ç', 'c')
            // Arabic presentation/hamza variants normalization
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
        val decomposed = Normalizer.normalize(turkishNormalized, Normalizer.Form.NFKD)
        return DIACRITICS_REGEX.replace(decomposed, "")
            .lowercase(Locale.ROOT)
            .replace('_', ' ')
            .replace('-', ' ')
            .replace('.', ' ')
            .replace('/', ' ')
            .replace('\\', ' ')
            .replace('(', ' ')
            .replace(')', ' ')
            .replace('[', ' ')
            .replace(']', ' ')
            .replace('{', ' ')
            .replace('}', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun containsAnyPhrase(normalized: String, phrases: List<String>): Boolean {
        return phrases.any { phrase ->
            val normPhrase = normalizeText(phrase)
            normPhrase.isNotBlank() && containsWord(normalized, normPhrase)
        }
    }

    private fun containsAnyToken(normalized: String, tokens: List<String>): Boolean {
        return tokens.any { token ->
            val normToken = normalizeText(token)
            normToken.isNotBlank() && containsWord(normalized, normToken)
        }
    }

    private fun containsWord(normalized: String, token: String): Boolean {
        val regex = synchronized(regexCache) {
            regexCache.getOrPut(token) {
                Regex("(?<![\\p{L}\\p{N}])${Regex.escape(token)}(?![\\p{L}\\p{N}])")
            }
        }
        return regex.containsMatchIn(normalized)
    }
}
