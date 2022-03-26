from game_simulation import GameSimulation, SnapshotData
from agent import InvalidCommand, MoveCommand, ShootCommand, Command
import typing
import time

def my_ai(gamestate: SnapshotData, my_data: typing.Dict) -> Command:

	def increase_right(data):
		if data.get('right') is None:
			data['right'] = 1
		else:
			data['right'] += 1

	x = gamestate.controlled_player.position[0]
	y = gamestate.controlled_player.position[1]
	increase_right(my_data)
	if x < 0:
		return MoveCommand((1.0, 0.0, 0.0))
	elif y > 0:
		return MoveCommand((0.0, -1.0, 0.0))
	else:
		return ShootCommand(45.0)
#		return MoveCommand((0.0, 0.0, 0.0))

def idle_ai(gamestate: SnapshotData, my_data: typing.Dict) -> Command:
	return MoveCommand((0.0, 1.0, 0.0))


if __name__ == '__main__':
	simulation_list = []
	used_port = 2049

	simulation = GameSimulation('java', game_time=20.0, ai_time=150.0, commands_per_second=4.0, port=used_port)

	first_agent_data = {}
	second_agent_data = {}

	simulation.set_first_agent('agent_0', my_ai, first_agent_data)
	simulation.set_second_agent('agent_1', idle_ai, second_agent_data)

	simulation.start_round()

	results, data = simulation.end_round()

	for agent_result in results:
		print(f'Agent result: {agent_result}')

	for agent_data in data:
		print(f'Agent data: {agent_data}')
        
