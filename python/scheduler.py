from dataclasses import dataclass
import typing
from game_simulation import GameSimulation
import json
import multiprocessing
from importlib import import_module
import os
import jdk

@dataclass
class ScheduledMatch:
	team_1: str
	team_2: str
	ai_time: float
	game_time: float
	commands_per_second: float
	save_file: str

@dataclass
class MatchResult:
	scheduled_match: ScheduledMatch
	score_team_1: str
	score_team_2: str
	winner: str
	aborted: bool
	message: str

@dataclass
class Pool:
	number: int
	players: typing.List[str]
	matches: typing.List[ScheduledMatch]
	results: typing.List[MatchResult]


AI_TIME = 150.0
GAME_TIME = 60.0
COMMANDS_PER_SECOND = 4.0
NB_PROCESSES = 4

JVM_PATH = None

def match_process_worker(port: int, match_queue: multiprocessing.Queue, match_result_queue: multiprocessing.Queue):
	while not match_queue.empty():
		match_schedule = match_queue.get()
		print(f'[Process {port}] Polled game simulation for teams: {match_schedule.team_1} - {match_schedule.team_2}')

		first_ai_file_name = os.path.join('teams', match_schedule.team_1, 'my_ai').replace('/', '.')
		second_ai_file_name = os.path.join('teams', match_schedule.team_2, 'my_ai').replace('/', '.')

		module_team_1 = import_module(first_ai_file_name)
		module_team_2 = import_module(second_ai_file_name)

		ai_team_1 = getattr(module_team_1, 'my_ai')
		data_team_1 = getattr(module_team_2, 'my_data')

		ai_team_2 = getattr(module_team_1, 'my_ai')
		data_team_2 = getattr(module_team_2, 'my_data')

		current_dir = os.getcwd()

		# Changes the directory to load the data inside the team folders
		os.chdir(os.path.join('teams', match_schedule.team_1))

		print(f'[Process {port}] Loading data for team {match_schedule.team_1}')
		loaded_data_team_1 = data_team_1()
		print(f'[Process {port}] Loading data for team {match_schedule.team_2}')
		loaded_data_team_2 = data_team_2()

		# Rolls bacj the current directory 
		os.chdir(current_dir)

		simulation = GameSimulation(JVM_PATH, match_schedule.game_time, match_schedule.ai_time, match_schedule.commands_per_second, match_schedule.save_file, port)

		simulation.set_first_agent(match_schedule.team_1, ai_team_1, loaded_data_team_1)
		simulation.set_second_agent(match_schedule.team_2, ai_team_2, loaded_data_team_2)

		print(f'[Process {port}] Started game simulation for teams: {match_schedule.team_1} - {match_schedule.team_2}')

		simulation.start_round()

		results, _ = simulation.end_round()

		winner = None
		for agent_result in results:
			if agent_result.won:
				winner = agent_result.username

		match_result = MatchResult(match_schedule, results[0].score, results[1].score, winner, results[0].aborted, results[0].error_message)
		print(f'[Process {port}] Finished game simulation for teams: {match_schedule.team_1} - {match_schedule.team_2}, aborted: {match_result.aborted}, winner: {match_result.winner}')

		match_result_queue.put(match_result)

	match_result_queue.put(None)


if __name__ == '__main__':

	try:
		jdk.uninstall('15', jre=True)
	except:
		pass

	jvm_path = jdk.install('15', jre=True)
	JVM_PATH = os.path.join(jvm_path, 'bin', 'java')

	with open('pool_data.json', 'r') as file:
		pool_data = json.load(file)

	pools = []
	
	for pool in pool_data['pools']:
		pool_number = pool['pool_number']
		pool_teams = []
		for team in pool['teams']:
			pool_teams.append(team['team_name'])

		pool_matches = []
		for team_1 in pool_teams:
			for team_2 in pool_teams:
				if team_1 != team_2:
					match = ScheduledMatch(team_1, team_2, AI_TIME, GAME_TIME, COMMANDS_PER_SECOND, f'/game_saves/{team_1}_{team_2}_pool{pool_number}.json')
					pool_matches.append(match)

		pools.append(Pool(pool_number, pool_teams, pool_matches, None))

	total_matches = []
	for pool in pools:
		total_matches += pool.matches

	print(total_matches)

	matches_per_process = len(total_matches) // NB_PROCESSES
	all_processes = []

	for idx in range(NB_PROCESSES):
		match_queue = multiprocessing.Queue()
		result_queue = multiprocessing.Queue()
		process = multiprocessing.Process(target=match_process_worker, args=(2049 + idx, match_queue, result_queue))
		all_processes.append((process, result_queue))
		
		if idx == NB_PROCESSES - 1:
			for match in total_matches[idx*matches_per_process:]:
				match_queue.put(match)
		else:
			for match in total_matches[idx*matches_per_process:(idx+1)*matches_per_process]:
				match_queue.put(match)

		process.start()

	matches_results = []

	for process, queue in all_processes:
		keep = True
		while keep:
			result = queue.get()
			if result is None:
				keep = False
				process.join()
			else:
				matches_results.append(result)

	# Count the wins
	for pool in pools:
		player_wins = {}
		for player in pool.players:
			player_wins[player] = 0
		for result in matches_results:
			if result.winner in pool.players:
				player_wins[result.winner] += 1
		print(f'Pool: {pool} player wins:\n{player_wins}')


