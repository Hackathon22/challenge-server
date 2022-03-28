import math							#TODO: spécifier dans l'email
import numpy as np
from game_simulation import GameSimulation, SnapshotData
from agent import InvalidCommand, MoveCommand, ShootCommand, Command
import typing


def my_ai(gamestate: SnapshotData, my_data: typing.Dict) -> Command:
	x1, y1, z = gamestate.controlled_player.position
	x2, y2, z2 = gamestate.other_players[0].position
	if "inPosition" not in my_data.keys():				# variable globale servaut à savoir si on est en position initiale pour tirer sur l'ennemi
		my_data["inPosition"] = False
	if "inCadre" not in my_data.keys():					# variable globale pour savoir quand on se trouve placé dans le cadre (ici en haut à gauche)
		my_data["inCadre"] = False
	if "isLeft" not in my_data.keys():
		my_data['isLeft'] = True
	
	diff_pos_p2 = None
	if "P2 previous position" not in my_data.keys():
		my_data["P2 previous position"] = gamestate.other_players[0].position
		px2, py2, pz2 = gamestate.other_players[0].position
		diff_pos_p2 = px2 - x2, py2 - y2, pz2 - z2 
	else:
		px2, py2, pz2 = my_data["P2 previous position"]
		diff_pos_p2 = x2 - px2, y2 - py2, pz2 - z2 
		my_data["P2 previous position"] = gamestate.other_players[0].position
		nx2, ny2, nz2 = diff_pos_p2
		
	
	if gamestate.controlled_player.health <= 0:			# quand on meurt, on doit retourner aux places d'avant
		my_data['inPosition'] = False
		my_data['inCadre'] = False

	if not my_data['inPosition']:
		if x1>0:
			my_data['isLeft']=False
	
	if not my_data['inPosition']:						# on se met en premiere position pour tirer dès qu'on voit l'ennemi
		if my_data['isLeft']:
			if y1 > 25:				# on le fait descendre
				return MoveCommand((0.0, -1.0, 0.0))
			elif y1 < -12.5:			# on le fait monter
				return MoveCommand((0.0, 1.0, 0.0))
			
			if x1+37.5 <= -212.5:
				my_data['inPosition']=True
				return MoveCommand((1.0, -1.0, 0.0))

		else:
			if y1 < -25:		# on le fait monter
				return MoveCommand((0.0, 1.0, 0.0))
			elif y1 > 12.5:		# on le fait descendre
				return MoveCommand((0.0, -1.0, 0.0))
			
			if x1-37.5 >= 212.5:
				my_data['inPosition']=True
				return MoveCommand((-1.0, 1.0, 0.0))
		
	
	if gamestate.other_players[0].health>=0:                        # on tire sur l'ennemi dès que sa vie est + grande que 0
		point1 = np.array(gamestate.controlled_player.position)
		point2 = np.array(gamestate.other_players[0].position)
		if not my_data['inCadre']:
			toa1 = math.degrees( math.atan(abs(y2-y1) / abs(x2-x1)))
			toa2 = math.degrees( math.atan(abs(x2-x1) / abs(y2-y1)))
		
		elif ((np.linalg.norm(point1 - point2)) < 250):
			toa1 = math.degrees( math.atan(abs(y2-y1) / abs(x2-x1)))
			toa2 = math.degrees( math.atan(abs(x2-x1) / abs(y2-y1)))
		elif ((np.linalg.norm(point1 - point2)) > 250):
			toa1 = math.degrees( math.atan(abs((y2+ny2*3)-y1) / abs((x2+nx2*3)-x1)) ) #far
			toa2 = math.degrees( math.atan(abs((x2+nx2*3)-x1) / abs((y2+ny2*3)-y1)) ) #far
		elif ((np.linalg.norm(point1 - point2)) > 350):
			toa1 = math.degrees( math.atan(abs((y2+ny2*4)-y1) / abs((x2+nx2*4)-x1)) ) #far
			toa2 = math.degrees( math.atan(abs((x2+nx2*4)-x1) / abs((y2+ny2*4)-y1)) ) #far
		else:
			toa1 = math.degrees( math.atan(abs((y2+ny2*5)-y1) / abs((x2+nx2*5)-x1)) ) #far
			toa2 = math.degrees( math.atan(abs((x2+nx2*5)-x1) / abs((y2+ny2*5)-y1)) ) #far
		
		if x1<x2 and y1<=y2:
			return ShootCommand(toa1)

		elif x1<=x2 and y1>y2:
			return ShootCommand(toa2+270)

		elif x1>x2 and y1>=y2:
			return ShootCommand(toa1+180)
		
		elif x1>=x2 and y1<y2:
			return ShootCommand(toa2+90)

	
	if not my_data['inCadre']:						# on se place dans le cadre
		if my_data['isLeft']:						# ici en haut à gauche
			if x1<-100:
				return MoveCommand((1.0, 0.0, 0.0))
			elif x1>-62.5:
				return MoveCommand((-1.0, 0.0, 0.0))
			
			if y1<62.5:
				newPos = y1+37.5
				if newPos>62.5 and newPos<100:
					my_data['inCadre'] = True
				return MoveCommand((0.0, 1.0, 0.0))

		else:										# ici en bas à droite
			if x1>100:
				return MoveCommand((-1.0, 0.0, 0.0))
			elif x1<62.5:
				return MoveCommand((1.0, 0.0, 0.0))
			
			if y1>-62.5:
				newPos = y1-37.5
				if newPos<-62.5 and newPos>-100:
					my_data['inCadre'] = True
				return MoveCommand((0.0, -1.0, 0.0))

	return MoveCommand((0.0, 0.0, 0.0))
			


def my_data() -> typing.Dict:
	return {}
