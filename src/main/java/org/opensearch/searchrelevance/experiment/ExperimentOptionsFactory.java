/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.experiment;

import java.util.ArrayList;
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
            Object rawValue = params.get("rankConstants");
            if (!(rawValue instanceof List<?> rawList)) {
                throw new IllegalArgumentException(
                    "rankConstants must be a list of integers, got: " + (rawValue == null ? "null" : rawValue.getClass())
                );
            }
            List<Integer> ranks = new ArrayList<>(rawList.size());
            for (Object element : rawList) {
                if (!(element instanceof Number n)) {
                    throw new IllegalArgumentException(
                        "rankConstants must contain integers, got: " + (element == null ? "null" : element.getClass())
                    );
                }
                ranks.add(n.intValue());
            }
            builder.rankConstants(ranks);
        }

        return builder.build();
    }

}
