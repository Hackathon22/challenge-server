from game_simulation import GameSimulation, SnapshotData
from agent import InvalidCommand, MoveCommand, ShootCommand, Command
import typing
import time
import math
import collections
import functools
import random

def my_ai(gamestate: SnapshotData, my_data: typing.Dict) -> Command:
	true_pos = collections.namedtuple("Pos", "x y z")
	pos = lambda a, b: true_pos(x=a, y=b, z=0)
	EPSILON = 2.5

	# Pos and size const
	MAP_POS = pos(-100, 100)
	MAP_SIZE = pos(200, -200)

	UP_EDGE = pos(-275, 275)
	UP_RIGHT_CORNER_1 = pos(-275 + 3 * 32, 275 - 8 * 32)
	UP_RIGHT_CORNER_2 = pos(-275 + 8 * 32, 275 - 3 * 32)

	DOWN_EDGE = pos(275, -275)
	DOWN_LEFT_CORNER_1 = pos(275 - 8 * 32, -275 + 3 * 32)
	DOWN_LEFT_CORNER_2 = pos(275 - 3 * 32, -275 + 8 * 32)

	CENTER_LEFT = pos(-95, 95)
	CENTER_RIGHT = pos(95, -95)

	SAFE_AURA = 150
	SIZE_HIT_BOX = 50

	ZONE_1_L = pos(-275, 100)
	ZONE_1_R = pos(-100, 100)
	ZONE_2_L = ZONE_1_R 
	ZONE_2_R = pos(100, -275)
	ZONE_3_L = pos(100, 100)
	ZONE_3_R = pos(275, -100)
	ZONE_4_R = ZONE_3_L 
	ZONE_4_L = pos(-100, 275)

	def is_in_zone(pos, top_left, bottom_right):
	    return top_left.x <= pos.x <= bottom_right.x and bottom_right.y <= pos.y <= top_left.y


	in_safe_up = lambda pos: is_in_zone(pos, UP_EDGE, UP_RIGHT_CORNER_1) or is_in_zone(pos, UP_EDGE, UP_RIGHT_CORNER_2)
	in_safe_down = lambda pos: is_in_zone(pos, DOWN_LEFT_CORNER_1, DOWN_EDGE) or is_in_zone(pos, DOWN_LEFT_CORNER_2, DOWN_EDGE)
	in_center = lambda pos: is_in_zone(pos, CENTER_LEFT, CENTER_RIGHT)
	to_the_moon = lambda pos: is_in_zone(pos, ZONE_1_L, ZONE_1_R) or is_in_zone(pos, ZONE_2_L, ZONE_2_R) or is_in_zone(pos, ZONE_3_L, ZONE_3_R) or is_in_zone(pos, ZONE_4_L, ZONE_4_R)
	in_zone_a = functools.partial(is_in_zone, top_left=ZONE_1_L, bottom_right=ZONE_1_R)
	in_zone_b = functools.partial(is_in_zone, top_left=ZONE_2_L, bottom_right=ZONE_2_R)
	in_zone_c = functools.partial(is_in_zone, top_left=ZONE_3_L, bottom_right=ZONE_3_R)
	in_zone_d = functools.partial(is_in_zone, top_left=ZONE_4_L, bottom_right=ZONE_4_R)

	# euhhh
	in_left_down = lambda pos: is_in_zone(pos, BOT_LEFT_LEFTPOINT, BOT_LEFT_RIGHTPOINT)
	in_right_down = lambda pos: is_in_zone(pos, BOT_RIGHT_LEFTPOINT, BOT_RIGHT_RIGHTPOINT)
	in_left_up = lambda pos : is_in_zone(pos,TOP_LEFT_LEFTPOINT, TOP_LEFT_RIGHTPOINT)
	in_right_up = lambda pos : is_in_zone(pos, TOP_RIGHT_LEFTPOINT, TOP_LEFT_RIGHTPOINT)

	# Si joueur 0 (pos_i en haut à gauche)
	BOT_POINT = (200, -200)
	BOT_LEFT_LEFTPOINT = pos(200, -225)
	BOT_LEFT_RIGHTPOINT = pos(225, -275)
	BOT_RIGHT_LEFTPOINT = pos(-200, 225)
	BOT_RIGHT_RIGHTPOINT = pos(275, -225)
	TOP_SHOOT_RIGHT = pos(-43, 225)
	TOP_SHOOT_LEFT = pos(-225, 43)

	# Si joueur 1 (pos_i en bas à droite)
	TOP_POINT = (-200, 200)
	TOP_LEFT_LEFTPOINT = pos(-275, 225)
	TOP_LEFT_RIGHTPOINT = pos(-225, 43)
	TOP_RIGHT_LEFTPOINT = pos(-225, 275)
	TOP_RIGHT_RIGHTPOINT = pos(-43, 225)
	BOT_SHOOT_RIGHT = pos(225, -43)
	BOT_SHOOT_LEFT = pos(43, -225)
	# euhh


	def signum(n: float) -> int:
		if n >= 0:
			return 1
		elif n:
			return -1
		return 0


	def add_pos(pos1, pos2):
		return pos(pos1.x + pos2.x, pos1.y + pos2.y)


	def shoot(my_pos, op_pos, op):
		op_speed = true_pos(*op.speed)
		r = math.dist(my_pos, op_pos)
		speed_wa = normalize(op_speed)
		theta = math.acos(min((op_pos.x + speed_wa.x * r/300 - my_pos.x) / r, 1)) 
		if math.fabs((op_pos.y - my_pos.y + speed_wa.y * r/300) - r * math.sin(theta)) > EPSILON:
			theta = -theta
		return ShootCommand(math.degrees(theta))


	def normalize(posi):
		if posi.x or posi.y:
			k = 1 / (posi.x ** 2 + posi.y ** 2)
			return pos(k * posi.x, k * posi.y)
		return pos(signum(posi.x), signum(posi.y))


	def stay_in_center(x, y, my_pos):
		k = 1/math.sqrt(x ** 2 + y ** 2)
		if not in_center(pos(k * 150 * x + my_pos.x, k * 150 * y + my_pos.y)):
			return MoveCommand((x, y, 2 * math.sqrt(abs(1 - x ** 2 - y ** 2))))
		return MoveCommand((x, y, 0))


	def closest_projectile(my_pos, projectiles):
		closest = min(projectiles, key=lambda b: math.dist(my_pos, b.position))
		return true_pos(*closest.position), true_pos(*closest.speed)


	def simulate_displacement(my_pos, hypothetical_speed, missile_pos): 
		speedo = normalize(hypothetical_speed)
		sp = pos(speedo.x * math.dist(my_pos, missile_pos) / 2
			, speedo.y * math.dist(my_pos, missile_pos) / 2)
		return add_pos(my_pos, sp)


	def doge(my_pos):
		dep_vec = pos(my_pos.x * (-1), my_pos.y * (-1))
		if in_zone_a(my_pos) or in_zone_c(my_pos):
			return MoveCommand(pos(0.2 * signum(dep_vec.x), random.choice([-1, 1])))
		return MoveCommand(pos(random.choice([-1, 1]), 0.2 * signum(dep_vec.y)))

	def targetting_me(my_pos, missile_pos, missile_speed): 
		t = (my_pos.x - missile_pos.x) / missile_speed.x
		if missile_speed.y == 0: 
			return True	
		return abs((my_pos.y - missile_pos.y) / missile_speed.y - t) < SIZE_HIT_BOX	

	def need_doge(my_pos, projectiles):
		proj_pos, proj_speed = closest_projectile(my_pos, projectiles)	
		dist = math.dist(my_pos, proj_pos)
		#print(proj_pos, proj_speed, dist)
		if SAFE_AURA > dist and targetting_me(my_pos, proj_pos, proj_speed):
			return dist
		return False


	player = gamestate.controlled_player
	opponent = gamestate.other_players[0]
	our_pos = true_pos(*player.position)
	opponent_pos = true_pos(*opponent.position)
	wants_to_shoot = not random.randint(0, 3)

	if in_safe_up(our_pos):
		return MoveCommand((0.0, -1.0, 0.0))
	elif in_safe_down(our_pos):
		return MoveCommand((0.0, 1.0, 0.0))

	# yaaaaaaaaa
	projectiles = gamestate.projectiles
	if len(projectiles) > 0:
		proj_pos, proj_speed = closest_projectile(our_pos, projectiles)
		if math.dist(our_pos, proj_pos) < SAFE_AURA: 
			return doge(our_pos)
	# ouuuuuuuuu

	if in_center(our_pos):
		if in_safe_up(opponent_pos) or in_safe_down(opponent_pos):
			return shoot(our_pos, opponent_pos, opponent)
		if in_safe_up(opponent_pos) or in_safe_down(opponent_pos) or opponent.health <= 0:
			return stay_in_center(random.uniform(-1, 1), random.uniform(-1, 1), our_pos)
		return random.choice([* 5*[shoot(our_pos, opponent_pos, opponent)],
			*[stay_in_center(random.uniform(-1, 1), random.uniform(-1, 1), our_pos)]])
	elif to_the_moon(our_pos): 
		if projectiles:
			missile_pos, missile_speed = closest_projectile(our_pos, projectiles)
			if targetting_me(our_pos, missile_pos, missile_speed): 
				return random.choice([*3*[doge(our_pos)], shoot(our_pos, opponent_pos, opponent)])
	
	if -100 <= our_pos.x <= 100:
		dy = -signum(our_pos.y)
		random_factor = random.uniform(-0.3, 0.3)
		if opponent.health <= 0: 
			return MoveCommand(pos(random_factor, dy))
		return random.choice([*2 * [MoveCommand(pos(random_factor, dy))], shoot(our_pos, opponent_pos, opponent)])
	else:
		dx = -signum(our_pos.x)
		random_factor = random.uniform(-0.3, 0.3)
		if opponent.health <= 0:
			return MoveCommand(pos(dx, random_factor))
		return random.choice([*3 * [MoveCommand(pos(dx, random_factor))], shoot(our_pos, opponent_pos, opponent)])

	if wants_to_shoot:
		return shoot(our_pos, opponent_pos, opponent)
	return MoveCommand((0.0, 1.0, 0))


def my_data():
	return dict()
