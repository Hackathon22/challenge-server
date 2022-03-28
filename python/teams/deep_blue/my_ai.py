from lib2to3.pytree import HUGE

from numpy import arctan
from game_simulation import GameSimulation, SnapshotData
from agent import InvalidCommand, MoveCommand, ShootCommand, Command
import typing
import random
import math
import time

def my_data():
	return {}

def my_ai(gamestate: SnapshotData, my_data: typing.Dict) -> Command:
	scoreMe = gamestate.controlled_player.score
	scoreHim = gamestate.other_players[0].score
	healthMe = gamestate.controlled_player.health
	healthHim = gamestate.other_players[0].health
	xMe = gamestate.controlled_player.position[0]
	yMe = gamestate.controlled_player.position[1]
	xHim = gamestate.other_players[0].position[0]
	yHim = gamestate.other_players[0].position[1]
 
	def moveTo(x,y):
		if esquive() != -1:
			projectile = gamestate.projectiles[esquive()]
			lol = bool(random.getrandbits(1))
			if lol:
				print((projectile.speed[1]/300, -projectile.speed[0]/300,0.0))
				return MoveCommand((projectile.speed[1]/300, -projectile.speed[0]/300,0.0))
			else:
				return MoveCommand((-projectile.speed[1]/300, projectile.speed[0]/300,0.0))
		if y==yMe and x==xMe:
			return ShootCommand(0.0)
		if x==xMe:
			return MoveCommand((0.0,math.copysign(1.0,y-yMe),0.0))
		if y==yMe:
			return MoveCommand((math.copysign(1.0,x-xMe),0.0,0.0))
		return MoveCommand(((x-xMe)/math.sqrt((xMe-x)*(xMe-x)+(yMe-y)*(yMe-y)), (y-yMe)/math.sqrt((xMe-x)*(xMe-x)+(yMe-y)*(yMe-y)),0.0))
	
	def zone(x,y):
			if y > 216:
				if x < -216:
					return "CornerUpLeft"
				elif x < -34:
					return "CornerUpAlmostLeft"
				elif x < 216:
					return "CenterUp"
				else:
					return "CornerUpRight"
			elif y > 0 and x < -216:
				return "CornerLeftAlmostUp"
			elif y < 0 and x > 216:
				return "CornerRightAlmostDown"
			elif y < -216:
				if x > 216:
					return "CornerDownRight"
				elif x > 34:
					return "CornerDownAlmostRight"
				elif x >-216:
					return "CenterDown"
				else:
					return "CornerDownLeft"
			elif x < 100 and x >-100 and y > -100 and y < 100:
				return "Center"
			return "Middle"

	def inDanger(projectile):
		if healthHim > 0 and healthMe >0:
			xPro = projectile.position[0]
			yPro = projectile.position[1]
			xSpeed = projectile.speed[0]
			ySpeed = projectile.speed[1]
			if xSpeed !=0:
				angleTir = (-math.copysign(1,xSpeed)*90+90)+arctan(ySpeed/xSpeed)*180/3.14
			if (xPro-xMe)!=0:
				angleMe = (-math.copysign(1,xPro-xMe)*90+90)+arctan((yMe-yPro)/(xPro-xMe+0.001))*180/3.14
			if xSpeed==0:
				angleTir = math.copysign(90,ySpeed)
				angleMe = (-math.copysign(1,xPro-xMe)*90+90)+arctan((yMe-yPro)/(xPro-xMe+0.001))*180/3.14
			else:
				angleTir = math.copysign(90,ySpeed)+90
				angleMe = (-math.copysign(1,xPro-xMe)*90+90)+arctan((yMe-yPro)/(xPro-xMe+0.001))*180/3.14
			if angleTir<0:
				angleTir+=360
			if angleMe<0:
				angleMe+=360
			rayon_carré = (xMe - xPro)*(xMe - xPro)+(yMe - yPro)*(yMe - yPro)
			if math.sqrt(rayon_carré) <72000 and abs(angleTir-angleMe) > 160 and abs(angleTir-angleMe) < 200:
				return True
			else:
				return False
		else:
			return False
		
	def esquive():
		for i in range(len(gamestate.projectiles)):
			if inDanger(gamestate.projectiles[i]):
				return i
		return -1
	
	if my_data.get('first') is None:
		if xMe < -20.0:
			my_data['first'] = 1
		else:
			my_data['first'] = 0
	meCenter = zone(xMe, yMe) == "Center"
	himCenter = zone(xHim, yHim) == "Center"
	 
	if healthHim < 0:
		if scoreHim + 10 < scoreMe and not zone(xMe,yMe) == "CornerLeftAlmostUp" and not zone(xMe,yMe) == "CornerDownAlmostRight":
			if zone(xMe,yMe) == "CornerUpRight":
				if my_data.get('first') == 1:
					return ShootCommand(-90)
				else:
					return ShootCommand(180)
			else:
				return moveTo(250,250)
		elif zone(xMe,yMe) == "CornerUpLeft":
			return moveTo(-250,0)
		elif zone(xMe,yMe) == "CornerDownRight":
			return moveTo(250,0)
		return moveTo(0,0)
	elif meCenter and not himCenter:
		return ShootCommand((-math.copysign(1,xHim-xMe)*90+90)+arctan((yHim-yMe)/(xHim-xMe))*180/3.14)
	elif not meCenter and not himCenter:
		if (zone(xMe,yMe) == "CornerUpLeft" and (zone(xHim,yHim) == "CornerUpRight" or zone(xHim,yHim) == "CornerUp")):
			return moveTo(-250,0)
		elif (zone(xMe,yMe) == "CornerUpLeft" and (zone(xHim,yHim) == "CornerUpAlmostLeft" or zone(xHim,yHim) == "CornerDownLeft")):
			return moveTo(0,250)
		elif (zone(xMe,yMe) == "CornerDownRight" and (zone(xHim,yHim) == "CornerUpRight" or zone(xHim,yHim) == "CornerRightAlmostDown")):
			return moveTo(0,-250)
		elif (zone(xMe,yMe) == "CornerDownRight" and (zone(xHim,yHim) == "CornerDownLeft" or zone(xHim,yHim) == "CornerUpAlmostLeft")):
			return moveTo(250,0)
		else:
			if zone(xMe,yMe) == "CornerUpLeft" or zone(xMe,yMe) == "CornerLeftAlmostUp":
				return moveTo(-250,0)
			if zone(xMe,yMe) == "CornerDownRight" or zone(xMe,yMe) == "CornerRightAlmostDown":
				return moveTo(250,0)
			return moveTo(0,0)
	elif not meCenter and himCenter:
		if zone(xMe,yMe) == "CornerUpLeft" or zone(xMe,yMe) == "CornerLeftAlmostUp":
			return moveTo(-250,0)
		if zone(xMe,yMe) == "CornerDownRight" or zone(xMe,yMe) == "CornerRightAlmostDown":
			return moveTo(250,0)
		return moveTo(0,0)
	elif meCenter and himCenter:
		return ShootCommand((-math.copysign(1,xHim-xMe)*90+90)+arctan((yHim-yMe)/(xHim-xMe))*180/3.14)
	"""
	def increase_right(data):
		if data.get('right') is None:
			data['right'] = 1
		else:
			data['right'] += 1
	"""
	return ShootCommand(45.0)

