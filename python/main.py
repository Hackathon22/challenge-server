from game_simulation import GameSimulation, SnapshotData
from agent import MoveCommand, ShootCommand, Command
import typing

def my_ai(gamestate: SnapshotData, my_data: typing.Dict) -> Command:
	if my_data.get('counter') is None:
		my_data['counter'] = 1
	else:
		my_data['counter'] += 1

	return MoveCommand((0.0, 0.0, 0.0))

if __name__ == '__main__':
	simulation_list = []
	used_port = 2049

	simulation = GameSimulation('java', game_time=600.0, ai_time=150.0, commands_per_second=4, port=used_port)

	first_agent_data = {}
	second_agent_data = {}

	simulation.set_first_agent('agent_0', my_ai, first_agent_data)
	simulation.set_second_agent('agent_1', my_ai, second_agent_data)

	simulation.start_round()

	results, data = simulation.end_round()

	for agent_result in results:
		print(f'Agent result: {agent_result}')

	for agent_data in data:
		print(f'Agent data: {agent_data}')
