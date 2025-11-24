/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jmeter.extractor.json.jmespath;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.extractor.json.JsonExtractionUtils;
import org.apache.jmeter.processor.PostProcessor;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.AbstractScopedTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.burt.jmespath.Expression;

/**
 * JMESPATH based extractor
 *
 * @since 5.2
 */
public class JMESPathExtractor extends AbstractScopedTestElement
        implements Serializable, PostProcessor, TestStateListener {

    private static final long serialVersionUID = 3849270294526207081L;

    private static final Logger log = LoggerFactory.getLogger(JMESPathExtractor.class);
    private static final String JMES_PATH_EXPRESSION = "JMESExtractor.jmesPathExpr"; // $NON-NLS-1$
    private static final String REFERENCE_NAME = "JMESExtractor.referenceName"; // $NON-NLS-1$
    private static final String DEFAULT_VALUE = "JMESExtractor.defaultValue"; // $NON-NLS-1$
    private static final String MATCH_NUMBER = "JMESExtractor.matchNumber"; // $NON-NLS-1$
    private static final String REF_MATCH_NR = "_matchNr"; // $NON-NLS-1$
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void process() {
        ExtractionConfig config = buildConfig();
        processExtraction(config);
    }

    private void processExtraction(ExtractionConfig config) {
        JMeterVariables vars = config.vars;
        clearOldRefVars(vars, config.refName);
        if (config.jsonResponse.isEmpty()) {
            handleEmptyResponse(vars, config.refName, config.defaultValue);
            return;
        }
        try {
            List<Object> extractedValues = extractValues(config);
            handleResult(vars, config, extractedValues);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.error("Error processing JSON content in {}, message: {}", getName(), e.getLocalizedMessage(), e);
            } else {
                log.error("Error processing JSON content in {}, message: {}", getName(), e.getLocalizedMessage());
            }
            vars.put(config.refName, config.defaultValue);
        }
    }

    private ExtractionConfig buildConfig() {
        JMeterContext context = getThreadContext();
        JMeterVariables vars = context.getVariables();
        List<String> jsonResponse = getData(vars, context);
        String refName = getRefName();
        String defaultValue = getDefaultValue();
        int matchNumber;
        if (StringUtils.isBlank(getMatchNumber())) {
            matchNumber = 0;
        } else {
            matchNumber = Integer.parseInt(getMatchNumber());
        }
        final String jsonPathExpression = getJmesPathExpression().trim();
        return new ExtractionConfig(vars, jsonResponse, refName, defaultValue, matchNumber, jsonPathExpression);
    }

    private void handleResult(JMeterVariables vars, ExtractionConfig config, List<Object> extractedValues) {
        if (extractedValues.isEmpty()) {
            handleNullResult(vars, config.refName, config.defaultValue, config.matchNumber);
            return;
        }
        List<Object> selectedValues = selectMatches(extractedValues, config.matchNumber, config.refName);
        List<String> finalizedValues = finalizeValues(selectedValues, config.defaultValue);
        assignVariables(vars, config, extractedValues.size(), finalizedValues);
    }

    private List<Object> selectMatches(List<Object> extractedValues, int matchNumber, String refName) {
        if (matchNumber < 0) {
            return new ArrayList<>(extractedValues);
        }
        if (matchNumber == 0) {
            int matchSize = extractedValues.size();
            int matchNr = JMeterUtils.getRandomInt(matchSize);
            return Collections.singletonList(extractedValues.get(matchNr));
        }
        if (matchNumber > extractedValues.size()) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "matchNumber({}) exceeds number of items found({}), default value will be used",
                        matchNumber, extractedValues.size());
            }
            return Collections.emptyList();
        }
        return Collections.singletonList(extractedValues.get(matchNumber - 1));
    }

    private List<Object> extractValues(ExtractionConfig config) throws IOException {
        List<Object> resultList = new ArrayList<>();
        Expression<JsonNode> searchExpression = JMESPathCache.getInstance().get(config.jsonPathExpression);
        for (String response : config.jsonResponse) {
            JsonNode actualObj = OBJECT_MAPPER.readValue(response, JsonNode.class);
            JsonNode result = searchExpression.search(actualObj);
            if (result.isNull()) {
                resultList.add(null);
                continue;
            }
            resultList.addAll(splitJson(result));
        }
        return resultList;
    }

    private List<String> finalizeValues(List<Object> rawValues, String defaultValue) {
        if (rawValues.isEmpty()) {
            return Collections.singletonList(defaultValue);
        }
        List<String> result = new ArrayList<>(rawValues.size());
        for (Object rawValue : rawValues) {
            result.add(JsonExtractionUtils.finalizeValue(rawValue, defaultValue));
        }
        return result;
    }

    private void assignVariables(JMeterVariables vars, ExtractionConfig config, int extractedSize, List<String> values) {
        if (values.size() > 1) {
            handleListResult(vars, config.refName, values);
        } else {
            handleSingleResult(vars, config.refName, config.matchNumber, values.get(0));
        }
        vars.put(config.refName + REF_MATCH_NR, Integer.toString(extractedSize));
    }

    private void handleSingleResult(JMeterVariables vars, String refName, int matchNumber, String value) {
        String suffix = (matchNumber < 0) ? "_1" : "";
        vars.put(refName + suffix, value);
    }

    private void handleListResult(JMeterVariables vars, String refName, List<String> resultList) {
        int index = 1;
        for (String extractedString : resultList) {
            vars.put(refName + "_" + index, extractedString); // $NON-NLS-1$
            index++;
        }
    }

    private void handleNullResult(JMeterVariables vars, String refName, String defaultValue, int matchNumber) {
        vars.put(refName, defaultValue);
        vars.put(refName + REF_MATCH_NR, "0"); //$NON-NLS-1$
        if (matchNumber < 0) {
            log.debug("No value extracted, storing empty in: {}", refName);
        }
    }

    private void handleEmptyResponse(JMeterVariables vars, String refName, String defaultValue) {
        if (log.isDebugEnabled()) {
            log.debug("Response or source variable is null or empty for {}", getName());
        }
        vars.put(refName, defaultValue);
    }

    private List<String> getData(JMeterVariables vars, JMeterContext context) {
        if (isScopeVariable()) {
            String jsonResponse = vars.get(getVariableName());
            if (log.isDebugEnabled()) {
                log.debug("JMESExtractor is using variable: {}, which content is: {}", getVariableName(), jsonResponse);
            }
            return Arrays.asList(jsonResponse);
        } else {
            SampleResult previousResult = context.getPreviousResult();
            if (previousResult != null) {
                List<String> results = getSampleList(previousResult).stream()
                        .map(SampleResult::getResponseDataAsString)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toList());
                if (log.isDebugEnabled()) {
                    log.debug("JMESExtractor {} working on Responses: {}", getName(), results);
                }
                return results;
            }
        }
        return Collections.emptyList();
    }

    public List<Object> splitJson(JsonNode jsonNode) throws IOException {
        List<Object> splittedJsonElements = new ArrayList<>();
        if (jsonNode.isArray()) {
            for (JsonNode element : (ArrayNode) jsonNode) {
                splittedJsonElements.add(writeJsonNode(OBJECT_MAPPER, element));
            }
        } else {
            splittedJsonElements.add(writeJsonNode(OBJECT_MAPPER, jsonNode));
        }
        return splittedJsonElements;
    }

    private static Object writeJsonNode(ObjectMapper mapper, JsonNode element) throws JsonProcessingException {
        if (element.isNull()) {
            return null;
        }
        if (element.isTextual()) {
            return element.asText();
        }
        return mapper.writeValueAsString(element);
    }

    void clearOldRefVars(JMeterVariables vars, String refName) {
        vars.remove(refName + REF_MATCH_NR);
        for (int i = 1; vars.get(refName + "_" + i) != null; i++) {
            vars.remove(refName + "_" + i);
        }
    }

    public String getJmesPathExpression() {
        return getPropertyAsString(JMES_PATH_EXPRESSION);
    }

    public void setJmesPathExpression(String jsonPath) {
        setProperty(JMES_PATH_EXPRESSION, jsonPath);
    }

    public String getRefName() {
        return getPropertyAsString(REFERENCE_NAME);
    }

    public void setRefName(String refName) {
        setProperty(REFERENCE_NAME, refName);
    }

    public String getDefaultValue() {
        return getPropertyAsString(DEFAULT_VALUE);
    }

    public void setDefaultValue(String defaultValue) {
        setProperty(DEFAULT_VALUE, defaultValue, ""); // $NON-NLS-1$
    }

    public void setMatchNumber(String matchNumber) {
        setProperty(MATCH_NUMBER, matchNumber);
    }

    public String getMatchNumber() {
        return getPropertyAsString(MATCH_NUMBER);
    }

    @Override
    public void testStarted() {
        testStarted("");
    }

    @Override
    public void testStarted(String host) {
        // NOOP
    }

    @Override
    public void testEnded() {
        testEnded("");
    }

    @Override
    public void testEnded(String host) {
        JMESPathCache.getInstance().cleanUp();
    }
}
