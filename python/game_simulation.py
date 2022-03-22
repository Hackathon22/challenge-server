from agent import AIAgent, challenge_thread_work, SnapshotData, Command, AgentResult
import multiprocessing
import typing
import threading
import datetime
import time

class GameSimulation:

	def __init__(self, jvm_path: str, game_time: float = 60.0, ai_time: float = 150.0,
	  commands_per_second: int = 4, save_file: str = f'{str(datetime.datetime.now())}.hackathon', port: int = 2049):
		self._first_agent_username = None
		self._second_agent_username = None

		self._first_agent_ai = None
		self._second_agent_ai = None

		self._first_agent_data = None
		self._second_agent_data = None

		self._process = None

		self.game_time = game_time
		self.ai_time = ai_time
		self.commands_per_second = commands_per_second
		self.save_file = save_file
		self._jvm_path = jvm_path
		self._port = port

	def set_first_agent(self, username: str, ai: typing.Callable[[SnapshotData, typing.Dict], Command], data: typing.Dict):
		self._first_agent_username = username
		self._first_agent_ai = ai
		self._first_agent_data = data

	def set_second_agent(self, username: str, ai: typing.Callable[[SnapshotData, typing.Dict], Command], data: typing.Dict):
		self._second_agent_username = username
		self._second_agent_ai = ai
		self._second_agent_data = data

	def _round_worker(self, pipe_out: multiprocessing.Pipe):
		challenge_thread = threading.Thread(target=challenge_thread_work, args=(self._jvm_path, self.save_file, self.game_time, self.ai_time, self.commands_per_second, self._port))
		challenge_thread.start()

		time.sleep(1)

		first_agent = AIAgent(self._first_agent_username, 0, self._first_agent_ai, self._first_agent_data, port=self._port)
		second_agent = AIAgent(self._second_agent_username, 1, self._second_agent_ai, self._second_agent_data, port=self._port)

		first_agent.start()
		second_agent.start()

		first_agent.join()
		second_agent.join()

		challenge_thread.join()

		pipe_out.send(([first_agent.results, second_agent.results], [self._first_agent_data, self._second_agent_data]))

	def start_round(self) -> typing.List[AgentResult]:
		if self._process is not None:
			raise Exception('This game simulation already has a process running.')

		assert self._first_agent_username is not None
		assert self._first_agent_ai is not None

		assert self._second_agent_username is not None
		assert self._second_agent_ai is not None

		self._recv_end, send_end = multiprocessing.Pipe(False)

		self._process = multiprocessing.Process(target=self._round_worker, args=(send_end,))
		self._process.start()


	def end_round(self) -> typing.Tuple[typing.List[AgentResult], typing.List[typing.Any]]:
		if self._process is None:
			raise Exception('No round launched for this game simulation.')

		self._process.join()
		agents_results, agents_data = self._recv_end.recv()

		self._process = None

		return agents_results, agents_data
