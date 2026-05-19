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
 * Experiment options for hybrid search
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
    static class WeightsRange {
        private float rangeMin;
        private float rangeMax;
        private float increment;
    }

    public List<ExperimentVariantHybridSearchDTO> getParameterCombinations(boolean includeWeights) {
        List<ExperimentVariantHybridSearchDTO> allPossibleParameterCombinations = new ArrayList<>();
        boolean rrfAlreadyExpanded = false;
        for (String normalizationTechnique : normalizationTechniques) {
            for (String combinationTechnique : combinationTechniques) {
                // z_score produces negative values which are incompatible with geometric/harmonic mean
                if (NORMALIZATION_Z_SCORE.equals(normalizationTechnique)
                    && (COMBINATION_GEOMETRIC_MEAN.equals(combinationTechnique)
                        || COMBINATION_HARMONIC_MEAN.equals(combinationTechnique))) {
                    continue;
                }
                // RRF is rank-based: iterate over rank_constant values instead of weights.
                // RRF is independent of normalization technique, so expand it only once across the whole matrix.
                if (COMBINATION_RRF.equals(combinationTechnique)) {
                    if (rrfAlreadyExpanded || rankConstants == null || rankConstants.isEmpty()) {
                        continue;
                    }
                    for (Integer rankConstant : rankConstants) {
                        allPossibleParameterCombinations.add(
                            ExperimentVariantHybridSearchDTO.builder()
                                .combinationTechnique(COMBINATION_RRF)
                                .rrfConfig(RRFVariantConfig.builder().rankConstant(rankConstant).build())
                                .build()
                        );
                    }
                    rrfAlreadyExpanded = true;
                    continue;
                }
                if (includeWeights) {
                    // use integer-based approach to avoid floating-point precision issues
                    float min = weightsRange.getRangeMin();
                    float max = weightsRange.getRangeMax();
                    float increment = weightsRange.getIncrement();

                    // calculate number of steps to ensure we include all values including the max
                    int steps = Math.round((max - min) / increment) + 1;

                    for (int i = 0; i < steps; i++) {
                        // calculate weight, ensuring the last step is exactly the max value
                        float queryWeightForCombination;
                        if (i == steps - 1) {
                            queryWeightForCombination = max;
                        } else {
                            queryWeightForCombination = min + (i * increment);
                        }

                        float w1 = Math.round(queryWeightForCombination * 10) / 10.0f;
                        float w2 = Math.round((1.0f - w1) * 10) / 10.0f;

                        allPossibleParameterCombinations.add(
                            ExperimentVariantHybridSearchDTO.builder()
                                .normalizationTechnique(normalizationTechnique)
                                .combinationTechnique(combinationTechnique)
                                .queryWeightsForCombination(new float[] { w1, w2 })
                                .build()
                        );
                    }
                } else {
                    allPossibleParameterCombinations.add(
                        ExperimentVariantHybridSearchDTO.builder()
                            .normalizationTechnique(normalizationTechnique)
                            .combinationTechnique(combinationTechnique)
                            .queryWeightsForCombination(new float[] { 0.5f, 0.5f })
                            .build()
                    );
                }
            }
        }
        return allPossibleParameterCombinations;
    }
}
