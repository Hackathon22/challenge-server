import subprocess
import datetime
import threading
import typing
import socket
import time
import json
from dataclasses import dataclass
from abc import ABC, abstractmethod
import traceback
from contextlib import contextmanager
from func_timeout import func_timeout, FunctionTimedOut

JAR_FILE = 'challenge.jar'

BLUE_USERNAME = 'player_0'
BLUE_TEAM = 0

RED_USERNAME = 'player_1'
RED_TEAM = 1


def challenge_thread_work(jvm_path: str, file: str = f'{str(datetime.datetime.now())}.hackathon',
 game_time: float = 60.0, ai_time: float = 150.0, commands_per_second: int = 4, port: int = 2049):
	current_datetime = datetime.datetime.now()
	process_result = subprocess.run([jvm_path, '-jar', '--illegal-access=warn', JAR_FILE, '-m', 'windowless',
	 '-f', file, '-t', str(game_time), '-a', str(ai_time), '-c', str(commands_per_second), '-p', str(port)])
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


@dataclass
class InvalidCommand(Command):
	'''
	Invalid command as a crash test for the server
	'''
	whatever_value : str

	def __init__(self, value):
		self.command_type = 'INVALID'
		self.whatever_value = value


@dataclass
class AgentResult:
	username: str
	team: int
	score: float
	won: bool
	aborted: bool
	error_message: str
	blame: str

class AIAgent:

	def __init__(self, username : str, team: int, ai: typing.Callable[[], str], data: typing.Dict = None, ai_time: float = 150.0, address: str = '127.0.0.1', port: int = 2049):
		self.username = username
		self.team = team
		self.ai = ai
		self._remaining_time = ai_time
		self._port = port
		self._address = address
		self._socket = None
		self._thread = None

		self._data = data

		self.results = None
		self.aborted = False
		self.abort_message = None
		self.abort_blame = None

	def _connect(self):
		self._socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		self._socket.connect((self._address, self._port))
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
									  float(player_data['pos']['y']),
									  float(player_data['pos']['z']))
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

	def _send_command(self, snapshot: typing.Dict):
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
		try:
			begin = datetime.datetime.now()
			
			command = func_timeout(self._remaining_time, self.ai, args=(snapshot, self._data))
			
			end = datetime.datetime.now()
			elapsed_seconds = (end - begin).total_seconds()
			self._remaining_time -= elapsed_seconds

			if (type(command) not in [ShootCommand, MoveCommand, InvalidCommand]):
				raise Exception(f'Invalid command type returned by ai: {type(command)}')
				
		except FunctionTimedOut as exc:
			print(f'Agent timed out inside AI function. Sending an invalid command to abort the game.')
			command = InvalidCommand(f'Agent timed out during AI function.')
		except Exception as exc:
			# In case there is problem inside the function coded by the participants
			print(f'Exception during the AI function:\n\t{exc}\nSending an invalid command to abort the game.')
			traceback.print_exc()
			command = InvalidCommand(f'Exception during the ai function:\n{exc}')

		# Sends the command to the server
		serialized_command = json.dumps(command.__dict__)

		self._send_message(serialized_command)

	def _parse_results(self, score_results: typing.Dict) -> AgentResult:
		for result in score_results:
			if int(result['team']) == self.team:
				score = float(result['score'])
				won = bool(result['won'])
				return AgentResult(self.username, self.team, score, won, False, None, None)

	def _parse_abortion(self, error: str, blame: str):
		return AgentResult(self.username, self.team, 0.0, False, True, error, blame)

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
				self.results = self._parse_results(message['score'])
				should_stop = True
			elif message['header'] == 'ABORT':
				self.results = self._parse_abortion(message['error'], message['blame'])
				should_stop = True

