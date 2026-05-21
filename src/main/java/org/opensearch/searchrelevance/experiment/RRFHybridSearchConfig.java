/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.experiment;

import static org.opensearch.searchrelevance.experiment.ExperimentOptionsForHybridSearch.COMBINATION_RRF;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * A rank-based hybrid-search configuration backed by the neural-search
 * score-ranker-processor. Independent of any score normalization; the only
 * tunable is the rank_constant. Expands once into one variant per
 * configured rank_constant.
 */
@Data
@Builder
public class RRFHybridSearchConfig implements HybridSearchConfig {

    private final List<Integer> rankConstants;

    @Override
    public List<ExperimentVariantHybridSearchDTO> getAllVariants(boolean includeWeights) {
        if (rankConstants == null || rankConstants.isEmpty()) {
            throw new IllegalArgumentException(
                "rankConstants is required when 'rrf' is among the requested combination techniques; got null or empty"
            );
        }
        List<ExperimentVariantHybridSearchDTO> variants = new ArrayList<>();
        for (Integer rankConstant : rankConstants) {
            variants.add(
                ExperimentVariantHybridSearchDTO.builder()
                    .combinationTechnique(COMBINATION_RRF)
                    .rrfConfig(new RRFVariantConfig(rankConstant))
                    .build()
            );
        }
        return variants;
    }
}
