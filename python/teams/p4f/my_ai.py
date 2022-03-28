from game_simulation import GameSimulation, SnapshotData
from agent import MoveCommand, ShootCommand, Command
import typing
import math

def my_data():
    return {}

def my_ai(gamestate: SnapshotData, my_data: typing.Dict) -> Command:
    
    if (gamestate.controlled_player.position[0] == gamestate.other_players[0].position[0]):
       angle = 270
       if (gamestate.controlled_player.position[1] < gamestate.other_players[0].position[1]):
          angle = 90
       return ShootCommand(angle)
    
    if (gamestate.controlled_player.position[1] == gamestate.other_players[0].position[1]):
        angle = 0
        if  gamestate.controlled_player.position[1] > gamestate.other_players[0].position[1]:
          angle =   180    
        return ShootCommand(angle)
     
    
    xsq = (gamestate.controlled_player.position[0]-gamestate.other_players[0].position[0])**2
    ysq = (gamestate.controlled_player.position[1]-gamestate.other_players[0].position[1])**2
    reuclidean =  math.sqrt(xsq+ysq)
    if (reuclidean < 80):
     angle = (((math.atan((gamestate.other_players[0].position[1]-gamestate.controlled_player.position[1])/(gamestate.other_players[0].position[0]-gamestate.controlled_player.position[0]))*180.0 / math.pi)))    
     if  angle  < 0:
       angle = 180-angle
     return ShootCommand(angle)
      
    

    if (gamestate.controlled_player.position[0]) > -20 and (gamestate.controlled_player.position[1] < 15):   
     if gamestate.controlled_player.position[0] > 0:
      return MoveCommand((-1.0, 0.0, 0.0))
     elif gamestate.controlled_player.position[1] < -20:
        return MoveCommand((0.0, 1.0, 0.0))
     else: 
      angle = (((math.atan((gamestate.other_players[0].position[1]-gamestate.controlled_player.position[1])/(gamestate.other_players[0].position[0]-gamestate.controlled_player.position[0]))*180.0 / math.pi)))    
      if  angle  < 0:
       angle = 180-angle
      return ShootCommand(180-angle) 
    else:
     if (gamestate.controlled_player.position[0]) < 20 and (gamestate.controlled_player.position[1] > -15):
       if gamestate.controlled_player.position[0] < 0:
        return MoveCommand((1.0, 0.0, 0.0))
       elif gamestate.controlled_player.position[1] > 0:
        return MoveCommand((0.0, -1.0, 0.0)) 
       else:
        angle = (((math.atan((gamestate.other_players[0].position[1]-gamestate.controlled_player.position[1])/(gamestate.other_players[0].position[0]-gamestate.controlled_player.position[0]))*180.0 / math.pi)))    
        if  angle  < 0:
          angle = 180-angle
        return ShootCommand(angle)
     else:
        angle = (((math.atan((gamestate.other_players[0].position[1]-gamestate.controlled_player.position[1])/(gamestate.other_players[0].position[0]-gamestate.controlled_player.position[0]))*180.0 / math.pi)))    
        if  angle  < 0:
          angle = 180-angle
        return ShootCommand(angle)