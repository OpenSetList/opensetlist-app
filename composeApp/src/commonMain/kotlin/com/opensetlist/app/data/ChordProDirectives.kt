package com.opensetlist.app.data

import com.opensetlist.app.model.CommentStyle

/**
 * Catálogo de diretivas ChordPro conhecidas, com aliases e metadados de renderização.
 *
 * @author ruanitto
 */
object ChordProDirectives {

    /** Categoria funcional de uma diretiva. */
    enum class Kind {
        METADATA,
        COMMENT,
        SECTION_START,
        SECTION_END,
        CHORUS,
        IGNORE
    }

    /** Uma diretiva resolvida, com o tipo, chave de metadado e rótulo de seção. */
    data class Directive(
        val name: String,
        val kind: Kind,
        val metadataKey: String? = null,
        val sectionLabel: String? = null,
        val commentStyle: CommentStyle? = null
    )

    private val ALIASES = mapOf(
        // Preamble
        "new_song" to "new_song", "ns" to "new_song",

        // Meta-data
        "title" to "title", "t" to "title",
        "sorttitle" to "sorttitle",
        "subtitle" to "subtitle", "st" to "subtitle",
        "artist" to "artist",
        "sortartist" to "sortartist",
        "composer" to "composer",
        "lyricist" to "lyricist",
        "copyright" to "copyright",
        "album" to "album",
        "year" to "year",
        "key" to "key", "k" to "key", "tom" to "key",
        "time" to "time", "compasso" to "time", "time_signature" to "time",
        "tempo" to "tempo", "bpm" to "tempo",
        "duration" to "duration", "duracao" to "duration", "duração" to "duration",
        "capo" to "capo",
        "transpose" to "transpose", "trans" to "transpose",
        "youtube" to "youtube", "youtube_url" to "youtube", "url" to "youtube",
        "tag" to "tag",
        "meta" to "meta",

        // Formatting / comments
        "comment" to "comment", "c" to "comment",
        "remark" to "remark",
        "comentario" to "comment", "comentário" to "comment",
        "observacao" to "comment", "observação" to "comment",
        "highlight" to "highlight",
        "comment_italic" to "comment_italic", "ci" to "comment_italic",
        "comment_box" to "comment_box", "cb" to "comment_box",
        "soh" to "start_of_highlight", "start_of_highlight" to "start_of_highlight",
        "eoh" to "end_of_highlight", "end_of_highlight" to "end_of_highlight",

        // Environment sections
        "start_of_chorus" to "start_of_chorus", "soc" to "start_of_chorus",
        "end_of_chorus" to "end_of_chorus", "eoc" to "end_of_chorus",
        "chorus" to "chorus",
        "start_of_verse" to "start_of_verse", "sov" to "start_of_verse",
        "end_of_verse" to "end_of_verse", "eov" to "end_of_verse",
        "start_of_bridge" to "start_of_bridge", "sob" to "start_of_bridge",
        "end_of_bridge" to "end_of_bridge", "eob" to "end_of_bridge",
        "start_of_tab" to "start_of_tab", "sot" to "start_of_tab",
        "end_of_tab" to "end_of_tab", "eot" to "end_of_tab",
        "start_of_grid" to "start_of_grid", "sog" to "start_of_grid",
        "end_of_grid" to "end_of_grid", "eog" to "end_of_grid",

        // Delegated environments
        "start_of_abc" to "start_of_abc",
        "end_of_abc" to "end_of_abc",
        "start_of_ly" to "start_of_ly",
        "end_of_ly" to "end_of_ly",
        "start_of_svg" to "start_of_svg",
        "end_of_svg" to "end_of_svg",
        "start_of_textblock" to "start_of_textblock",
        "end_of_textblock" to "end_of_textblock",

        // Chord diagrams
        "define" to "define",
        "chord" to "chord",

        // Transposition
        "transpose" to "transpose",

        // Fonts, sizes and colours
        "chordfont" to "chordfont", "cf" to "chordfont",
        "chordsize" to "chordsize", "cs" to "chordsize",
        "chordcolour" to "chordcolour", "chordcolor" to "chordcolour",
        "chorusfont" to "chorusfont",
        "chorussize" to "chorussize",
        "choruscolour" to "choruscolour", "choruscolor" to "choruscolour",
        "footerfont" to "footerfont",
        "footersize" to "footersize",
        "footercolour" to "footercolour", "footercolor" to "footercolour",
        "gridfont" to "gridfont",
        "gridsize" to "gridsize",
        "gridcolour" to "gridcolour", "gridcolor" to "gridcolour",
        "tabfont" to "tabfont",
        "tabsize" to "tabsize",
        "tabcolour" to "tabcolour", "tabcolor" to "tabcolour",
        "labelfont" to "labelfont",
        "labelsize" to "labelsize",
        "labelcolour" to "labelcolour", "labelcolor" to "labelcolour",
        "tocfont" to "tocfont",
        "tocsize" to "tocsize",
        "toccolour" to "toccolour", "toccolor" to "toccolour",
        "textfont" to "textfont", "tf" to "textfont",
        "textsize" to "textsize", "ts" to "textsize",
        "textcolour" to "textcolour", "textcolor" to "textcolour",
        "titlefont" to "titlefont",
        "titlesize" to "titlesize",
        "titlecolour" to "titlecolour", "titlecolor" to "titlecolour",

        // Output related
        "new_page" to "new_page", "np" to "new_page",
        "new_physical_page" to "new_physical_page", "npp" to "new_physical_page",
        "column_break" to "column_break", "colb" to "column_break",
        "pagetype" to "pagetype",
        "diagrams" to "diagrams",
        "grid" to "grid", "g" to "grid",
        "no_grid" to "no_grid", "ng" to "no_grid",
        "titles" to "titles",
        "columns" to "columns", "col" to "columns"
    )

    private val DIRECTIVES: Map<String, Directive> = buildMap {
        fun add(name: String, kind: Kind, metadataKey: String? = null, sectionLabel: String? = null, commentStyle: CommentStyle? = null) {
            put(name, Directive(name, kind, metadataKey, sectionLabel, commentStyle))
        }
        add("new_song", Kind.IGNORE)

        add("title", Kind.METADATA, metadataKey = "title")
        add("sorttitle", Kind.METADATA, metadataKey = "sorttitle")
        add("subtitle", Kind.METADATA, metadataKey = "subtitle")
        add("artist", Kind.METADATA, metadataKey = "artist")
        add("sortartist", Kind.METADATA, metadataKey = "sortartist")
        add("composer", Kind.METADATA, metadataKey = "composer")
        add("lyricist", Kind.METADATA, metadataKey = "lyricist")
        add("copyright", Kind.METADATA, metadataKey = "copyright")
        add("album", Kind.METADATA, metadataKey = "album")
        add("year", Kind.METADATA, metadataKey = "year")
        add("key", Kind.METADATA, metadataKey = "key")
        add("time", Kind.METADATA, metadataKey = "time")
        add("tempo", Kind.METADATA, metadataKey = "tempo")
        add("duration", Kind.METADATA, metadataKey = "duration")
        add("capo", Kind.METADATA, metadataKey = "capo")
        add("transpose", Kind.METADATA, metadataKey = "transpose")
        add("youtube", Kind.METADATA, metadataKey = "youtube")
        add("tag", Kind.METADATA, metadataKey = "tag")
        add("meta", Kind.METADATA, metadataKey = "meta")

        add("comment", Kind.COMMENT, commentStyle = CommentStyle.PLAIN)
        add("remark", Kind.COMMENT, commentStyle = CommentStyle.PLAIN)
        add("highlight", Kind.COMMENT, commentStyle = CommentStyle.HIGHLIGHT)
        add("comment_italic", Kind.COMMENT, commentStyle = CommentStyle.ITALIC)
        add("comment_box", Kind.COMMENT, commentStyle = CommentStyle.BOX)
        add("image", Kind.IGNORE)

        add("start_of_chorus", Kind.SECTION_START, sectionLabel = "Chorus")
        add("end_of_chorus", Kind.SECTION_END)
        add("chorus", Kind.CHORUS, sectionLabel = "Chorus")
        add("start_of_verse", Kind.SECTION_START, sectionLabel = "Verse")
        add("end_of_verse", Kind.SECTION_END)
        add("start_of_bridge", Kind.SECTION_START, sectionLabel = "Bridge")
        add("end_of_bridge", Kind.SECTION_END)
        add("start_of_tab", Kind.SECTION_START, sectionLabel = "Tab")
        add("end_of_tab", Kind.SECTION_END)
        add("start_of_grid", Kind.SECTION_START, sectionLabel = "Grid")
        add("end_of_grid", Kind.SECTION_END)
        add("start_of_highlight", Kind.SECTION_START, sectionLabel = "Highlight")
        add("end_of_highlight", Kind.SECTION_END)

        add("start_of_abc", Kind.SECTION_START, sectionLabel = "ABC")
        add("end_of_abc", Kind.SECTION_END)
        add("start_of_ly", Kind.SECTION_START, sectionLabel = "Lilypond")
        add("end_of_ly", Kind.SECTION_END)
        add("start_of_svg", Kind.SECTION_START, sectionLabel = "SVG")
        add("end_of_svg", Kind.SECTION_END)
        add("start_of_textblock", Kind.SECTION_START, sectionLabel = "Text")
        add("end_of_textblock", Kind.SECTION_END)

        add("define", Kind.IGNORE)
        add("chord", Kind.IGNORE)

        add("chordfont", Kind.IGNORE)
        add("chordsize", Kind.IGNORE)
        add("chordcolour", Kind.IGNORE)
        add("chorusfont", Kind.IGNORE)
        add("chorussize", Kind.IGNORE)
        add("choruscolour", Kind.IGNORE)
        add("footerfont", Kind.IGNORE)
        add("footersize", Kind.IGNORE)
        add("footercolour", Kind.IGNORE)
        add("gridfont", Kind.IGNORE)
        add("gridsize", Kind.IGNORE)
        add("gridcolour", Kind.IGNORE)
        add("tabfont", Kind.IGNORE)
        add("tabsize", Kind.IGNORE)
        add("tabcolour", Kind.IGNORE)
        add("labelfont", Kind.IGNORE)
        add("labelsize", Kind.IGNORE)
        add("labelcolour", Kind.IGNORE)
        add("tocfont", Kind.IGNORE)
        add("tocsize", Kind.IGNORE)
        add("toccolour", Kind.IGNORE)
        add("textfont", Kind.IGNORE)
        add("textsize", Kind.IGNORE)
        add("textcolour", Kind.IGNORE)
        add("titlefont", Kind.IGNORE)
        add("titlesize", Kind.IGNORE)
        add("titlecolour", Kind.IGNORE)

        add("new_page", Kind.IGNORE)
        add("new_physical_page", Kind.IGNORE)
        add("column_break", Kind.IGNORE)
        add("pagetype", Kind.IGNORE)
        add("diagrams", Kind.IGNORE)
        add("grid", Kind.IGNORE)
        add("no_grid", Kind.IGNORE)
        add("titles", Kind.IGNORE)
        add("columns", Kind.IGNORE)
    }

    private val EXTENSION_IGNORE = Directive("x_", Kind.IGNORE)

    private val CONDITIONAL_NAME = Regex("^[0-9][0-9,+\\-]*$")

    fun resolve(rawName: String): Directive? {
        var name = rawName.trim()
        if (name.isEmpty()) return null
        if (name.startsWith("x_")) return EXTENSION_IGNORE
        name = name.substringBefore('-').trim().lowercase()
        if (name.isEmpty()) return null
        val canonical = ALIASES[name] ?: return null
        return DIRECTIVES[canonical]
    }

    fun isConditionalName(name: String): Boolean = CONDITIONAL_NAME.matches(name.trim())

    fun sectionLabel(directive: Directive, arg: String?): String {
        if (!arg.isNullOrBlank()) {
            extractLabel(arg)?.let { return it }
            if (!arg.startsWith("@")) return arg.trim()
        }
        return directive.sectionLabel ?: directive.name
    }

    fun extractLabel(arg: String): String? {
        val trimmed = arg.trim()
        if (!trimmed.startsWith("label=")) return null
        var rest = trimmed.removePrefix("label=").trim()
        rest = when {
            rest.startsWith("\"") -> rest.removePrefix("\"").substringBefore("\"")
            rest.startsWith("'") -> rest.removePrefix("'").substringBefore("'")
            else -> rest.substringBefore(" ")
        }
        return rest.trim().ifBlank { null }
    }
}
