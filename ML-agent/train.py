from env.market_env import MarketMakingEnv

env = MarketMakingEnv()

state, _ = env.reset()

for _ in range(10):

    action = env.action_space.sample()

    next_state, reward, done, _, _ = env.step(action)

    print("State:", next_state)
    print("Reward:", reward)