/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.experiment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Factory class for creating ExperimentOptions based on the experiment name and parameters
 */
public class ExperimentOptionsFactory {

    public static final String EMPTY_EXPERIMENT_OPTIONS = "EMPTY_EXPERIMENT_OPTIONS";
    public static final String HYBRID_SEARCH_EXPERIMENT_OPTIONS = "HYBRID_SEARCH_EXPERIMENT_OPTIONS";

    private static final Map<String, Function<Map<String, Object>, ExperimentOptions>> OPTIONS_BY_EXPERIMENT_NAME = Map.of(
        EMPTY_EXPERIMENT_OPTIONS,
        params -> new EmptyExperimentOptions(),
        HYBRID_SEARCH_EXPERIMENT_OPTIONS,
        ExperimentOptionsFactory::getExperimentOptionsForHybridSearch
    );

    /**
     * Creates an ExperimentOptions object based on the provided experiment name and parameters.
     *
     * @param experimentName The name of the experiment.
     * @param params The parameters for the experiment.
     * @return An ExperimentOptions object.
     * @throws IllegalArgumentException If the provided experiment name is not supported.
     */
    public static ExperimentOptions createExperimentOptions(final String experimentName, final Map<String, Object> params) {
        return Optional.ofNullable(OPTIONS_BY_EXPERIMENT_NAME.get(experimentName))
            .orElseThrow(() -> new IllegalArgumentException("provided experiment name is not supported"))
            .apply(params);
    }

    private static ExperimentOptionsForHybridSearch getExperimentOptionsForHybridSearch(Map<String, Object> params) {
        ExperimentOptionsForHybridSearch.ExperimentOptionsForHybridSearchBuilder builder = ExperimentOptionsForHybridSearch.builder();

        if (params.containsKey("normalizationTechniques")) {
            builder.normalizationTechniques((Set<String>) params.get("normalizationTechniques"));
        }

        if (params.containsKey("combinationTechniques")) {
            builder.combinationTechniques((Set<String>) params.get("combinationTechniques"));
        }

        if (params.containsKey("weightsRange")) {
            Map<String, Object> weightsRangeMap = (Map<String, Object>) params.get("weightsRange");
            ExperimentOptionsForHybridSearch.WeightsRange.WeightsRangeBuilder weightsRangeBuilder =
                ExperimentOptionsForHybridSearch.WeightsRange.builder();

            if (weightsRangeMap.containsKey("rangeMin")) {
                weightsRangeBuilder.rangeMin(((Number) weightsRangeMap.get("rangeMin")).floatValue());
            }

            if (weightsRangeMap.containsKey("rangeMax")) {
                weightsRangeBuilder.rangeMax(((Number) weightsRangeMap.get("rangeMax")).floatValue());
            }

            if (weightsRangeMap.containsKey("increment")) {
                weightsRangeBuilder.increment(((Number) weightsRangeMap.get("increment")).floatValue());
            }

            builder.weightsRange(weightsRangeBuilder.build());
        }

        if (params.containsKey("rankConstants")) {
            builder.rankConstants((List<Integer>) params.get("rankConstants"));
        }

        return builder.build();
    }

    /**
     * Creates a default set of experiment parameters for hybrid search.
     * <p>
     * The {@code rankConstants} list used by RRF variants was selected empirically
     * by sweeping k &isin; {1, 2, 5, 10, 20, 40, 60, 80, 100, 150, 250, 500, 1000, 3000, 10000}
     * on the ESCI benchmark (100k documents, 150 judged queries). The 15 k values
     * collapsed into only 8 distinct top-10 equivalence groups; k &isin;
     * {40, 60, 80, 100, 150, 250, 500, 1000} all produced identical retrieval on
     * every query, while k &le; 20 each yielded a distinct top-10. The curated
     * list below keeps one representative per behaviorally-distinct region
     * (1, 5, 10, 20 for the sensitive range and 60 for the plateau), avoiding
     * redundant RRF variants. See design doc &sect; 3.3.2 for the full analysis.
     *
     * @return A map containing the default experiment parameters.
     */
    public static Map<String, Object> createDefaultExperimentParametersForHybridSearch() {
        return Map.of(
            "normalizationTechniques",
            Set.of("min_max", "l2", "z_score"),
            "combinationTechniques",
            Set.of("arithmetic_mean", "geometric_mean", "harmonic_mean", "rrf"),
            "weightsRange",
            Map.of("rangeMin", 0.0, "rangeMax", 1.0, "increment", 0.1),
            "rankConstants",
            List.of(1, 5, 10, 20, 60)
        );
    }
}
