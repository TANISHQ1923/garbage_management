const env = require('../config/env');

/**
 * Service for communicating with the standalone Python FastAPI AI microservice.
 * Provides garbage classification and severity estimation.
 * Strictly fail-safe: never throws unhandled errors to avoid crashing complaint workflows.
 */

/**
 * Sends an image URL to the AI microservice for classification and severity analysis.
 *
 * @param {string} imageUrl - The public HTTPS/HTTP image URL (e.g. Cloudinary CDN)
 * @returns {Promise<{success: boolean, status: string, data?: object, error?: string}>}
 */
const analyzeGarbageImage = async (imageUrl) => {
  if (!imageUrl || typeof imageUrl !== 'string' || imageUrl.trim() === '') {
    return {
      success: false,
      status: 'FAILED',
      error: 'No valid image URL provided for AI analysis.'
    };
  }

  const endpoint = `${env.AI_SERVICE_URL.replace(/\/+$/, '')}/api/predict/image`;

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), env.AI_SERVICE_TIMEOUT_MS);

    const response = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify({ imageUrl: imageUrl.trim() }),
      signal: controller.signal
    });

    clearTimeout(timeoutId);

    if (response.ok) {
      const json = await response.json();
      if (json && json.success && json.data) {
        return {
          success: true,
          status: 'COMPLETED',
          data: json.data
        };
      }
      return {
        success: false,
        status: 'FAILED',
        error: 'AI service returned an unexpected response structure.'
      };
    }

    // Handle 4xx vs 5xx
    const errorBody = await response.text();
    let errorDetail = errorBody;
    try {
      const parsed = JSON.parse(errorBody);
      errorDetail = parsed.detail || parsed.message || errorBody;
    } catch (_) {
      // Keep string body
    }

    if (response.status >= 500) {
      console.warn(`[AIService] AI service server error (${response.status}): ${errorDetail}`);
      return {
        success: false,
        status: 'AI_UNAVAILABLE',
        error: `AI service returned error ${response.status}: ${errorDetail}`
      };
    }

    // 4xx client errors (e.g. invalid image format, SSRF blocked, oversized)
    return {
      success: false,
      status: 'FAILED',
      error: `AI analysis failed: ${errorDetail}`
    };

  } catch (error) {
    const isTimeout = error.name === 'AbortError' || error.name === 'TimeoutError';
    const isConnectionError = error.cause?.code === 'ECONNREFUSED' || error.message?.includes('fetch failed');

    if (isTimeout) {
      console.warn(`[AIService] Request to AI microservice timed out after ${env.AI_SERVICE_TIMEOUT_MS}ms`);
    } else if (isConnectionError) {
      console.warn(`[AIService] AI microservice unavailable at ${endpoint} (${error.message})`);
    } else {
      console.warn(`[AIService] Error communicating with AI microservice: ${error.message}`);
    }

    return {
      success: false,
      status: 'AI_UNAVAILABLE',
      error: isTimeout ? 'AI service request timed out' : 'AI service unavailable'
    };
  }
};

/**
 * Checks connectivity to the AI microservice health endpoint.
 *
 * @returns {Promise<{healthy: boolean, data?: object, error?: string}>}
 */
const checkAiHealth = async () => {
  const endpoint = `${env.AI_SERVICE_URL.replace(/\/+$/, '')}/api/health`;
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 3000);

    const response = await fetch(endpoint, {
      method: 'GET',
      headers: { 'Accept': 'application/json' },
      signal: controller.signal
    });

    clearTimeout(timeoutId);

    if (response.ok) {
      const json = await response.json();
      return {
        healthy: true,
        data: json
      };
    }
    return {
      healthy: false,
      error: `HTTP ${response.status}`
    };
  } catch (error) {
    return {
      healthy: false,
      error: error.message
    };
  }
};

module.exports = {
  analyzeGarbageImage,
  checkAiHealth
};
