import subprocess
import datetime
import threading
import typing
import socket
import time

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



class AIAgent:

	def __init__(self, username : str, team: int):
		self.username = username
		self.team = team
		self._socket = None
		self._thread = None

	def connect(self, port: int = 2049, address: str = '127.0.0.1'):
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

	def start(self):
		self._thread = threading.Thread(target=_work)

	def _work(self):
		pass


if __name__ == '__main__':
	print('Starting the challenge server.')
	challenge_thread = threading.Thread(target=challenge_thread_work)
	challenge_thread.start()

	print('Waiting 2 seconds for the server to launch.')
	time.sleep(2)

	blue_agent = AIAgent(BLUE_USERNAME, BLUE_TEAM)
	blue_agent.connect()

	red_agent = AIAgent(RED_USERNAME, RED_TEAM)
	red_agent.connect()

	print('Joining the challenge thread!')
	challenge_thread.join()

