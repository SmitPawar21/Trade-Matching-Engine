"""
Pure NumPy implementation of PPO (Proximal Policy Optimization).

No PyTorch, no TensorFlow — just NumPy.
Designed for discrete action spaces with small MLPs.

Exports model weights as JSON for Java inference.
"""

import json
import numpy as np
from pathlib import Path


# ---------------------------------------------------------------------------
# Neural Network Primitives
# ---------------------------------------------------------------------------

def relu(x):
    """ReLU activation."""
    return np.maximum(0, x)


def softmax(x):
    """Numerically stable softmax."""
    shifted = x - np.max(x, axis=-1, keepdims=True)
    exp_x = np.exp(shifted)
    return exp_x / np.sum(exp_x, axis=-1, keepdims=True)


def log_softmax(x):
    """Numerically stable log-softmax."""
    shifted = x - np.max(x, axis=-1, keepdims=True)
    return shifted - np.log(np.sum(np.exp(shifted), axis=-1, keepdims=True))


# ---------------------------------------------------------------------------
# Adam Optimizer
# ---------------------------------------------------------------------------

class AdamOptimizer:
    """Adam optimizer for a list of parameter arrays."""

    def __init__(self, params, lr=3e-4, beta1=0.9, beta2=0.999, eps=1e-8):
        self.lr = lr
        self.beta1 = beta1
        self.beta2 = beta2
        self.eps = eps
        self.t = 0

        # First and second moment estimates for each parameter
        self.m = [np.zeros_like(p) for p in params]
        self.v = [np.zeros_like(p) for p in params]

    def step(self, params, grads):
        """Update parameters using gradients. Returns updated params list."""
        self.t += 1
        updated = []
        for i, (p, g) in enumerate(zip(params, grads)):
            self.m[i] = self.beta1 * self.m[i] + (1 - self.beta1) * g
            self.v[i] = self.beta2 * self.v[i] + (1 - self.beta2) * (g ** 2)

            m_hat = self.m[i] / (1 - self.beta1 ** self.t)
            v_hat = self.v[i] / (1 - self.beta2 ** self.t)

            p = p - self.lr * m_hat / (np.sqrt(v_hat) + self.eps)
            updated.append(p)
        return updated


# ---------------------------------------------------------------------------
# MLP (Multi-Layer Perceptron)
# ---------------------------------------------------------------------------

class MLP:
    """
    Simple feedforward neural network with ReLU hidden layers.

    Architecture: input -> [hidden1] -> ReLU -> [hidden2] -> ReLU -> output
    """

    def __init__(self, input_dim, hidden_dims, output_dim, seed=42):
        rng = np.random.default_rng(seed)

        self.weights = []
        self.biases = []

        dims = [input_dim] + list(hidden_dims) + [output_dim]
        for i in range(len(dims) - 1):
            fan_in, fan_out = dims[i], dims[i + 1]
            # Xavier / Glorot initialization
            scale = np.sqrt(2.0 / (fan_in + fan_out))
            w = rng.normal(0, scale, (fan_in, fan_out)).astype(np.float64)
            b = np.zeros(fan_out, dtype=np.float64)
            self.weights.append(w)
            self.biases.append(b)

        self.num_layers = len(self.weights)

    def forward(self, x):
        """
        Forward pass. Returns (output, cache) for backprop.

        Cache stores pre-activation (z) and post-activation (a) for each layer.
        """
        x = np.atleast_2d(x).astype(np.float64)
        cache = {"a": [x]}  # a[0] = input

        h = x
        for i in range(self.num_layers - 1):
            z = h @ self.weights[i] + self.biases[i]
            h = relu(z)
            cache[f"z_{i}"] = z
            cache["a"].append(h)

        # Output layer (no activation)
        z_out = h @ self.weights[-1] + self.biases[-1]
        cache[f"z_{self.num_layers - 1}"] = z_out
        cache["a"].append(z_out)

        return z_out, cache

    def backward(self, d_output, cache):
        """
        Backward pass using analytical gradients.

        Args:
            d_output: gradient of loss w.r.t. network output, shape (batch, output_dim)
            cache: dictionary from forward pass

        Returns:
            grads: list of gradients [dW0, db0, dW1, db1, ...] matching get_params() order
        """
        batch_size = d_output.shape[0]
        grads_w = []
        grads_b = []

        delta = d_output  # Start from output layer

        # Backprop through layers in reverse
        for i in range(self.num_layers - 1, -1, -1):
            a_prev = cache["a"][i]  # Input to this layer

            # Gradients for this layer's parameters
            dW = (a_prev.T @ delta) / batch_size
            db = np.mean(delta, axis=0)

            grads_w.insert(0, dW)
            grads_b.insert(0, db)

            # Gradient to pass to previous layer (skip for first layer)
            if i > 0:
                delta = delta @ self.weights[i].T
                # ReLU derivative: gradient is zero where pre-activation was <= 0
                z_prev = cache[f"z_{i - 1}"]
                delta = delta * (z_prev > 0).astype(np.float64)

        # Interleave weights and biases to match get_params() order
        grads = []
        for dw, db in zip(grads_w, grads_b):
            grads.append(dw)
            grads.append(db)

        return grads

    def get_params(self):
        """Return flat list of all parameters: [W0, b0, W1, b1, ...]."""
        params = []
        for w, b in zip(self.weights, self.biases):
            params.append(w)
            params.append(b)
        return params

    def set_params(self, params):
        """Set parameters from flat list."""
        idx = 0
        for i in range(self.num_layers):
            self.weights[i] = params[idx]
            self.biases[i] = params[idx + 1]
            idx += 2

    def to_dict(self):
        """Serialize to dictionary for JSON export."""
        return {
            "weights": [w.tolist() for w in self.weights],
            "biases": [b.tolist() for b in self.biases],
        }

    @classmethod
    def from_dict(cls, d, input_dim, hidden_dims, output_dim):
        """Load from dictionary."""
        mlp = cls(input_dim, hidden_dims, output_dim)
        mlp.weights = [np.array(w, dtype=np.float64) for w in d["weights"]]
        mlp.biases = [np.array(b, dtype=np.float64) for b in d["biases"]]
        return mlp


# ---------------------------------------------------------------------------
# PPO Agent
# ---------------------------------------------------------------------------

class PPOAgent:
    """
    PPO agent with separate policy and value networks.

    Policy network:  obs -> hidden -> hidden -> action_logits (Discrete)
    Value network:   obs -> hidden -> hidden -> scalar value
    """

    def __init__(
        self,
        obs_dim=4,
        n_actions=5,
        hidden_dims=(64, 64),
        lr=3e-4,
        gamma=0.99,
        gae_lambda=0.95,
        clip_range=0.2,
        ent_coef=0.01,
        vf_coef=0.5,
        max_grad_norm=0.5,
        n_steps=256,
        batch_size=64,
        n_epochs=10,
        seed=42,
    ):
        self.obs_dim = obs_dim
        self.n_actions = n_actions
        self.hidden_dims = hidden_dims
        self.gamma = gamma
        self.gae_lambda = gae_lambda
        self.clip_range = clip_range
        self.ent_coef = ent_coef
        self.vf_coef = vf_coef
        self.max_grad_norm = max_grad_norm
        self.n_steps = n_steps
        self.batch_size = batch_size
        self.n_epochs = n_epochs
        self.seed = seed
        self.rng = np.random.default_rng(seed)

        # Policy network: obs -> action logits
        self.policy_net = MLP(obs_dim, hidden_dims, n_actions, seed=seed)

        # Value network: obs -> scalar value
        self.value_net = MLP(obs_dim, hidden_dims, 1, seed=seed + 1)

        # Optimizers
        self.policy_optimizer = AdamOptimizer(self.policy_net.get_params(), lr=lr)
        self.value_optimizer = AdamOptimizer(self.value_net.get_params(), lr=lr)

    def predict(self, obs):
        """
        Select an action given an observation.

        Returns: (action, log_prob, value)
        """
        obs = np.array(obs, dtype=np.float64).reshape(1, -1)

        # Policy forward pass
        logits, _ = self.policy_net.forward(obs)
        probs = softmax(logits)[0]

        # Clamp probabilities to avoid numerical issues
        probs = np.clip(probs, 1e-8, 1.0)
        probs = probs / probs.sum()

        # Sample action
        action = self.rng.choice(self.n_actions, p=probs)

        # Log probability
        log_prob = np.log(probs[action])

        # Value estimate
        value, _ = self.value_net.forward(obs)

        return action, log_prob, value[0, 0]

    def get_action_deterministic(self, obs):
        """Select the best action (greedy / argmax). Used for evaluation."""
        obs = np.array(obs, dtype=np.float64).reshape(1, -1)
        logits, _ = self.policy_net.forward(obs)
        return int(np.argmax(logits[0]))

    def get_value(self, obs):
        """Get value estimate for an observation."""
        obs = np.array(obs, dtype=np.float64).reshape(1, -1)
        value, _ = self.value_net.forward(obs)
        return value[0, 0]

    def compute_gae(self, rewards, values, dones, last_value):
        """
        Compute Generalized Advantage Estimation.

        Returns: (advantages, returns)
        """
        n = len(rewards)
        advantages = np.zeros(n, dtype=np.float64)
        last_gae = 0.0

        for t in reversed(range(n)):
            if t == n - 1:
                next_value = last_value
            else:
                next_value = values[t + 1]

            next_non_terminal = 1.0 - float(dones[t])
            delta = rewards[t] + self.gamma * next_value * next_non_terminal - values[t]
            advantages[t] = last_gae = (
                delta + self.gamma * self.gae_lambda * next_non_terminal * last_gae
            )

        returns = advantages + np.array(values, dtype=np.float64)
        return advantages, returns

    def _compute_policy_grads(self, obs_batch, actions_batch, old_log_probs, advantages):
        """
        Compute policy gradients using analytical backpropagation.

        PPO clipped surrogate objective with entropy bonus.
        """
        batch_size = len(obs_batch)

        # Forward pass
        logits, cache = self.policy_net.forward(obs_batch)
        probs = softmax(logits)
        log_probs_all = log_softmax(logits)

        # Gather log probs for taken actions
        new_log_probs = log_probs_all[np.arange(batch_size), actions_batch]

        # Ratio: exp(new_log_prob - old_log_prob)
        ratio = np.exp(new_log_probs - old_log_probs)

        # Clipped surrogate objective
        surr1 = ratio * advantages
        surr2 = np.clip(ratio, 1 - self.clip_range, 1 + self.clip_range) * advantages

        # Determine which surrogate to use per sample
        use_clipped = surr2 < surr1
        effective_ratio_grad = np.where(
            use_clipped,
            np.clip(ratio, 1 - self.clip_range, 1 + self.clip_range),
            ratio,
        )

        # d(loss)/d(log_prob_action) for policy loss
        # loss = -mean(min(surr1, surr2))
        # d(surr)/d(new_log_prob) = ratio * advantage (since d(ratio)/d(logp) = ratio)
        # But for clipped: gradient is 0 when clipped
        clip_mask = (ratio >= 1 - self.clip_range) & (ratio <= 1 + self.clip_range)
        d_ratio = np.where(
            use_clipped & ~clip_mask,
            0.0,  # Clipped and outside range -> zero gradient
            ratio * advantages,
        )
        # For unclipped case, gradient flows normally
        d_ratio = np.where(use_clipped, 0.0, ratio * advantages)
        # Simpler: when clip is active AND ratio is outside bounds, grad is 0
        # When clip is not active, grad = ratio * advantage
        d_logp = -d_ratio / batch_size  # negative because we minimize

        # Entropy bonus gradient
        # entropy = -sum(p * log_p), we want to maximize entropy
        # d(entropy_loss)/d(logits) = p * (log_p + 1) - but applied as penalty
        entropy_grad = self.ent_coef * probs * (log_probs_all + 1) / batch_size

        # Combined gradient w.r.t. logits
        # d(loss)/d(logits) via d(loss)/d(log_prob_action)
        # d(log_softmax_j)/d(logit_k) = 1(j==k) - softmax(k)
        d_logits = np.zeros_like(logits)

        # Policy gradient contribution
        for i in range(batch_size):
            a = actions_batch[i]
            # d(log_prob_a)/d(logit_k) = 1(a==k) - prob_k
            d_log_prob_d_logits = -probs[i].copy()
            d_log_prob_d_logits[a] += 1.0
            d_logits[i] = d_logp[i] * d_log_prob_d_logits

        # Add entropy gradient
        d_logits += entropy_grad

        # Backprop through network
        grads = self.policy_net.backward(d_logits, cache)

        # Compute loss for logging
        policy_loss = -np.mean(np.minimum(surr1, surr2))
        entropy = -np.sum(probs * log_probs_all, axis=-1).mean()
        total_loss = policy_loss - self.ent_coef * entropy

        return grads, total_loss

    def _compute_value_grads(self, obs_batch, returns):
        """
        Compute value network gradients using analytical backpropagation.

        MSE loss: 0.5 * mean((V(s) - R)^2)
        """
        # Forward pass
        values, cache = self.value_net.forward(obs_batch)
        values = values[:, 0]  # (batch,)

        # MSE loss
        loss = 0.5 * np.mean((values - returns) ** 2)

        # d(loss)/d(values) = (values - returns) / batch_size
        batch_size = len(returns)
        d_values = (values - returns) / batch_size  # (batch,)

        # Reshape for backprop: (batch, 1)
        d_output = d_values.reshape(-1, 1)

        # Backprop through network
        grads = self.value_net.backward(d_output, cache)

        return grads, loss

    def _clip_gradients(self, grads):
        """Clip gradients by global norm."""
        total_norm = np.sqrt(sum(np.sum(g ** 2) for g in grads))
        if total_norm > self.max_grad_norm:
            scale = self.max_grad_norm / (total_norm + 1e-8)
            grads = [g * scale for g in grads]
        return grads

    def update(self, obs_buf, act_buf, logp_buf, ret_buf, adv_buf):
        """
        Run PPO update using collected rollout data.

        Args:
            obs_buf:  (N, obs_dim) observations
            act_buf:  (N,) actions taken
            logp_buf: (N,) log-probs at time of action
            ret_buf:  (N,) discounted returns
            adv_buf:  (N,) advantages
        """
        # Normalize advantages
        adv_buf = (adv_buf - np.mean(adv_buf)) / (np.std(adv_buf) + 1e-8)

        n = len(obs_buf)
        total_policy_loss = 0.0
        total_value_loss = 0.0
        num_updates = 0

        for epoch in range(self.n_epochs):
            # Shuffle
            indices = self.rng.permutation(n)

            for start in range(0, n, self.batch_size):
                end = min(start + self.batch_size, n)
                batch_idx = indices[start:end]

                obs_batch = obs_buf[batch_idx]
                act_batch = act_buf[batch_idx]
                logp_batch = logp_buf[batch_idx]
                ret_batch = ret_buf[batch_idx]
                adv_batch = adv_buf[batch_idx]

                # Policy update (analytical backprop)
                policy_grads, p_loss = self._compute_policy_grads(
                    obs_batch, act_batch, logp_batch, adv_batch
                )
                policy_grads = self._clip_gradients(policy_grads)
                new_policy_params = self.policy_optimizer.step(
                    self.policy_net.get_params(), policy_grads
                )
                self.policy_net.set_params(new_policy_params)

                # Value update (analytical backprop)
                value_grads, v_loss = self._compute_value_grads(obs_batch, ret_batch)
                value_grads = self._clip_gradients(value_grads)
                value_grads = [g * self.vf_coef for g in value_grads]
                new_value_params = self.value_optimizer.step(
                    self.value_net.get_params(), value_grads
                )
                self.value_net.set_params(new_value_params)

                total_policy_loss += p_loss
                total_value_loss += v_loss
                num_updates += 1

        avg_policy_loss = total_policy_loss / max(num_updates, 1)
        avg_value_loss = total_value_loss / max(num_updates, 1)

        return {
            "policy_loss": avg_policy_loss,
            "value_loss": avg_value_loss,
            "num_updates": num_updates,
        }

    # -----------------------------------------------------------------------
    # Save / Load
    # -----------------------------------------------------------------------

    def save(self, path):
        """Save model weights to JSON file."""
        path = Path(path)
        path.parent.mkdir(parents=True, exist_ok=True)

        model_data = {
            "obs_dim": self.obs_dim,
            "n_actions": self.n_actions,
            "hidden_dims": list(self.hidden_dims),
            "policy_net": self.policy_net.to_dict(),
            "value_net": self.value_net.to_dict(),
        }

        with open(path, "w") as f:
            json.dump(model_data, f)

        print(f"Model saved to {path}")

    @classmethod
    def load(cls, path, **kwargs):
        """Load model weights from JSON file."""
        with open(path, "r") as f:
            model_data = json.load(f)

        obs_dim = model_data["obs_dim"]
        n_actions = model_data["n_actions"]
        hidden_dims = tuple(model_data["hidden_dims"])

        agent = cls(
            obs_dim=obs_dim,
            n_actions=n_actions,
            hidden_dims=hidden_dims,
            **kwargs,
        )

        agent.policy_net = MLP.from_dict(
            model_data["policy_net"], obs_dim, hidden_dims, n_actions
        )
        agent.value_net = MLP.from_dict(
            model_data["value_net"], obs_dim, hidden_dims, 1
        )

        print(f"Model loaded from {path}")
        return agent
