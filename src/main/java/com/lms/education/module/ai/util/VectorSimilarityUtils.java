package com.lms.education.module.ai.util;

public class VectorSimilarityUtils {

    public static float[] parseVectorString(String vectorStr) {
        if (vectorStr == null || vectorStr.isBlank()) {
            return new float[0];
        }
        
        // Remove brackets
        String cleanStr = vectorStr.trim();
        if (cleanStr.startsWith("[")) {
            cleanStr = cleanStr.substring(1);
        }
        if (cleanStr.endsWith("]")) {
            cleanStr = cleanStr.substring(0, cleanStr.length() - 1);
        }
        
        String[] parts = cleanStr.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                vector[i] = Float.parseFloat(parts[i].trim());
            } catch (NumberFormatException e) {
                vector[i] = 0f;
            }
        }
        return vector;
    }

    public static double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length || vectorA.length == 0) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
