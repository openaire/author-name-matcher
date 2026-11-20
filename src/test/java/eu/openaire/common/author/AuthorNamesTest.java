
package eu.openaire.common.author;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AuthorNamesTest {

	/** punctuation separates tokens, it does not survive the normalization */
	@Test
	void lowercasesAndCollapsesTheTokens() {
		assertEquals("anthony r burrell", AuthorNames.normalize("Anthony  R   Burrell"));
		assertEquals("burrell anthony r", AuthorNames.normalize("Burrell, Anthony R."));
	}

	@Test
	void stripsAccentsAndTransliterates() {
		assertEquals("anthony r burrell", AuthorNames.normalize("Anthóny R Burrell"));
		assertEquals("muller", AuthorNames.normalize("Müller"));
	}

	@Test
	void stripsHonorifics() {
		assertEquals("anthony r burrell", AuthorNames.normalize("Dr. Anthony R Burrell"));
		assertEquals("anthony r burrell", AuthorNames.normalize("Prof Anthony R Burrell"));
		assertEquals("anthony r burrell", AuthorNames.normalize("Anthony R Burrell, PhD"));
	}

	/** trailing digits are DBLP identifiers, not part of the name */
	@Test
	void stripsTrailingDigits() {
		assertEquals("anthony r burrell", AuthorNames.normalize("Anthony R Burrell 0001"));
	}

	@Test
	void handlesNull() {
		assertNull(AuthorNames.normalize(null));
	}

}
