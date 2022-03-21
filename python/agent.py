import subprocess
import datetime
import threading
import typing
import socket
import time
import json
from dataclasses import dataclass

JAR_FILE = 'challenge.jar'
COMMANDS_PER_SECOND = 4
GAME_TIME = 60.0
AI_TIME = 150.0

BLUE_USERNAME = 'player_0'
BLUE_TEAM = 0

RED_USERNAME = 'player_1'
RED_TEAM = 1


def challenge_thread_work():
	current_datetime = datetime.datetime.now()
	process_result = subprocess.run(['java', '-jar', '--illegal-access=warn', JAR_FILE, '-m', 'windowless', '-f', f'{str(current_datetime)}.hackathon', '-t', f'{GAME_TIME}', '-a', f'{AI_TIME}', '-c', f'{COMMANDS_PER_SECOND}'])
	print(f'Challenge process finished, returned code: {process_result.returncode}')


@dataclass
class PlayerData:
	'''Structure containing the position (x, y, z), speed (dx, dy, dz), health (float), 
	team (integer) and score (float, in seconds) of a player.'''
	position: typing.Tuple[float, float, float]
	speed: typing.Tuple[float, float, float]
	health: float
	team: int
	score: float

@dataclass
class ProjectileData:
	'''Structure containing the position (x, y, z) and speed (dx, dy, dz) of a projectile.'''
	position: typing.Tuple[float, float, float]
	speed: typing.Tuple[float, float]

@dataclass
class SnapshotData:
	'''Structure containing the PlayerData of the controlled player and the other players,
	as well as the ProjectileData of all projectiles'''
	controlled_player: PlayerData
	other_players: typing.List[PlayerData]
	projectiles: typing.List[ProjectileData]


class AIAgent:

	def __init__(self, username : str, team: int, ai: typing.Callable[[], str], address: str = '127.0.0.1', port: int = 2049):
		self.username = username
		self.team = team
		self.ai = ai
		self._port = port
		self._address = address
		self._socket = None
		self._thread = None

	def _connect(self, port: int = 2049, address: str = '127.0.0.1'):
		self._socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		self._socket.connect((address, port))
		# Sends the username and the team
		self._send_message(self.username)
		self._send_message(str(self.team))

	def _send_message(self, message: str):
		# Checking if there is a \n at the end of the string
		if message[-1] != '\n':
			message += '\n'
		self._socket.sendall(message.encode('UTF-8'))


	def _receive_message(self) -> typing.Dict:
		message = self._socket.recv(1024).decode('UTF-8')
		parsed_message = json.loads(message)
		print(f'Json received: {parsed_message}')
		return parsed_message

	def _parse_player_data(self, data: typing.Dict) -> PlayerData:
		pass


	def _send_command(self, snapshot: typing.Dict) -> str:
		# Parses the message
		controlled_player_data = snapshot['controlledPlayer']
		controlled_player_position = (float(controlled_player_data['pos']['x']), 
									  float(controlled_player_data['pos']['x']),
									  float(controlled_player_data['pos']['y']))
		controlled_player_speed = (float(controlled_player_data['speed']['x']),
								   float(controlled_player_data['speed']['y']),
								   float(controlled_player_data['speed']['z']))
		controlled_player_health = float(controlled_player_data['health'])
		controlled_player_team = int(controlled_player_data['team'])
		controlled_player_score = float(controlled_player_data['score'])


	def start(self):
		self._thread = threading.Thread(target=self._work)
		self._thread.start()

	def join(self):
		self._thread.join()

	def _work(self):
		self._connect()
		should_stop = False
		while not should_stop:
			message = self._receive_message()

			if message['header'] == 'ASK_COMMAND':
				# TODO remove
				self._send_command(message['snapshot'])
				should_stop = True
			elif message['header'] == 'GAME_FINISHED':
				should_stop = True
			elif message['header'] == 'ABORT':
				should_stop = True
			# TODO: Parse message
			# TODO: act accordingly
			# TODO: stop the worker if there is a problem


def my_ai(**kwargs):
	return 'Nothing'

if __name__ == '__main__':
	print('Starting the challenge server.')
	challenge_thread = threading.Thread(target=challenge_thread_work)
	challenge_thread.start()

	print('Waiting 2 seconds for the server to launch.')
	time.sleep(2)

	blue_agent = AIAgent(BLUE_USERNAME, BLUE_TEAM, my_ai)
	red_agent = AIAgent(RED_USERNAME, RED_TEAM, my_ai)

	print('Starting the agents')
	blue_agent.start()
	red_agent.start()

	blue_agent.join()
	red_agent.join()

	print('Agents joined.')

	print('Joining the challenge thread.')
	challenge_thread.join()

