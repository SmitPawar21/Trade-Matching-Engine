"""
Export a trained SB3 PPO model to ONNX format.

The exported ONNX model takes a float32 observation [spread, inventory, imbalance, midPrice]
and outputs action logits (or action probabilities).

Usage:
    python export_onnx.py
"""

import torch
import numpy as np
from stable_baselines3 import PPO


def main():
    # Load trained model
    model = PPO.load("models/ppo_market_maker")
    policy = model.policy

    # Move to eval mode
    policy.eval()

    # Observation shape: (1, 4) — [spread, inventory, imbalance, midPrice]
    dummy_obs = torch.randn(1, 4, dtype=torch.float32)

    # SB3's policy forward pass internally calls the MLP extractor + action_net
    # We need to export just the action prediction part
    class ActionExporter(torch.nn.Module):
        """Wrapper that extracts action logits from SB3 policy."""

        def __init__(self, sb3_policy):
            super().__init__()
            self.mlp_extractor = sb3_policy.mlp_extractor
            self.action_net = sb3_policy.action_net

        def forward(self, obs):
            # SB3 MlpPolicy architecture:
            #   obs -> features_extractor -> mlp_extractor -> (pi, vf)
            #   pi -> action_net -> logits
            features = obs  # MlpPolicy uses FlattenExtractor (identity)
            pi_features, _ = self.mlp_extractor(features)
            action_logits = self.action_net(pi_features)
            return action_logits

    exporter = ActionExporter(policy)
    exporter.eval()

    # Test the exporter
    with torch.no_grad():
        test_output = exporter(dummy_obs)
        print(f"Test output shape: {test_output.shape}")
        print(f"Test output: {test_output}")

    # Export to ONNX
    onnx_path = "models/ppo_market_maker.onnx"

    torch.onnx.export(
        exporter,
        dummy_obs,
        onnx_path,
        input_names=["observation"],
        output_names=["action_logits"],
        dynamic_axes={
            "observation": {0: "batch_size"},
            "action_logits": {0: "batch_size"},
        },
        opset_version=11,
    )

    print(f"ONNX model exported to {onnx_path}")

    # Verify with ONNX Runtime
    try:
        import onnxruntime as ort

        session = ort.InferenceSession(onnx_path)
        test_input = np.array([[200.0, 0.0, 0.1, 100000.0]], dtype=np.float32)
        result = session.run(None, {"observation": test_input})
        print(f"ONNX verification — action logits: {result[0]}")
        print(f"Selected action: {np.argmax(result[0])}")
        print("ONNX export verified successfully!")
    except ImportError:
        print("onnxruntime not installed — skipping verification")


if __name__ == "__main__":
    main()
