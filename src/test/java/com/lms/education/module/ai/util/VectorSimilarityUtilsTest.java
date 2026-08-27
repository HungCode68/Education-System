package com.lms.education.module.ai.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VectorSimilarityUtilsTest {

    @Test
    void parseVectorString_ValidString_ReturnsArray() {
        String vectorStr = "[0.1, 0.2, 0.3]";
        float[] result = VectorSimilarityUtils.parseVectorString(vectorStr);

        assertArrayEquals(new float[]{0.1f, 0.2f, 0.3f}, result);
    }
    
    @Test
    void parseVectorString_NullOrBlank_ReturnsEmptyArray() {
        assertArrayEquals(new float[0], VectorSimilarityUtils.parseVectorString(null));
        assertArrayEquals(new float[0], VectorSimilarityUtils.parseVectorString(""));
        assertArrayEquals(new float[0], VectorSimilarityUtils.parseVectorString("   "));
    }
    
    @Test
    void parseVectorString_InvalidFormat_HandlesGracefully() {
        String vectorStr = "[0.1, invalid, 0.3]";
        float[] result = VectorSimilarityUtils.parseVectorString(vectorStr);

        assertArrayEquals(new float[]{0.1f, 0.0f, 0.3f}, result);
    }

    @Test
    void cosineSimilarity_ValidVectors_ReturnsCorrectSimilarity() {
        float[] vectorA = {1.0f, 0.0f, 0.0f};
        float[] vectorB = {1.0f, 0.0f, 0.0f};
        assertEquals(1.0, VectorSimilarityUtils.cosineSimilarity(vectorA, vectorB), 0.0001);
        
        float[] vectorC = {0.0f, 1.0f, 0.0f};
        assertEquals(0.0, VectorSimilarityUtils.cosineSimilarity(vectorA, vectorC), 0.0001);
    }

    @Test
    void cosineSimilarity_DifferentLengths_ReturnsZero() {
        float[] vectorA = {1.0f, 0.0f, 0.0f};
        float[] vectorB = {1.0f, 0.0f};
        assertEquals(0.0, VectorSimilarityUtils.cosineSimilarity(vectorA, vectorB));
    }
    
    @Test
    void cosineSimilarity_ZeroNorm_ReturnsZero() {
        float[] vectorA = {0.0f, 0.0f, 0.0f};
        float[] vectorB = {1.0f, 0.0f, 0.0f};
        assertEquals(0.0, VectorSimilarityUtils.cosineSimilarity(vectorA, vectorB));
    }
}
