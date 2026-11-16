
package eu.openaire.common.author;

import com.ibm.icu.text.Transliterator;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalization of author names, so that the jobs comparing names coming from different sources all
 * agree on what "the same name" is.
 */
public class AuthorNames implements Serializable {

	private static final ThreadLocal<Transliterator> TRANSLITERATOR_THREAD_LOCAL = ThreadLocal
		.withInitial(() -> Transliterator.getInstance("Any-Latin; Latin-ASCII"));

	// compiled once: normalize is called for every author of the graph, and String.replaceAll would
	// compile these on each of those calls
	private static final Pattern TRAILING_DIGITS = Pattern.compile("[\\d\\s]*$");

	private static final Pattern HONORIFICS = Pattern.compile("(?i)\\b(?:Dr|Prof|PhD)\\.?\\b");

	private static final BitSet DELIMITERS_MASK = buildDelims();

	private AuthorNames() {
	}

	private static boolean isAscii(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) > 0x7F) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the given name transliterated to Latin, stripped of its accents, of the common honorifics
	 * and of the trailing digits (DBLP identifiers), lower cased, and with its tokens separated by a
	 * single space.
	 *
	 * @return null when the given name is null
	 */
	public static String normalize(String authorName) {
		if (authorName == null) {
			return null;
		}

		// remove trailing numbers (DBLP identifier )
		authorName = TRAILING_DIGITS.matcher(authorName).replaceAll("");

		// transliterate. It is by far the most expensive step of the normalization, and it leaves the
		// names already written in ASCII - the overwhelming majority of them - exactly as they are.
		if (!isAscii(authorName)) {
			authorName = TRANSLITERATOR_THREAD_LOCAL.get().transliterate(authorName);
		}

		// remove common honorifics
		authorName = HONORIFICS.matcher(authorName).replaceAll("");

		// strip accents
		authorName = StringUtils.stripAccents(authorName).toLowerCase(Locale.ROOT);

		List<String> res = split(authorName);

		return String.join(" ", split(authorName));
	}

	private static BitSet buildDelims() {
		Pattern single = Pattern.compile("[\\s\\p{Punct}\\p{Pd}]");
		BitSet bs = new BitSet(0x10000);
		for (int c = 0; c <= 0xFFFF; c++) {
			if (single.matcher(String.valueOf((char) c)).matches()) bs.set(c);
		}
		return bs;
	}

	public static List<String> split(String name) {
		final int n = name.length();
		final List<String> out = new ArrayList<>(4);
		int start = -1;
		for (int i = 0; i < n; ) {
			char c = name.charAt(i);
			boolean delim;
			int len = 1;
			if (Character.isHighSurrogate(c) && i + 1 < n && Character.isLowSurrogate(name.charAt(i + 1))) {
				int cp = Character.toCodePoint(c, name.charAt(i + 1));
				delim = Character.getType(cp) == Character.DASH_PUNCTUATION; // \p{Pd} beyond the BMP
				len = 2;
			} else {
				delim = DELIMITERS_MASK.get(c); // a char is always 0..0xFFFF, so always inside the table
			}
			if (delim) {
				if (start >= 0) {
					out.add(name.substring(start, i));
					start = -1;
				}
			} else if (start < 0) {
				start = i;
			}
			i += len;
		}
		if (start >= 0) out.add(name.substring(start, n));
		return out;
	}
}
