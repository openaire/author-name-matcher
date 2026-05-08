
package eu.openaire.common.author;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Helper class providing methods to match authors by their names.
 */
public class AuthorMatchers {

	/**
	 * Checks whether two author names are equal, ignoring case differences.
	 *
	 * @param a1 The first author name.
	 * @param a2 The second author name.
	 * @return {@code true} if the names are equal (case-insensitive), otherwise {@code false}.
	 */
	static public boolean matchEqualsIgnoreCase(String a1, String a2) {
		if (a1 == null || a2 == null)
			return false;
		else
			return a1 == a2 || a1.toLowerCase(Locale.ROOT).equals(a2.toLowerCase(Locale.ROOT));
	}

	/**
	 * Checks whether two author names match based on ordered tokens and abbreviations.
	 *
	 * <p>This method leverages the {@code OrderedTokenAndAbbreviationsMatcher} to compare names
	 * by considering token order and abbreviation expansion.</p>
	 *
	 * @param a1 The first author name.
	 * @param a2 The second author name.
	 * @return An {@code Optional<Double>} with a confidence score (1.0 if a match is found), or empty if no match.
	 */
	static public Optional<Double> matchOrderedTokenAndAbbreviations(String a1, String a2) {
		return OrderedTokenAndAbbreviationsMatcher.compare(a1, a2);
	}

	/**
	 * Removes matching authors from two lists using a custom matching function.
	 *
	 * <p>This method iterates through the lists of base authors and enriching authors, applying the
	 * given matching function to determine matches. If a match is found, both elements are removed
	 * from their respective lists.</p>
	 *
	 * @param base_authors      The list of base authors.
	 * @param enriching_authors The list of enriching authors.
	 * @param matchingFunc      A function that determines whether two author names match.
	 * @return A list containing matched base-enriching authors pairs.
	 */
	static public List<String> removeMatches(
		List<String> base_authors,
		List<String> enriching_authors,
		BiFunction<String, String, Boolean> matchingFunc) {
		List<String> matched = new ArrayList<>();

		if (base_authors != null && !base_authors.isEmpty()) {
			Iterator<String> ait = base_authors.iterator();

			while (ait.hasNext()) {
				String base = ait.next();
				Iterator<String> oit = enriching_authors.iterator();

				while (oit.hasNext()) {
					String enriching = oit.next();

					if (matchingFunc.apply(base, enriching)) {
						ait.remove();
						oit.remove();

						matched.add(base);
						matched.add(enriching);

						break;
					}
				}

			}
		}

		return matched;
	}

	/**
	 * Finds matches between a list of base authors and a list of enriching authors using a sequence of matching steps.
	 *
	 * <p>Each step in the list of {@link AuthorMatcherStep} objects applies a specific matching strategy. The result
	 * is a list of successful matches.</p>
	 *
	 * @param base_authors       The list of base authors.
	 * @param enrichment_authors The list of enriching authors.
	 * @param steps              The list of matching steps to apply.
	 * @param <BA>               The type representing the base author data.
	 * @param <EA>               The type representing the enriching author data.
	 * @return A list of {@link AuthorMatch} objects representing the matches found.
	 */
	static public <BA, EA> List<AuthorMatch<BA, EA>> findMatches(
		List<BA> base_authors,
		List<EA> enrichment_authors,
		List<AuthorMatcherStep<BA, EA>> steps) {
		List<AuthorMatch<BA, EA>> result = new ArrayList<>();
		List<BA> unmatched_base_authors = new ArrayList<>(base_authors);
		List<EA> unmatched_enrichment_authors = new ArrayList<>(enrichment_authors);

		for (AuthorMatcherStep<BA, EA> s : steps) {
			result
				.addAll(
					evaluateStep(
						unmatched_base_authors,
						unmatched_enrichment_authors,
						s));
		}
		return result;
	}

	static private <BA, EA> List<AuthorMatch<BA, EA>> evaluateStep(
		List<BA> unmatched_base_authors,
		List<EA> unmatched_enriching_authors,
		AuthorMatcherStep<BA, EA> step) {
		List<AuthorMatch<BA, EA>> matches = new ArrayList<>(unmatched_enriching_authors.size());

		if (unmatched_base_authors.isEmpty()) {
			return Collections.emptyList();
		}

		for (EA enriching_author : unmatched_enriching_authors) {
			List<AuthorMatch<BA, EA>> potential_matches = unmatched_base_authors
				.stream()
				.map(x -> step.getMatchingFunc().apply(x, enriching_author))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.sorted(Collections.reverseOrder(Comparator.comparingDouble(AuthorMatch::getConfidence)))
				.collect(Collectors.toList());

			if (potential_matches.size() == 1 ||
				(potential_matches.size() > 1
					&& (step.getExclusionPredicate() == null
						|| !step.getExclusionPredicate().test(potential_matches)))) {
				matches.addAll(potential_matches);
			}
		}

		if (matches.isEmpty()) {
			return Collections.emptyList();
		}

		Set<BA> matched_base_authors = Collections.newSetFromMap(
                new IdentityHashMap<>());
		Set<EA> matched_enrichment_authors = Collections.newSetFromMap(
                new IdentityHashMap<>());
		List<AuthorMatch<BA, EA>> results = new ArrayList<>(
			Math.min(unmatched_base_authors.size(), unmatched_enriching_authors.size()));

		List<AuthorMatch<BA, EA>> sorted_matches = matches
				.stream()
				.sorted(Collections.reverseOrder(Comparator.comparingDouble(AuthorMatch::getConfidence)))
				.collect(Collectors.toList());

		Map<BA, List<AuthorMatch<BA, EA>>> bestByBaseAuthor = new IdentityHashMap<>();
		Map<EA, List<AuthorMatch<BA, EA>>> bestByEnrichingAuthor = new IdentityHashMap<>();

		for (AuthorMatch<BA, EA> m : sorted_matches) {
			bestByBaseAuthor.compute(m.getBaseAuthor(), (k, v) -> {
				if (v == null) {
					v = new ArrayList<>();
				}
				if (v.isEmpty() || v.get(0).getConfidence() == m.getConfidence()) {
					v.add(m);
					bestByEnrichingAuthor.compute(m.getEnrichingAuthor(), (ea, lv) -> {
						if (lv == null) {
							lv = new ArrayList<>();
						}
						lv.add(m);
						return lv;
					});
				}
				return v;
			});

		}

		for (AuthorMatch<BA, EA> m : sorted_matches) {
			if (!matched_base_authors.contains(m.getBaseAuthor())
				&& !matched_enrichment_authors.contains(m.getEnrichingAuthor())) {

				List<AuthorMatch<BA, EA>> baseAuthorBestMatches = bestByBaseAuthor.get(m.getBaseAuthor());

				// detect ambiguity: more candidate enriching authors than matched authors:
				// do not resolve them
				if (baseAuthorBestMatches.size() > 1) {
					List<EA> enrichingAuthors = baseAuthorBestMatches.stream()
						.map(AuthorMatch::getEnrichingAuthor)
						.collect(Collectors.toList());

					IdentityHashMap<BA,BA> baseAuthorsFromEnriching = new IdentityHashMap<>();
					enrichingAuthors.stream()
						.flatMap(ea -> bestByEnrichingAuthor.get(ea).stream())
						.map(AuthorMatch::getBaseAuthor)
							.forEach(baseAuthor -> baseAuthorsFromEnriching.put(baseAuthor, baseAuthor));

					if (enrichingAuthors.size() > baseAuthorsFromEnriching.size()) {
						matched_base_authors.addAll(baseAuthorsFromEnriching.keySet());
						matched_enrichment_authors.addAll(enrichingAuthors);
						continue;
					}
				}

				matched_base_authors.add(m.getBaseAuthor());
				matched_enrichment_authors.add(m.getEnrichingAuthor());

				results.add(m);
			}
		}

		unmatched_base_authors.removeAll(matched_base_authors);
		unmatched_enriching_authors.removeAll(matched_enrichment_authors);

		return results;
	}
}
