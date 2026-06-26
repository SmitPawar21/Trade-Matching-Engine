"""
Visualize market-making agent behavior.

Run with random actions to see how the environment responds,
or load a trained model for evaluation.

Usage:
    python visualize.py              # Random agent
    python visualize.py --trained    # Trained agent
"""

import argparse
from env.market_env import MarketMakingEnv
from ppo_numpy import PPOAgent
import matplotlib.pyplot as plt


def run_agent(num_steps=200, use_trained=False):
    env = MarketMakingEnv()

    agent = None
    if use_trained:
        try:
            agent = PPOAgent.load("models/ppo_market_maker.json")
            print("Loaded trained model for evaluation")
        except FileNotFoundError:
            print("No trained model found, falling back to random agent")
            use_trained = False

    state, _ = env.reset()

    spreads = []
    inventories = []
    prices = []
    rewards = []

    for step in range(num_steps):

        if use_trained and agent is not None:
            action = agent.get_action_deterministic(state)
        else:
            action = env.action_space.sample()

        next_state, reward, terminated, truncated, info = env.step(action)

        spreads.append(next_state[0])
        inventories.append(next_state[1])
        prices.append(next_state[3])
        rewards.append(reward)

        state = next_state

        if terminated or truncated:
            print(f"Episode ended at step {step + 1}")
            break

    env.close()
    return spreads, inventories, prices, rewards, use_trained


def plot_results(spreads, inventories, prices, rewards, trained=False):
    fig, axes = plt.subplots(2, 2, figsize=(14, 10))

    policy_name = "Trained PPO" if trained else "Random"

    # Plot 1: Mid price
    axes[0, 0].plot(prices, color='#2196F3', linewidth=1.5)
    axes[0, 0].set_title("Mid Price")
    axes[0, 0].set_xlabel("Step")
    axes[0, 0].set_ylabel("Price")
    axes[0, 0].grid(True, alpha=0.3)

    # Plot 2: Spread
    axes[0, 1].plot(spreads, color='#FF9800', linewidth=1.5)
    axes[0, 1].set_title("Spread")
    axes[0, 1].set_xlabel("Step")
    axes[0, 1].set_ylabel("Spread")
    axes[0, 1].grid(True, alpha=0.3)

    # Plot 3: Inventory
    axes[1, 0].plot(inventories, color='#4CAF50', linewidth=1.5)
    axes[1, 0].axhline(y=0, color='red', linestyle='--', alpha=0.5)
    axes[1, 0].set_title("Inventory")
    axes[1, 0].set_xlabel("Step")
    axes[1, 0].set_ylabel("Inventory")
    axes[1, 0].grid(True, alpha=0.3)

    # Plot 4: Cumulative Reward
    cumulative_rewards = [sum(rewards[:i+1]) for i in range(len(rewards))]
    axes[1, 1].plot(cumulative_rewards, color='#9C27B0', linewidth=1.5)
    axes[1, 1].set_title("Cumulative Reward")
    axes[1, 1].set_xlabel("Step")
    axes[1, 1].set_ylabel("Reward")
    axes[1, 1].grid(True, alpha=0.3)

    plt.tight_layout()
    plt.suptitle(f"Market Making Agent - {policy_name} Policy", y=1.02, fontsize=14)
    plt.show()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Visualize market-making agent")
    parser.add_argument("--trained", action="store_true", help="Use trained model")
    parser.add_argument("--steps", type=int, default=200, help="Number of steps")
    args = parser.parse_args()

    spreads, inventories, prices, rewards, trained = run_agent(
        num_steps=args.steps, use_trained=args.trained
    )
    plot_results(spreads, inventories, prices, rewards, trained=trained)