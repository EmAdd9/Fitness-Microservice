package com.fitness.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAIService {
    private final GeminiService geminiService;

    public Recommendation generateRecommendation(Activity activity){
        String prompt = createPromptForActivity(activity);
        String aiResponse = geminiService.getAnswer(prompt);
        log.info("Response From GEMINI: {}",aiResponse);
        return processAiResponse(activity,aiResponse);
    }

    private Recommendation processAiResponse(Activity activity, String aiResponse){
        try {

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(aiResponse);

            JsonNode textNode = rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0).path("text");

            String jsonContent = textNode.asText()
                    .replaceAll("```json\\n","")
                    .replaceAll("```","")
                    .replaceAll("\\n","")
                    .trim();

//            log.info("Parsed Response From AI: {}",jsonContent);
            JsonNode analysisJson = mapper.readTree(jsonContent);
            JsonNode analysisNode = analysisJson.path("analysis");
            StringBuilder fullAnalysis = new StringBuilder();
            addAnalysisSection(fullAnalysis,analysisNode,"overall","Overall: ");
            addAnalysisSection(fullAnalysis,analysisNode,"pace","Pace: ");
            addAnalysisSection(fullAnalysis,analysisNode,"heartRate","HeartRate: ");
            addAnalysisSection(fullAnalysis,analysisNode,"caloriesBurnt","CaloriesBurnt: ");

            List<String> improvements = extractImprovements(analysisJson.path("improvements"));
            List<String> suggestions = extractSuggestions(analysisJson.path("suggestions"));
            List<String> safety = extractSafetyGuidelines(analysisJson.path("safety"));
            return Recommendation.builder()
                    .activityId(activity.getId())
                    .activityType(activity.getType())
                    .userId(activity.getUserId())
                    .recommendation(fullAnalysis.toString().trim())
                    .improvements(improvements)
                    .suggestions(suggestions)
                    .safety(safety)
                    .createdAt(LocalDateTime.now())
                    .build();


        }catch (Exception e){
            e.printStackTrace();
            return createDefaultRecommendation(activity);
        }
    }

    private Recommendation createDefaultRecommendation(Activity activity) {
        return Recommendation.builder()
                .activityId(activity.getId())
                .activityType(activity.getType())
                .userId(activity.getUserId())
                .recommendation("unable to generate detailed Analysis")
                .improvements(Collections.singletonList("Continue with your Motivation"))
                .suggestions(Collections.singletonList("Consider consulting with af Trainer"))
                .safety(Arrays.asList(
                        "Stay Hydrated",
                        "Warm Up",
                        "Listen to your body"))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private List<String> extractSafetyGuidelines(JsonNode safetyNode) {
        List<String> safetyList = new ArrayList<>();
        if(safetyNode.isArray()){
            safetyNode.forEach(item -> {
                safetyList.add(item.asText());
            });
        }
        return safetyList.isEmpty() ?
                Collections.singletonList("No specific safety guidelines provided") :
                safetyList;
    }


    private List<String> extractSuggestions(JsonNode suggestionNode) {
        List<String> suggestionsList = new ArrayList<>();
        if(suggestionNode.isArray()){
            suggestionNode.forEach(suggestion -> {
                String workout = suggestion.path("workout").asText();
                String description = suggestion.path("description").asText();
                suggestionsList.add(String.format("%s: %s",workout,description));
            });
        }
        return suggestionsList.isEmpty() ?
                Collections.singletonList("No specific suggestions provided") :
                suggestionsList;
    }

    private List<String> extractImprovements(JsonNode improvementNode) {
        List<String> improvementList = new ArrayList<>();
        if(improvementNode.isArray()){
            improvementNode.forEach(improvement -> {
                String area = improvement.path("area").asText();
                String recommendation = improvement.path("recommendation").asText();
                improvementList.add(String.format("%s: %s",area,recommendation));
            });
        }
        return improvementList.isEmpty() ?
                Collections.singletonList("No specific improvements provided") :
                improvementList;
    }

    private void addAnalysisSection(StringBuilder fullAnalysis, JsonNode analysisNode, String key, String prefix) {
           if(!analysisNode.path(key).isMissingNode()){
               fullAnalysis.append(prefix)
                       .append(analysisNode.path(key).asText())
                       .append("\n\n");
           }
    }

    private String createPromptForActivity(Activity activity) {
        return String.format("""
                Analyze this fitness activity and provide detailed recommendations in the following EXACT JSON format:
                        {
                          "analysis": {
                            "overall": "Overall analysis here",
                            "pace": "Pace analysis here",
                            "heartRate": "Heart rate analysis here",
                            "caloriesBurnt": "Calories analysis here"
                          },
                          "improvements": [
                            {
                              "area": "Area name",
                              "recommendation": "Detailed recommendation"
                            }
                          ],
                          "suggestions": [
                            {
                              "workout": "Workout name",
                              "description": "Detailed workout description"
                            }
                          ],
                          "safety": [
                            "Safety point 1",
                            "Safety point 2"
                          ]
                        }
                                
                        Analyze this activity:
                        Activity Type: %s
                        Duration: %d minutes
                        Calories Burnt: %d
                        Additional Metrics: %s
                
                        Provide detailed analysis focusing on performance, improvements, next workout suggestions, and safety guidelines.
                        Ensure the response follows the EXACT JSON format shown above.
                """,
                activity.getType(),
                activity.getDuration(),
                activity.getCaloriesBurnt(),
                activity.getAdditionalMetrics()

        );
    }
}
