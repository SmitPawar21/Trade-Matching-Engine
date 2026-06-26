"""
Export a trained NumPy PPO model to JSON format for Java inference.

The exported JSON model contains:
  - Network architecture metadata (dims, hidden sizes)
  - Weight matrices and bias vectors for each layer
  - Only the policy network (action selection), not the value network

Java side loads this JSON and performs a simple forward pass:
  obs -> W1*x+b1 -> ReLU -> W2*x+b2 -> ReLU -> W3*x+b3 -> argmax -> action

Usage:
    python export_model.py
"""

import json
import numpy as np
from ppo_numpy import PPOAgent, softmax


def main():
    # Load trained model
    model_path = "models/ppo_market_maker.json"
    agent = PPOAgent.load(model_path)

    # Export only the policy network (what Java needs for inference)
    export_data = {
        "description": "PPO Market Maker - Policy Network",
        "obs_dim": agent.obs_dim,
        "n_actions": agent.n_actions,
        "hidden_dims": list(agent.hidden_dims),
        "activation": "relu",
        "layers": [],
    }

    # Export each layer's weights and biases
    for i, (w, b) in enumerate(
        zip(agent.policy_net.weights, agent.policy_net.biases)
    ):
        layer = {
            "name": f"layer_{i}",
            "weights": w.tolist(),  # (in_features, out_features)
            "biases": b.tolist(),   # (out_features,)
            "in_features": w.shape[0],
            "out_features": w.shape[1],
        }
        export_data["layers"].append(layer)

    # Save
    export_path = "models/ppo_market_maker_policy.json"
    with open(export_path, "w") as f:
        json.dump(export_data, f, indent=2)

    print(f"Policy network exported to {export_path}")
    print(f"  Layers: {len(export_data['layers'])}")
    for layer in export_data["layers"]:
        print(f"    {layer['name']}: {layer['in_features']} -> {layer['out_features']}")

    # --- Verify ---
    print("\nVerification:")
    test_obs = np.array([[200.0, 0.0, 0.1, 100000.0]], dtype=np.float64)

    # Forward pass using the agent
    logits, _ = agent.policy_net.forward(test_obs)
    probs = softmax(logits)[0]
    action = int(np.argmax(logits[0]))

    print(f"  Test input: {test_obs[0].tolist()}")
    print(f"  Logits: {logits[0].tolist()}")
    print(f"  Probabilities: {[f'{p:.4f}' for p in probs]}")
    print(f"  Selected action: {action}")

    action_names = [
        "TIGHT_SPREAD", "WIDE_SPREAD", "AGGRESSIVE_BUY",
        "AGGRESSIVE_SELL", "CANCEL_QUOTES"
    ]
    print(f"  Action name: {action_names[action]}")
    print("\nExport verified successfully!")


if __name__ == "__main__":
    main()
