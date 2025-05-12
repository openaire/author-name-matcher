
package eu.openaire.common.author;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class AuthorMatchersTest {

    @Test
    void mixedmethods() {
        // Data for DOI 10.1111/jbi.14978
        List<TestAuthor> authors = TestAuthor
                .of(
                        "Marco Ferrante",
                        "Gabor L. Lövei",
                        "Andy G. Howe");

        List<TestORCIDAuthor> candidates = Arrays
                .asList(
                        new TestORCIDAuthor("Marco", "Ferrante", "", "0000-0003-2421-396X"),
                        new TestORCIDAuthor("Gabor", "Lövei", "", "0000-0002-6467-9812"),
                        new TestORCIDAuthor("Andrew", "Howe", "Andy G. Howe", "0000-0002-7460-5227"));

        List<AuthorMatch<TestAuthor, TestORCIDAuthor>> result = getMatches(authors, candidates);

        Assertions.assertEquals(3, result.size());
        Assertions.assertTrue(checkMatch(result, "Marco Ferrante", "0000-0003-2421-396X"));
        Assertions.assertTrue(checkMatch(result, "Gabor L. Lövei", "0000-0002-6467-9812"));
        Assertions.assertTrue(checkMatch(result, "Andy G. Howe", "0000-0002-7460-5227"));

    }

    @Test
    void homonomyTest() {
        // Data for DOI https://doi.org/10.57805/revstat.v20i4.382
        List<TestAuthor> authors = TestAuthor
                .of(
                        "Otto, Philipp",
                        "Otto, P.");

        List<TestORCIDAuthor> candidates = Arrays
                .asList(
                        new TestORCIDAuthor("Philipp", "Otto", "", "0000-0001-8630-108X"),
                        new TestORCIDAuthor("Philipp", "Otto", "", "0000-0002-9796-6682"));

        List<AuthorMatch<TestAuthor, TestORCIDAuthor>> result = getMatches(authors, candidates);

        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(checkMatch(result, "Otto, Philipp", "0000-0001-8630-108X"));
        Assertions.assertTrue(checkMatch(result, "Otto, P.", "0000-0002-9796-6682"));
    }

    @Test
    void accentInsensitiveTest() {
        // Data for DOI 10.48550/arxiv.1210.5363
        List<TestAuthor> authors = TestAuthor
                .of(
                        "Michal Pilipczuk");

        List<TestORCIDAuthor> candidates = Arrays
                .asList(
                        new TestORCIDAuthor("Michał", "Pilipczuk", "", "0000-0001-7891-1988"));
        List<AuthorMatch<TestAuthor, TestORCIDAuthor>> result = getMatches(authors, candidates);

        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(checkMatch(result, "Michal Pilipczuk", "0000-0001-7891-1988"));

    }

    @Test
    void fullNamesTest() {
        // Data for DOI 10.1145/3618260.3649791
        List<TestAuthor> authors = TestAuthor
                .of(
                        "Peter Gartland",
                        "Daniel Lokshtanov",
                        "Tomáš Masařík",
                        "Marcin Pilipczuk",
                        "Michał Pilipczuk",
                        "Paweł Rzążewski");

        List<TestORCIDAuthor> candidates = Arrays
                .asList(
                        new TestORCIDAuthor("Tomáš", "Masařík", "", "0000-0001-8524-4036"),
                        new TestORCIDAuthor("Daniel", "Lokshtanov", "", "0000-0002-3166-9212"),
                        new TestORCIDAuthor("Paweł", "Rzążewski", "", "0000-0001-7696-3848"),
                        new TestORCIDAuthor("Marcin", "Pilipczuk", "", "0000-0001-5680-7397"),
                        new TestORCIDAuthor("Michał", "Pilipczuk", "", "0000-0001-7891-1988")

                );
        List<AuthorMatch<TestAuthor, TestORCIDAuthor>> result = getMatches(authors, candidates);

        Assertions.assertEquals(5, result.size());
        Assertions.assertTrue(checkMatch(result, "Daniel Lokshtanov", "0000-0002-3166-9212"));
        Assertions.assertTrue(checkMatch(result, "Tomáš Masařík", "0000-0001-8524-4036"));
        Assertions.assertTrue(checkMatch(result, "Marcin Pilipczuk", "0000-0001-5680-7397"));
        Assertions.assertTrue(checkMatch(result, "Michał Pilipczuk", "0000-0001-7891-1988"));
        Assertions.assertTrue(checkMatch(result, "Paweł Rzążewski", "0000-0001-7696-3848"));
    }

    @Test
    void caseInsensitiveTest() {
        // Data for pmid: 14244447
        List<TestAuthor> authors = TestAuthor
                .of(
                        "Davis, M. J. F.");

        List<TestORCIDAuthor> candidates = Arrays
                .asList(
                        new TestORCIDAuthor("M J", "DAVIS", "", ""));
        List<AuthorMatch<TestAuthor, TestORCIDAuthor>> result = getMatches(authors, candidates);

        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(checkMatch(result, "Davis, M. J. F.", ""));
    }

    @Test
    @Disabled
    void iWithoutDot() {
        // Data for ?
        List<TestAuthor> authors = TestAuthor
                .of(
                        "Zekai Tarakçı");

        List<TestORCIDAuthor> candidates = Arrays
                .asList(
                        new TestORCIDAuthor("Zekai", "TARAKÇI", "", "0000-0002-3828-3232"));
        List<AuthorMatch<TestAuthor, TestORCIDAuthor>> result = getMatches(authors, candidates);

        Assertions.assertEquals(1, result.size());

        Assertions.assertTrue(checkMatch(result, "Zekai Tarakçı", "0000-0002-3828-3232"));

    }

    static List<AuthorMatch<TestAuthor, TestORCIDAuthor>> getMatches(List<TestAuthor> baseAuthors,
                                                                     List<TestORCIDAuthor> enrichingAuthors) {
        List<AuthorMatch<TestAuthor, TestORCIDAuthor>> result =
                AuthorMatchers
                        .findMatches(
                                baseAuthors,
                                enrichingAuthors,
                                Arrays
                                        .asList(
                                                AuthorMatcherStep
                                                        .stringIgnoreCaseMatcher(TestAuthor::getFullName, TestORCIDAuthor::getFullName)
                                                        .name("fullName")
                                                        .build(),
                                                AuthorMatcherStep
                                                        .stringIgnoreCaseMatcher(TestAuthor::getFullName, TestORCIDAuthor::getInvertedFullName)
                                                        .name("invertedFullName")
                                                        .build(),
                                                AuthorMatcherStep
                                                        .abbreviationsMatcher(TestAuthor::getFullName, TestORCIDAuthor::getFullName)
                                                        .name("orderedTokens")
                                                        .build(),
                                                AuthorMatcherStep
                                                        .stringIgnoreCaseMatcher(TestAuthor::getFullName, TestORCIDAuthor::getCreditName)
                                                        .name("creditName")
                                                        .build()));
        for (AuthorMatch<TestAuthor, TestORCIDAuthor> match : result) {
            System.out.println(match);
        }
        return result;
    }

    static boolean checkMatch(List<AuthorMatch<TestAuthor, TestORCIDAuthor>> matches, String name, String orcid) {
        return matches.stream().anyMatch(x -> checkMatch(x, name, orcid));
    }

    static boolean checkMatch(AuthorMatch<TestAuthor, TestORCIDAuthor> x, String name, String orcid) {
        return x.getBaseAuthor().getFullName().equals(name) && x.getEnrichingAuthor().getORCID().equals(orcid);
    }
}
