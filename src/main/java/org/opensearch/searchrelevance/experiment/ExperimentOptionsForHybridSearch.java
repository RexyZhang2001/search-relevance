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
import java.util.Set;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
/**
 * Experiment options for hybrid search.
 * <p>
 * Holds the user-provided technique sets and assembles a list of
 * {@link HybridSearchConfig} instances at expansion time. Each config knows
 * how to expand itself into concrete variants (score-based ones via
 * {@link ScoreBasedHybridSearchConfig}, rank-based RRF via
 * {@link RRFHybridSearchConfig}).
 */
public class ExperimentOptionsForHybridSearch implements ExperimentOptions {
    private Set<String> normalizationTechniques;
    private Set<String> combinationTechniques;
    private WeightsRange weightsRange;
    private List<Integer> rankConstants;

    public static final String EXPERIMENT_OPTION_NORMALIZATION_TECHNIQUE = "normalization";
    public static final String EXPERIMENT_OPTION_COMBINATION_TECHNIQUE = "combination";
    public static final String EXPERIMENT_OPTION_WEIGHTS_FOR_COMBINATION = "weights";
    public static final String EXPERIMENT_OPTION_RANK_CONSTANT = "rank_constant";

    public static final String NORMALIZATION_MIN_MAX = "min_max";
    public static final String NORMALIZATION_L2 = "l2";
    public static final String NORMALIZATION_Z_SCORE = "z_score";

    public static final String COMBINATION_ARITHMETIC_MEAN = "arithmetic_mean";
    public static final String COMBINATION_GEOMETRIC_MEAN = "geometric_mean";
    public static final String COMBINATION_HARMONIC_MEAN = "harmonic_mean";
    public static final String COMBINATION_RRF = "rrf";

    @Data
    @Builder
    public static class WeightsRange {
        private float rangeMin;
        private float rangeMax;
        private float increment;
    }

    /**
     * Creates the default experiment options for hybrid search with all supported techniques.
     */
    public static ExperimentOptionsForHybridSearch createDefault() {
        return ExperimentOptionsForHybridSearch.builder()
            .normalizationTechniques(Set.of(NORMALIZATION_MIN_MAX, NORMALIZATION_L2, NORMALIZATION_Z_SCORE))
            .combinationTechniques(
                Set.of(COMBINATION_ARITHMETIC_MEAN, COMBINATION_GEOMETRIC_MEAN, COMBINATION_HARMONIC_MEAN, COMBINATION_RRF)
            )
            .weightsRange(WeightsRange.builder().rangeMin(0.0f).rangeMax(1.0f).increment(0.1f).build())
            .rankConstants(List.of(1, 5, 10, 20, 60))
            .build();
    }

    public List<ExperimentVariantHybridSearchDTO> getParameterCombinations(boolean includeWeights) {
        List<ExperimentVariantHybridSearchDTO> allVariants = new ArrayList<>();
        for (HybridSearchConfig config : buildConfigs()) {
            allVariants.addAll(config.getAllVariants(includeWeights));
        }
        return allVariants;
    }

    /**
     * Build the list of configurations represented by the user-provided technique sets.
     * Score-based configs are created only for valid (normalization, combination) pairs;
     * RRF, which is normalization-independent, contributes a single config when requested.
     */
    private List<HybridSearchConfig> buildConfigs() {
        List<HybridSearchConfig> configs = new ArrayList<>();

        if (combinationTechniques != null && combinationTechniques.contains(COMBINATION_RRF)) {
            configs.add(RRFHybridSearchConfig.builder().rankConstants(rankConstants).build());
        }

        if (normalizationTechniques == null || combinationTechniques == null) {
            return configs;
        }
        for (String normalizationTechnique : normalizationTechniques) {
            for (String combinationTechnique : combinationTechniques) {
                if (COMBINATION_RRF.equals(combinationTechnique)) {
                    continue;
                }
                if (isIncompatible(normalizationTechnique, combinationTechnique)) {
                    continue;
                }
                configs.add(
                    ScoreBasedHybridSearchConfig.builder()
                        .normalizationTechnique(normalizationTechnique)
                        .combinationTechnique(combinationTechnique)
                        .weightsRange(weightsRange)
                        .build()
                );
            }
        }
        return configs;
    }

    /**
     * z_score produces negative values, which break geometric_mean (n-th root of a product
     * with negative factors) and harmonic_mean (division by values near zero).
     */
    private static boolean isIncompatible(String normalization, String combination) {
        return NORMALIZATION_Z_SCORE.equals(normalization)
            && (COMBINATION_GEOMETRIC_MEAN.equals(combination) || COMBINATION_HARMONIC_MEAN.equals(combination));
    }
}
