import math as m
from game_simulation import GameSimulation, SnapshotData
from agent import InvalidCommand, MoveCommand, ShootCommand, Command
import typing

def my_data():
    return {}

def my_ai(gamestate: SnapshotData, my_data: typing.Dict) -> Command:
    def rad_to_degr(angle):
        degr = (180 / m.pi) * angle
        return degr


    def ennemy_dirr(gamestate: SnapshotData, my_data: typing.Dict):
        dy = gamestate.other_players[0].speed[1]
        dx = gamestate.other_players[0].speed[0]
        if dx != 0:
            direction_ennemy = m.atan(dy / dx)
        else:
            if dy > 0:
                direction_ennemy = m.pi / 2
            else:
                direction_ennemy = - m.pi / 2
        return direction_ennemy


    def ennemy_distance(gamestate: SnapshotData, my_data: typing.Dict):
        x_ennemi = gamestate.other_players[0].position[0]
        x_nous = gamestate.controlled_player.position[0]
        y_ennemi = gamestate.other_players[0].position[1]
        y_nous = gamestate.controlled_player.position[1]
        return m.sqrt((x_nous - x_ennemi) * (x_nous - x_ennemi) + (y_nous - y_ennemi) * (y_nous - y_ennemi))


    def angle_between_players(gamestate: SnapshotData, my_data: typing.Dict):
        x_ennemi = gamestate.other_players[0].position[0]
        x_nous = gamestate.controlled_player.position[0]
        y_ennemi = gamestate.other_players[0].position[1]
        y_nous = gamestate.controlled_player.position[1]
        deltay = y_ennemi - y_nous
        deltax = x_ennemi - x_nous

        if gamestate.other_players[0].speed[1] != 0 and gamestate.other_players[0].speed[1] != 0:
            corr_factor = 0.24
        else:
            corr_factor = 0

        if deltax + corr_factor * ennemy_distance(gamestate, my_data) * m.cos(
                ennemy_dirr(gamestate, my_data)) != 0:  # abscisse différente
            if x_ennemi > x_nous:
                total_angle = m.atan(
                    (deltay + corr_factor * ennemy_distance(gamestate, my_data) * m.sin(
                        ennemy_dirr(gamestate, my_data))) / (
                            deltax + corr_factor * ennemy_distance(gamestate, my_data) * m.cos(
                        ennemy_dirr(gamestate, my_data))))
            else:
                total_angle = m.atan(
                    (deltay + corr_factor * ennemy_distance(gamestate, my_data) * m.sin(
                        ennemy_dirr(gamestate, my_data))) / (
                            deltax + corr_factor * ennemy_distance(gamestate, my_data) * m.cos(
                        ennemy_dirr(gamestate, my_data)))) + m.pi
        else:
            if deltay + corr_factor * ennemy_distance(gamestate, my_data) * m.sin(ennemy_dirr(gamestate, my_data)) > 0:
                total_angle = m.pi / 2
            elif deltay + corr_factor * ennemy_distance(gamestate, my_data) * m.sin(ennemy_dirr(gamestate, my_data)) < 0:
                total_angle = - m.pi / 2
            else:
                total_angle = 0
        degr = rad_to_degr(total_angle)
        return degr


    def us_in_zone(gamestate: SnapshotData, my_data: typing.Dict):
        if gamestate.controlled_player.position[0] > -100 and gamestate.controlled_player.position[0] < 100 and \
                gamestate.controlled_player.position[1] > -100 and gamestate.controlled_player.position[1] < 100:
            return True
        else:
            return False


    def ennemy_in_zone(gamestate: SnapshotData, my_data: typing.Dict):
        if gamestate.other_players[0].position[0] > -100 and gamestate.other_players[0].position[0] < 100 and \
                gamestate.other_players[0].position[1] > -100 and gamestate.other_players[0].position[1] < 100:
            return True
        else:
            return False


    def min(a, b):
        if a <= b:
            return a
        else:
            return b


    def dist_hill(gamestate: SnapshotData, my_data: typing.Dict):
        if us_in_zone(gamestate, my_data) is False:
            dist_nous_x = min(abs(gamestate.controlled_player.position[0] - 100),
                              abs(gamestate.controlled_player.position[0] + 100))
            dist_nous_y = min(abs(gamestate.controlled_player.position[1] - 100),
                              abs(gamestate.controlled_player.position[1] + 100))
            dist_nous = min(dist_nous_y, dist_nous_x)
        else:
            dist_nous = 0
        if ennemy_in_zone(gamestate, my_data) is False:
            diste_e_x = min(abs(gamestate.other_players[0].position[0] - 100),
                            abs(gamestate.other_players[0].position[0] + 100))
            diste_e_y = min(abs(gamestate.other_players[0].position[1] - 100),
                            abs(gamestate.other_players[0].position[1] + 100))
            dist_ennemy = min(diste_e_x, diste_e_y)
        else:
            dist_ennemy = 0
        return [dist_nous, dist_ennemy]


    '''idée de mon bot: rush vers le ilieux de la map puis voir si on est le premier vers la zone ou pas .
    Si premier, rush zone.sinon on tue l'ennemi.'''
    distance_hill = dist_hill(gamestate, my_data)

    if gamestate.controlled_player.position[1] > 20:
        return MoveCommand((0.0, -1.0, 0.0))
    elif gamestate.controlled_player.position[1] < -20:
        return MoveCommand((0.0, 1.0, 0.0))
    elif distance_hill[0] < distance_hill[1]:
        if gamestate.controlled_player.position[0] < -20:
            return MoveCommand((1.0, 0.0, 0.0))
        elif gamestate.controlled_player.position[0] > 20:
            return MoveCommand((-1.0, 0.0, 0.0))
        else:
            if gamestate.other_players[0].health >= 0:
                return ShootCommand(angle_between_players(gamestate, my_data))
            else:
                return MoveCommand((0.0, 0.0, 0.0))
    elif distance_hill[0] > distance_hill[1]:
        if gamestate.other_players[0].health >= 0:
            return ShootCommand(angle_between_players(gamestate, my_data))
    else:
        if gamestate.controlled_player.position[0] < 0:
            return MoveCommand((1.0, 0.0, 0.0))
        else:
            return MoveCommand((0.0, 0.0, 0.0))