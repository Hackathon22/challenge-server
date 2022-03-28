from game_simulation import GameSimulation, SnapshotData
from agent import MoveCommand, ShootCommand, Command
import typing
import random
import numpy as np
import math

def my_data() -> typing.Dict:
    return {}

def my_ai(gamestate: SnapshotData, my_data: typing.Dict) -> Command:

    
    def compute_shoot_angle(myPosition, enemyPosition):
        angle = math.degrees(math.atan2((enemyPosition[1] + 275) - (myPosition[1] + 275), (enemyPosition[0] + 275) - (myPosition[0] + 275)))
        if (angle < 0):
            angle += 360
        return angle
    myPosition = gamestate.controlled_player.position
    enemyPosition = gamestate.other_players[0].position
    
    if my_data.get('counter') is None:
        my_data['counter'] = 1
    else:
        my_data['counter'] += 1

    if (myPosition[0] < 0):
        if (myPosition[1] > 24):
            return MoveCommand((0.0, -1.0, 0.0))
        elif (gamestate.other_players[0].health > 0):
            return ShootCommand(compute_shoot_angle(myPosition, enemyPosition))
        elif (myPosition[0] < -100):
            return MoveCommand((1.0, 0.0, 0.0))
        else:
            return ShootCommand(compute_shoot_angle(myPosition, enemyPosition))
    else:
        if (myPosition[1] < -24):
            return MoveCommand((0.0, 1.0, 0.0))
        elif (gamestate.other_players[0].health > 0):
            return ShootCommand(compute_shoot_angle(myPosition, enemyPosition))
        elif (gamestate.other_players[0].health <= 0):
            if (myPosition[0] > 100):
                return MoveCommand((-1.0, 0.0, 0.0))
            else:   
                return MoveCommand((0.0, 0.0, 0.0))
        else:
            return ShootCommand(compute_shoot_angle(myPosition, enemyPosition))