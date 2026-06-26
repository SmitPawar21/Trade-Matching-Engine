package agent;

import model.AgentAction;
import model.MarketState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Production-mode agent that loads a trained PPO policy network
 * from a JSON file and performs inference to select market-making actions.
 *
 * No external dependencies required — uses built-in JSON parsing.
 *
 * The JSON model expects:
 *   - layers[]: array of {weights: float[][], biases: float[]}
 *   - activation: "relu"
 *   - obs_dim: 4
 *   - n_actions: 5
 *
 * Input:  float[4] = [spread, inventory, imbalance, midPrice]
 * Output: argmax of action logits -> AgentAction
 *
 * Usage:
 *   MLPInferenceAgent agent = new MLPInferenceAgent("models/ppo_market_maker_policy.json");
 *   AgentAction action = agent.decide(marketState);
 */
public class ONNXInferenceAgent implements MarketAgent {

    private final String modelPath;
    private boolean modelLoaded = false;

    // Network parameters: list of (weight_matrix, bias_vector) per layer
    private final List<double[][]> weights = new ArrayList<>();
    private final List<double[]> biases = new ArrayList<>();

    private static final AgentAction[] ACTIONS = AgentAction.values();

    public ONNXInferenceAgent(String modelPath) {
        this.modelPath = modelPath;

        try {
            loadModel(modelPath);
            modelLoaded = true;
            System.out.println("MLP policy model loaded: " + modelPath);
        } catch (Exception e) {
            System.err.println("Failed to load MLP model: " + e.getMessage());
            e.printStackTrace();
            System.out.println("NOTE: Using fallback (WIDE_SPREAD).");
        }
    }

    /**
     * Parse the JSON model file and extract weight matrices and bias vectors.
     * Uses simple string parsing — no external JSON library needed.
     */
    private void loadModel(String path) throws IOException {
        String json = Files.readString(Path.of(path));

        // Parse layers from JSON
        int layersStart = json.indexOf("\"layers\"");
        if (layersStart == -1) {
            throw new IOException("Invalid model format: missing 'layers' key");
        }

        // Find the layers array
        int arrStart = json.indexOf('[', layersStart);
        int depth = 0;
        int arrEnd = arrStart;
        for (int i = arrStart; i < json.length(); i++) {
            if (json.charAt(i) == '[') depth++;
            else if (json.charAt(i) == ']') {
                depth--;
                if (depth == 0) {
                    arrEnd = i;
                    break;
                }
            }
        }

        String layersJson = json.substring(arrStart, arrEnd + 1);

        // Parse each layer object
        int searchFrom = 0;
        while (true) {
            int wStart = layersJson.indexOf("\"weights\"", searchFrom);
            if (wStart == -1) break;

            // Parse weights (2D array)
            double[][] w = parse2DArray(layersJson, wStart);

            // Parse biases (1D array)
            int bStart = layersJson.indexOf("\"biases\"", wStart);
            double[] b = parse1DArray(layersJson, bStart);

            weights.add(w);
            biases.add(b);

            // Move search position past this layer
            searchFrom = bStart + 1;
        }

        System.out.println("  Loaded " + weights.size() + " layers");
        for (int i = 0; i < weights.size(); i++) {
            System.out.println("    Layer " + i + ": " +
                weights.get(i).length + " x " + weights.get(i)[0].length);
        }
    }

    /**
     * Parse a 2D array from JSON starting at the "weights" key position.
     */
    private double[][] parse2DArray(String json, int keyStart) {
        // Find the outer '[[' 
        int outerStart = json.indexOf("[[", keyStart);
        int depth = 0;
        int outerEnd = outerStart;
        for (int i = outerStart; i < json.length(); i++) {
            if (json.charAt(i) == '[') depth++;
            else if (json.charAt(i) == ']') {
                depth--;
                if (depth == 0) {
                    outerEnd = i;
                    break;
                }
            }
        }

        String arrStr = json.substring(outerStart, outerEnd + 1);

        // Split into rows
        List<double[]> rows = new ArrayList<>();
        int pos = 0;
        while (true) {
            int rowStart = arrStr.indexOf('[', pos + 1);
            if (rowStart == -1 || rowStart > arrStr.length() - 2) break;
            int rowEnd = arrStr.indexOf(']', rowStart);
            if (rowEnd == -1) break;

            String rowStr = arrStr.substring(rowStart + 1, rowEnd).trim();
            if (!rowStr.isEmpty()) {
                String[] parts = rowStr.split(",");
                double[] row = new double[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    row[i] = Double.parseDouble(parts[i].trim());
                }
                rows.add(row);
            }
            pos = rowEnd;
        }

        return rows.toArray(new double[0][]);
    }

    /**
     * Parse a 1D array from JSON starting at the "biases" key position.
     */
    private double[] parse1DArray(String json, int keyStart) {
        int arrStart = json.indexOf('[', keyStart);
        int arrEnd = json.indexOf(']', arrStart);
        String arrStr = json.substring(arrStart + 1, arrEnd).trim();

        if (arrStr.isEmpty()) return new double[0];

        String[] parts = arrStr.split(",");
        double[] result = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Double.parseDouble(parts[i].trim());
        }
        return result;
    }

    @Override
    public AgentAction decide(MarketState state) {

        if (modelLoaded) {
            try {
                double[] input = new double[] {
                    state.getSpread(),
                    state.getInventory(),
                    state.getImbalance(),
                    state.getMidPrice()
                };

                double[] output = forwardPass(input);

                // Argmax to get best action
                int bestAction = 0;
                double bestValue = output[0];
                for (int i = 1; i < output.length; i++) {
                    if (output[i] > bestValue) {
                        bestValue = output[i];
                        bestAction = i;
                    }
                }

                return ACTIONS[bestAction];
            } catch (Exception e) {
                System.err.println("MLP inference failed: " + e.getMessage());
            }
        }

        // Fallback: use simple rule-based logic
        return AgentAction.WIDE_SPREAD;
    }

    /**
     * Forward pass through the MLP.
     * Hidden layers use ReLU activation, output layer has no activation.
     */
    private double[] forwardPass(double[] input) {
        double[] h = input;

        for (int layer = 0; layer < weights.size(); layer++) {
            double[][] w = weights.get(layer);
            double[] b = biases.get(layer);
            int outSize = w[0].length;

            double[] z = new double[outSize];

            // Matrix multiply: z = h * W + b
            for (int j = 0; j < outSize; j++) {
                z[j] = b[j];
                for (int i = 0; i < h.length; i++) {
                    z[j] += h[i] * w[i][j];
                }
            }

            // Apply ReLU for all layers except the last
            if (layer < weights.size() - 1) {
                for (int j = 0; j < z.length; j++) {
                    z[j] = Math.max(0, z[j]);
                }
            }

            h = z;
        }

        return h;
    }

    /**
     * Clean up resources (nothing to clean for JSON-based model).
     */
    public void close() {
        // No external resources to clean up
    }
}
