from env.market_env import MarketMakingEnv
import matplotlib.pyplot as plt

env = MarketMakingEnv()

state, _ = env.reset()

spreads = []
inventories = []
prices = []
rewards = []

for _ in range(200):

    # random actions for now
    action = env.action_space.sample()

    next_state, reward, done, _, _ = env.step(action)


    spreads.append(next_state[0])
    inventories.append(next_state[1])
    prices.append(next_state[3])
    rewards.append(reward)


    if done:
        break

# Plot 1: Mid price
plt.figure()
plt.plot(prices)
plt.title("Mid Price")
plt.xlabel("Step")
plt.ylabel("Price")
plt.show()


# Plot 2: Spread
plt.figure()
plt.plot(spreads)
plt.title("Spread")
plt.xlabel("Step")
plt.ylabel("Spread")
plt.show()

# Plot 3: Inventory
plt.figure()
plt.plot(inventories)
plt.title("Inventory")
plt.xlabel("Step")
plt.ylabel("Inventory")
plt.show()


# Plot 4: Reward
plt.figure()
plt.plot(rewards)
plt.title("Reward")
plt.xlabel("Step")
plt.ylabel("Reward")
plt.show()