/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.experiment;

import static org.opensearch.searchrelevance.experiment.ExperimentOptionsForHybridSearch.COMBINATION_RRF;
import static org.opensearch.searchrelevance.experiment.ExperimentOptionsForHybridSearch.EXPERIMENT_OPTION_COMBINATION_TECHNIQUE;
import static org.opensearch.searchrelevance.experiment.ExperimentOptionsForHybridSearch.EXPERIMENT_OPTION_NORMALIZATION_TECHNIQUE;
import static org.opensearch.searchrelevance.experiment.ExperimentOptionsForHybridSearch.EXPERIMENT_OPTION_RANK_CONSTANT;
import static org.opensearch.searchrelevance.experiment.ExperimentOptionsForHybridSearch.EXPERIMENT_OPTION_WEIGHTS_FOR_COMBINATION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.opensearch.searchrelevance.model.ExperimentVariant;

/**
 * Utility class for a query source
 */
public class QuerySourceUtil {

    public static final int NUMBER_OF_SUBQUERIES_IN_HYBRID_QUERY = 2;

    static final String PHASE_RESULTS_PROCESSORS_KEY = "phase_results_processors";
    static final String NORMALIZATION_PROCESSOR_KEY = "normalization-processor";
    static final String SCORE_RANKER_PROCESSOR_KEY = "score-ranker-processor";
    static final String NORMALIZATION_KEY = "normalization";
    static final String COMBINATION_KEY = "combination";
    static final String TECHNIQUE_KEY = "technique";
    static final String PARAMETERS_KEY = "parameters";
    static final String WEIGHTS_KEY = "weights";
    static final String RANK_CONSTANT_KEY = "rank_constant";

    /**
     * Creates a definition of a temporary search pipeline for hybrid search.
     * @param experimentVariant sub-experiment to create the pipeline for
     * @return definition of a temporary search pipeline
     */
    public static Map<String, Object> createDefinitionOfTemporarySearchPipeline(final ExperimentVariant experimentVariant) {
        Map<String, Object> experimentVariantParameters = experimentVariant.getParameters();

        if (COMBINATION_RRF.equals(experimentVariantParameters.get(EXPERIMENT_OPTION_COMBINATION_TECHNIQUE))) {
            Object rankConstantObj = experimentVariantParameters.get(EXPERIMENT_OPTION_RANK_CONSTANT);
            int rankConstant = ((Number) rankConstantObj).intValue();
            Map<String, Object> rrfCombinationConfig = new HashMap<>(
                Map.of(TECHNIQUE_KEY, COMBINATION_RRF, RANK_CONSTANT_KEY, rankConstant)
            );
            Map<String, Object> scoreRankerConfig = new HashMap<>(Map.of(COMBINATION_KEY, rrfCombinationConfig));
            Map<String, Object> rrfPhaseProcessorObject = new HashMap<>(Map.of(SCORE_RANKER_PROCESSOR_KEY, scoreRankerConfig));
            Map<String, Object> rrfTemporarySearchPipeline = new HashMap<>();
            rrfTemporarySearchPipeline.put(PHASE_RESULTS_PROCESSORS_KEY, List.of(rrfPhaseProcessorObject));
            return rrfTemporarySearchPipeline;
        }

        Map<String, Object> normalizationTechniqueConfig = new HashMap<>(
            Map.of(TECHNIQUE_KEY, experimentVariantParameters.get(EXPERIMENT_OPTION_NORMALIZATION_TECHNIQUE))
        );

        Map<String, Object> combinationTechniqueConfig = new HashMap<>(
            Map.of(TECHNIQUE_KEY, experimentVariantParameters.get(EXPERIMENT_OPTION_COMBINATION_TECHNIQUE))
        );
        if (Objects.nonNull(experimentVariantParameters.get(EXPERIMENT_OPTION_WEIGHTS_FOR_COMBINATION))) {
            float[] weights = (float[]) experimentVariantParameters.get(EXPERIMENT_OPTION_WEIGHTS_FOR_COMBINATION);
            List<Double> weightsList = new ArrayList<>(weights.length);
            for (float weight : weights) {
                weightsList.add((double) weight);
            }
            combinationTechniqueConfig.put(PARAMETERS_KEY, new HashMap<>(Map.of(WEIGHTS_KEY, weightsList)));
        }

        Map<String, Object> normalizationProcessorConfig = new HashMap<>(
            Map.of(NORMALIZATION_KEY, normalizationTechniqueConfig, COMBINATION_KEY, combinationTechniqueConfig)
        );
        Map<String, Object> phaseProcessorObject = new HashMap<>(Map.of(NORMALIZATION_PROCESSOR_KEY, normalizationProcessorConfig));
        Map<String, Object> temporarySearchPipeline = new HashMap<>();
        temporarySearchPipeline.put(PHASE_RESULTS_PROCESSORS_KEY, List.of(phaseProcessorObject));
        return temporarySearchPipeline;
    }

    /**
     * Validate that the query in the search configuration is a hybrid query with two sub-queries.
     * @param fullQueryMap
     * @throws IOException
     */
    public static void validateHybridQuery(Map<String, Object> fullQueryMap) throws IOException {
        if (fullQueryMap.containsKey("query") == false || fullQueryMap.get("query") instanceof Map == false) {
            throw new IllegalArgumentException("search configuration must have at least one query");
        }
        Map<String, Object> queryMap = (Map<String, Object>) fullQueryMap.get("query");
        if (queryMap.containsKey("hybrid") == false || queryMap.get("hybrid") instanceof Map<?, ?> == false) {
            throw new IllegalArgumentException("query in search configuration must be of type hybrid");
        }
        Map<String, Object> hybridMap = (Map<String, Object>) queryMap.get("hybrid");
        if (hybridMap.containsKey("queries") == false || hybridMap.get("queries") instanceof List<?> == false) {
            throw new IllegalArgumentException("hybrid query in search configuration does not have sub-queries");
        }
        List<?> queriesMap = (List<?>) hybridMap.get("queries");
        if (queriesMap.size() != NUMBER_OF_SUBQUERIES_IN_HYBRID_QUERY) {
            throw new IllegalArgumentException(
                String.format(
                    Locale.ROOT,
                    "invalid hybrid query: expected exactly [%d] sub-queries but found [%d]",
                    NUMBER_OF_SUBQUERIES_IN_HYBRID_QUERY,
                    queriesMap.size()
                )
            );
        }
    }
}
