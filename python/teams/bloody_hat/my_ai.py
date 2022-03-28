from game_simulation import GameSimulation, SnapshotData
from agent import InvalidCommand, MoveCommand, ShootCommand, Command
import typing
import time
import math
import random

def my_ai(gamestate: SnapshotData, my_data: typing.Dict) -> Command:

	MIDDLE = (0.0, 0.0, 0.0)
	OUTER_LEFT = (-245.0, -40.0, 0.0)
	OUTER_RIGHT = (250.0, 50.0, 0.0)

	OUTER_TOP =  (0.0, 250, 0.0)
	OUTER_BOTTOM = (0.0, -250, 0)
	TEST_SPOT = (-125.0, 275.0, 0.0)
	SHOOT = (1000.0, 1000.0, 1000.0)
	INNER_TOP_LEFT=(-90,90)
	INNER_BOTTOM_RIGHT=(90,-90)

	MOVEMENTS = [(0.0, 1.0, 0.0),
				 (0.0, -1.0, 0.0),
				 (1.0, 0.0, 0.0),
				 (-1.0, 0.0, 0.0),
				 (1.0, 1.0, 0.0),
				 (-1,0, 1.0, 0.0),
				 (1,0, -1.0, 0.0),
				 (-1,0, -1.0, 0.0)]

	LARGEUR = 20.0


	def check_pos(pos, a, spot):
		if (spot[a] - LARGEUR < pos[a] < spot[a] + LARGEUR):
				return True
		else:
				return False

	def track_player_angle(gamestate: SnapshotData, my_data: typing.Dict) -> float:
		if (gamestate.controlled_player.team == 1 and gamestate.other_players[0].health <= 0):
			return 135

		if (gamestate.controlled_player.team == 0 and gamestate.other_players[0].health <= 0):
			return 315
		target_x = gamestate.other_players[0].position[0] + gamestate.other_players[0].speed[0]*0.2 + 0.0007*(gamestate.other_players[0].position[0] - gamestate.controlled_player.position[0])*gamestate.other_players[0].speed[0]
		target_y = gamestate.other_players[0].position[1] + gamestate.other_players[0].speed[1]*0.2 + 0.0007*(gamestate.other_players[0].position[1] - gamestate.controlled_player.position[1])*gamestate.other_players[0].speed[1]
		current_x = gamestate.controlled_player.position[0]
		current_y = gamestate.controlled_player.position[1]
		delta_x = target_x - current_x
		delta_y = target_y - current_y
		angle = math.atan2(delta_y, delta_x)
		return math.degrees(angle)

	def onSpot(pos, spot):
		if (check_pos(pos, 1, spot) and check_pos(pos, 0, spot)):
		 	return True
		return False

	def next_position(my_data: typing.Dict, positions, position):
		p = my_data['position']
		if p < len(positions):
				position = positions[p]
				my_data['position'] = p+1
		else:
				position = positions[-1]

	def ai(gamestate: SnapshotData, my_data: typing.Dict, positions):
		position = positions[0]
		if 'position' not in my_data or gamestate.controlled_player.health <= 0:
			my_data['position'] = 0
		else:
			p = my_data['position']
			if p < len(positions):
				position = positions[p]
			else:
				position = positions[-1]

		if position == SHOOT:
			if gamestate.other_players[0].health <= 0.0:
				next_position(my_data, positions, position)
			angle = track_player_angle(gamestate, my_data)
			return ShootCommand(angle)


		if  onSpot(gamestate.controlled_player.position, position):
			next_position(my_data, positions, position)
		return go_to_pos(position, gamestate, my_data)

	def go_to_pos(pos, gamestate: SnapshotData, my_data:typing.Dict) -> Command:
	    target_x = pos[0]
	    target_y = pos[1]
	    current_x = gamestate.controlled_player.position[0]
	    current_y = gamestate.controlled_player.position[1]
	    move_x=0.0
	    move_y=0.0
	    largeur=20.0
		## X movement ##
	    # If target_x is on right
	    mul = 0
	    #if gamestate.controlled_player.team == 1:
	    #    mul = 1.25
	    if (current_x > target_x + largeur):
	        move_x=-1.0 + random.random() * mul
	    # If target_y is on left
	    elif (current_x <target_x- largeur):
	        move_x=1.0 + random.random() * mul

	    if (current_y > target_y + largeur):
	        move_y=-1.0 + random.random() * mul
	    # If target_y is on left
	    elif (current_y < target_y-largeur):
	        move_y=1.0 + random.random() * mul

	    return MoveCommand((move_x, move_y, 0.0))

	# PTIT_COUP_DE_PUTE = [(DANGEROUS_SPOT), SHOOT, MIDDLE]
	# EN BAS A DROITE
	SMART_0 = [OUTER_LEFT, SHOOT, INNER_BOTTOM_RIGHT, SHOOT]
	SMART_0_1 = [OUTER_TOP, SHOOT, INNER_BOTTOM_RIGHT, SHOOT]
	strats_0 = [SMART_0, SMART_0_1]
	if 'strats0' not in my_data:
		my_data['strats0'] = random.choice(strats_0)


	# EN HAUT A GAUCHE
	SMART_1 = [OUTER_RIGHT, SHOOT, INNER_TOP_LEFT, SHOOT]
	SMART_1_1 = [OUTER_BOTTOM, SHOOT, INNER_TOP_LEFT, SHOOT]
	strats_1 = [SMART_1, SMART_1_1]

	if 'strats1' not in my_data:
		my_data['strats1'] = random.choice(strats_1)
	player = gamestate.controlled_player
	if (player.team == 0):
		#return ai(gamestate, my_data, [(-250.0, -100.0, 0.0), SHOOT,(0.0, 0.0, 0.0),(50.0, 0.0, 0.0),(50.0, 50.0, 0.0)])
		return ai(gamestate, my_data, my_data['strats0'])
	else:
		return ai(gamestate, my_data, my_data['strats1'])

def my_data() -> typing.Dict:
	return {}
