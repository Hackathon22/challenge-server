from game_simulation import GameSimulation, SnapshotData
from agent import InvalidCommand, MoveCommand, ShootCommand, Command
import typing
import random
import math

def my_ai(gamestate: SnapshotData, my_data: typing.Dict) -> Command:
	def dodge(mispos1, mispos2, playerpos):
		# findsense if mispos2 plus proche de playerpos
		# if
		manhattan = lambda pos1, pos2: abs(pos1[0] - pos2[0]) + abs(pos1[1] - pos2[1])
		if manhattan(mispos1, playerpos) > manhattan(mispos2, playerpos):
			deltax = (mispos2[0] - mispos1[0])
			if deltax == 0:
				pente = float('inf')
			else:
				pente = ((mispos2[1] - mispos1[1]) / (mispos2[0] - mispos1[0]))
			p = mispos1[1] - (pente * mispos1[0])
			pverif = (pente * playerpos[0]) + p
			if pverif == playerpos[1]:
				mov = (-(mispos2[1] - mispos1[1]), -(mispos2[0] - mispos1[0]), 0.0)
				return (True, mov)
			else:
				return (False, None)
		else:
			return (False, None)

	def in_central_zone(player_pos):
		return -100 < player_pos[0] < 100 and -100 < player_pos[1] < 100

	def in_corridor(player_pos):
		if -275 < player_pos[0] < -25:
			if -275 < player_pos[0] < -225 and 275 < player_pos[1] < 25:
				return True
			elif -225 <= player_pos[0] < -25 and 275 < player_pos[1] < 225:
				return True
		elif 25 < player_pos[0] < 275:
			if 225 < player_pos[0] < 275 and -25 > player_pos[1] > -275:
				return True
			elif 25 < player_pos[0] <= 225 and -225 > player_pos[1] > -275:
				return True
		return False

	def predict_fm(list_pos):
		pos_init = list_pos[-2]
		pos_actual = list_pos[-1]
		delta_x = abs(pos_actual[0]) - abs(pos_init[0])
		delta_y = abs(pos_actual[1]) - abs(pos_init[1])
		return delta_x > delta_y

	def start_predict1(my_data):
		if predict_fm(my_data['other_player']):
			if my_data['tick_count'] < 7:
				return MoveCommand((1.0, 0.0, 0.0))
			elif my_data['tick_count'] == 7:
				return ShootCommand(gamestate.controlled_player.position, gamestate.other_player[0].position)
			else:
				return MoveCommand((0.0, -1.0, 0.0))
		else:
			if my_data['tick_count'] < 7:
				return MoveCommand((0.0, -1.0, 0.0))
			elif my_data['tick_count'] == 7:
				return ShootCommand(gamestate.controlled_player.position, gamestate.other_player[0].position)
			else:
				return MoveCommand((1.0, 0.0, 0.0))

	def start_predict2(my_data):
		if predict_fm(my_data['other_player']):
			if my_data['tick_count'] < 7:
				return MoveCommand((-1.0, 0.0, 0.0))
			elif my_data['tick_count'] == 7:
				return ShootCommand(gamestate.controlled_player.position, gamestate.other_player[0].position)
			else:
				return MoveCommand((0.0, 1.0, 0.0))
		else:
			if my_data['tick_count'] < 7:
				return MoveCommand((0.0, 1.0, 0.0))
			elif my_data['tick_count'] == 7:
				return ShootCommand(gamestate.controlled_player.position, gamestate.other_player[0].position)
			else:
				return MoveCommand((-1.0, 0.0, 0.0))

	def default_start1(random_path, death_count, my_data):
		if death_count:
			if random_path > 0.5:
				if my_data['tick_count'] < 8:
					return MoveCommand((0.0, -1.0, 0.0))
				elif my_data['tick_count'] == 8:
					angle_shoot = shoot_direct(gamestate.controlled_player.position,
											   gamestate.other_players[0].position)
					return ShootCommand(angle_shoot)
				elif my_data['tick_count'] > 8:
					return MoveCommand((math.cos(0.68), math.sin(0.68), 0.0))
			if random_path < 0.5:
				if my_data['tick_count'] < 8:
					return MoveCommand((1.0, 0.0, 0.0))
				elif my_data['tick_count'] == 8:
					angle_shoot = shoot_direct(gamestate.controlled_player.position,
											   gamestate.other_players[0].position)
					return ShootCommand(angle_shoot)
				elif my_data['tick_count'] > 8:
					return MoveCommand((-math.cos(0.88), -math.sin(0.88), 0.0))
		else:
			if len(my_data['other_player']) > 1:
				return start_predict1(my_data)
			else:
				return MoveCommand((0.0, 0.0, 0.0))

	def default_start2(random_path, death_count, my_data):
		if death_count:
			if random_path > 0.5:
				if my_data['tick_count'] < 8:
					return MoveCommand((0.0, 1.0, 0.0))
				elif my_data['tick_count'] == 8:
					angle_shoot = shoot_direct(gamestate.controlled_player.position,
											   gamestate.other_players[0].position)
					return ShootCommand(angle_shoot)
				elif my_data['tick_count'] > 8:
					return MoveCommand((-math.cos(0.68), -math.sin(0.68), 0.0))
			if random_path < 0.5:
				if my_data['tick_count'] < 8:
					return MoveCommand((-1.0, 0.0, 0.0))
				elif my_data['tick_count'] == 8:
					angle_shoot = shoot_direct(gamestate.controlled_player.position,
											   gamestate.other_players[0].position)
					return ShootCommand(angle_shoot)
				elif my_data['tick_count'] > 8:
					return MoveCommand((math.cos(0.88), math.sin(0.88), 0.0))
		else:
			if random_path > 0.5:
				if gamestate.controlled_player.position[1] < 0:
					return MoveCommand((0.0, 1.0, 0.0))
				elif gamestate.controlled_player.position[0] > 0:
					return MoveCommand((-1.0, 0.0, 0.0))
			elif random_path < 0.5:
				if gamestate.controlled_player.position[0] > 0:
					return MoveCommand((-1.0, 0.0, 0.0))
				elif gamestate.controlled_player.position[1] < 0:
					return MoveCommand((0.0, 1.0, 0.0))

	def default_inzone():
		angle = random.random() * 360
		while not in_central_zone((gamestate.controlled_player.position[0] + math.cos(angle) * 37.5,
								   gamestate.controlled_player.position[1] + math.sin(angle) * 37.5)):
			angle = random.random() * 360
		return MoveCommand((math.cos(angle), math.sin(angle), 0.0))

	def shoot_direct(pos_a, pos_b):
		angle = math.atan(pente(pos_a, pos_b)) * 360.0 / (2 * math.pi)
		if pos_b[0] - pos_a[0] < 0:
			angle += 180.0
		elif pos_b[0] - pos_a[0] == 0 and pos_b[1] - pos_a[1] < 0:
			angle += 180.0
		return angle

	def pente(a, b):
		if a[0] == b[0]:
			return float("inf")
		return (b[1] - a[1]) / (b[0] - a[0])

	def perpendiculare_axis(projectile_actual, projectile_previous):
		if projectile_actual[1] == projectile_previous[1]:
			pente_per = float('inf')
		else:
			pente_per = -(projectile_actual[0] - projectile_previous[0]) / (
						projectile_actual[1] - projectile_previous[1])
		return math.degrees(math.atan(pente_per))

	def generate_axis(base, variance):
		if random.random() > 0.5:
			base = 360.0 - base
		angle = base + random.randrange(-variance, variance)
		return angle % 360.0

	def projectile_prediction(pos, projectile_actual, projectile_previous):
		manhattan = lambda pos1, pos2: abs(pos1[0] - pos2[0]) + abs(pos1[1] - pos2[1])
		if manhattan(pos, projectile_actual) < 35:
			return None
		else:
			dodge_angle = manhattan(pos, projectile_actual) / 9
			perpendicular = perpendiculare_axis(projectile_actual, projectile_previous)
			move = generate_axis(perpendicular, dodge_angle)
			while in_central_zone((pos[0] + 37.5 * math.cos(move), pos[1] + 37.5 * math.sin(move))):
				move = generate_axis(perpendicular, dodge_angle)
			return move

	def corner_finder(team):
		# team 0 en haut a gauche
		if team == 0:
			pos_cible = (-90, 90)
		else:
			pos_cible = (90, -90)
		return pos_cible
	def goto_inzone(pos, pos_cible):
		mov = (0.0, 0.0)
		if pos[0] !=  pos_cible[0]:
			if pos[0] < pos_cible[0] + 1:
				mov = (1.0, 0.0)
			elif pos[0] > pos_cible[0] - 1:
				mov = (-1.0, 0.0)
		elif pos[0] == pos_cible[0] and pos[1] != pos_cible[1]:
			if pos[1] < pos_cible[1] + 1:
				mov = (0.0, 1.0)
			elif pos[0] > pos_cible[0] - 1:
				mov = (0.0, -1.0)
		return (mov[0], mov[1], 0.0)
	# ************ my_data *******************
	my_data['pos'] = gamestate.controlled_player.position
	if 'other_player' in my_data:
		my_data['other_player'].append(gamestate.other_players[0].position)
	else:
		my_data['other_player'] = [gamestate.other_players[0].position]
	if gamestate.controlled_player.position == (-250.0, 250.0, 0.0) or gamestate.controlled_player.position == (
	250.0, -250.0, 0.0):
		my_data['start'] = random.random()
		my_data['tick_count'] = 0
		if 'death_count' in my_data:
			my_data['death_count'] += 1
		else:
			my_data['death_count'] = 0
	else:
		my_data['tick_count'] += 1
	# ************** dodge ***************

	# ********** déplacement  and shoot *************
	pos_j1, pos_j2 = gamestate.controlled_player.position, gamestate.other_players[0].position
	dis_player = abs(pos_j1[0] - pos_j2[0]) + abs(pos_j1[1] - pos_j2[1])

	if -100 < gamestate.controlled_player.position[0] < 100 and -100 < gamestate.controlled_player.position[1] < 100:
			if dis_player < 150:
				shoot_angle = shoot_direct(pos_j1, pos_j2)
				shoot = ShootCommand(shoot_angle)
				return shoot
			elif not in_central_zone(pos_j2):
				shoot = shoot_direct(pos_j1, pos_j2)
				return ShootCommand(shoot)
			else:
				move = default_inzone()
	else:
		if gamestate.controlled_player.team == 0:
			move = default_start1(my_data['start'], my_data['death_count'], my_data)
		else:
			move = default_start2(my_data['start'], my_data['death_count'], my_data)
	return move


def my_data() -> typing.Dict:
	return {}