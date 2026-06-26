"""
Train a PPO market-making agent against the Java matching engine.

Uses a pure NumPy PPO implementation (no PyTorch required).

Prerequisites:
    1. Java matching engine must be running on localhost:9090
    2. pip install -r requirements.txt

Usage:
    python train.py
"""

from ppo_numpy import PPOAgent
from env.market_env import MarketMakingEnv
import numpy as np
import os
import time


def main():
    # Create output directories
    os.makedirs("models", exist_ok=True)
    os.makedirs("models/checkpoints", exist_ok=True)
    os.makedirs("logs", exist_ok=True)

    # --- Training Environment ---
    env = MarketMakingEnv(symbol="BTC")

    # --- PPO Hyperparameters ---
    agent = PPOAgent(
        obs_dim=4,             # [spread, inventory, imbalance, midPrice]
        n_actions=5,           # Discrete(5) action space
        hidden_dims=(64, 64),  # Two hidden layers
        lr=3e-4,               # Adam learning rate
        gamma=0.99,            # Discount factor
        gae_lambda=0.95,       # GAE lambda
        clip_range=0.2,        # PPO clip range
        ent_coef=0.01,         # Entropy coefficient (encourages exploration)
        vf_coef=0.5,           # Value function coefficient
        max_grad_norm=0.5,     # Gradient clipping
        n_steps=256,           # Steps per rollout buffer collection
        batch_size=64,         # Minibatch size for SGD
        n_epochs=10,           # SGD epochs per rollout
        seed=42,
    )

    # --- Train ---
    total_timesteps = 50_000  # Increase for better performance
    checkpoint_freq = 10_000

    print(f"Starting PPO training for {total_timesteps} timesteps...")
    print("Make sure Java matching engine is running on localhost:9090")
    print(f"Using pure NumPy PPO (no PyTorch)")
    print("-" * 60)

    global_step = 0
    episode_count = 0
    episode_rewards = []

    obs, _ = env.reset()

    # Rollout buffers
    obs_buf = []
    act_buf = []
    rew_buf = []
    done_buf = []
    logp_buf = []
    val_buf = []

    start_time = time.time()

    while global_step < total_timesteps:
        # --- Collect Rollout ---
        for step in range(agent.n_steps):
            action, log_prob, value = agent.predict(obs)

            next_obs, reward, terminated, truncated, info = env.step(action)

            obs_buf.append(obs.copy())
            act_buf.append(action)
            rew_buf.append(reward)
            done_buf.append(terminated or truncated)
            logp_buf.append(log_prob)
            val_buf.append(value)

            obs = next_obs
            global_step += 1

            if terminated or truncated:
                episode_count += 1
                ep_reward = sum(rew_buf[-(step + 1):])
                episode_rewards.append(ep_reward)

                if episode_count % 5 == 0:
                    recent_avg = np.mean(episode_rewards[-10:])
                    elapsed = time.time() - start_time
                    fps = global_step / elapsed if elapsed > 0 else 0
                    print(
                        f"Episode {episode_count:4d} | "
                        f"Step {global_step:6d}/{total_timesteps} | "
                        f"Reward: {ep_reward:8.2f} | "
                        f"Avg(10): {recent_avg:8.2f} | "
                        f"FPS: {fps:.0f}"
                    )

                obs, _ = env.reset()

            if global_step >= total_timesteps:
                break

        # --- Compute GAE ---
        last_value = agent.get_value(obs)
        advantages, returns = agent.compute_gae(
            rew_buf, val_buf, done_buf, last_value
        )

        # --- PPO Update ---
        obs_arr = np.array(obs_buf, dtype=np.float64)
        act_arr = np.array(act_buf, dtype=np.int64)
        logp_arr = np.array(logp_buf, dtype=np.float64)
        ret_arr = np.array(returns, dtype=np.float64)
        adv_arr = np.array(advantages, dtype=np.float64)

        update_info = agent.update(obs_arr, act_arr, logp_arr, ret_arr, adv_arr)

        print(
            f"  Update @ step {global_step} | "
            f"Policy Loss: {update_info['policy_loss']:.4f} | "
            f"Value Loss: {update_info['value_loss']:.4f}"
        )

        # Clear buffers
        obs_buf.clear()
        act_buf.clear()
        rew_buf.clear()
        done_buf.clear()
        logp_buf.clear()
        val_buf.clear()

        # --- Checkpoint ---
        if global_step % checkpoint_freq < agent.n_steps:
            ckpt_path = f"models/checkpoints/ppo_mm_{global_step}.json"
            agent.save(ckpt_path)

    # --- Save Final Model ---
    agent.save("models/ppo_market_maker.json")
    print("-" * 60)
    print("Training complete!")
    print(f"  Total episodes: {episode_count}")
    print(f"  Total steps: {global_step}")
    if episode_rewards:
        print(f"  Final avg reward (last 10): {np.mean(episode_rewards[-10:]):.2f}")
    print(f"  Model saved to models/ppo_market_maker.json")

    env.close()


if __name__ == "__main__":
    main()