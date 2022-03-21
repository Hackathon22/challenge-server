import subprocess
import datetime
import threading
import typing
import socket
import time
import json
from dataclasses import dataclass
from abc import ABC, abstractmethod

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


@dataclass
class Command(ABC, json.JSONEncoder):
	'''Base class for all the commands'''
	command_type: str

@dataclass
class MoveCommand(Command):
	'''Asks the player to move. The direction will be normalized inside the game server 
	so there is no need to have a norm higher than 1 ;)
	Giving a move direction with a norm equal to 0 will put the character on IDLE state.
	'''
	move_direction: typing.Tuple[float, float, float]
	
	def __init__(self, move_direction):
		self.command_type = 'MOVE'
		self.move_direction = move_direction

@dataclass
class ShootCommand(Command):
	'''
	Asks the player where to shoot. The angle needs to be given in degrees and are directed
	counterclockwise between 0 and 360 deg.
	'''
	shoot_angle: float

	def __init__(self, shoot_angle):
		self.command_type = 'SHOOT'
		self.shoot_angle = shoot_angle

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
		return parsed_message

	@staticmethod
	def _parse_player_data(player_data: typing.Dict) -> PlayerData:
		player_position = (float(player_data['pos']['x']), 
									  float(player_data['pos']['x']),
									  float(player_data['pos']['y']))
		player_speed = (float(player_data['speed']['x']),
								   float(player_data['speed']['y']),
								   float(player_data['speed']['z']))
		player_health = float(player_data['health'])
		player_team = int(player_data['team'])
		player_score = float(player_data['score'])
		return PlayerData(player_position, player_speed, player_health, player_team, player_score)

	@staticmethod
	def _parse_projectile_data(projectile_data: typing.Dict) -> ProjectileData:
		projectile_position = (float(projectile_data['pos']['x']),
							   float(projectile_data['pos']['y']),
							   float(projectile_data['pos']['z']))
		projectile_speed =    (float(projectile_data['speed']['x']),
							   float(projectile_data['speed']['y']),
							   float(projectile_data['speed']['z']))
		return ProjectileData(projectile_position, projectile_speed)

	def _send_command(self, snapshot: typing.Dict) -> str:
		# Parses the message
		controlled_player_data = snapshot['controlledPlayer']
		controlled_player = self._parse_player_data(controlled_player_data)
		other_players_data = snapshot['otherPlayers']
		
		other_players = []
		for player_data in other_players_data:
			other_players.append(self._parse_player_data(player_data))

		projectiles = []
		projectiles_data = snapshot['projectiles']
		for projectile_data in projectiles_data:
			projectiles.append(self._parse_projectile_data(projectile_data))

		snapshot = SnapshotData(controlled_player, other_players, projectiles)

		# Asks for the command to the AI
		command = self.ai(snapshot)

		# Sends the command to the server
		serialized_command = json.dumps(command.__dict__)
#		print(f'Sending command: {serialized_command}')

		self._send_message(serialized_command)

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
			elif message['header'] == 'GAME_FINISHED':
				should_stop = True
			elif message['header'] == 'ABORT':
				should_stop = True
			# TODO: Parse message
			# TODO: act accordingly
			# TODO: stop the worker if there is a problem


def my_ai(gamestate: SnapshotData):
	print(f'Current gamestate: {gamestate}')
	return ShootCommand(270.0)
#	return MoveCommand((0.0, -1.0, 0.0))

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

