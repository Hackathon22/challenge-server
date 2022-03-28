from game_simulation import GameSimulation, SnapshotData
from agent import MoveCommand, ShootCommand, Command
import typing
import numpy as np
import random

def my_data():
    return {}


def my_ai(gamestate: SnapshotData, my_data: typing.Dict) -> Command:
    ''' ---------- INITIALISATION ---------- '''
    if my_data.get('Walls') is None:
        ''' WALLS '''
        halfLength = 182 / 2
        halfWidth = 36 / 2
        # Borders
        Wall_TOP = [-275, 275, 275, 275]  # (x1, y1, x2, y2)
        Wall_BOTTOM = [-275, -275, 275, -275]
        Wall_LEFT = [-275, 275, -275, -275]
        Wall_RIGHT = [275, 275, 275, -275]
        Wall_TOP = [-275, 275, 275, 275]
        my_data['Wall_EXT'] = [Wall_TOP, Wall_BOTTOM, Wall_LEFT, Wall_RIGHT]
        # Wall1 (Block1)
        Wall1_T = [150 - halfWidth, 150 + halfWidth, 150 + halfWidth, 150 + halfWidth]
        Wall1_B = [150 - halfWidth, 150 - halfWidth, 150 + halfWidth, 150 - halfWidth]
        Wall1_L = [150 - halfWidth, 150 + halfWidth, 150 - halfWidth, 150 - halfWidth]
        Wall1_R = [150 + halfWidth, 150 + halfWidth, 150 + halfWidth, 150 - halfWidth]
        # Wall2 (Block2)
        Wall2_T = [-150 - halfWidth, -150 + halfWidth, -150 + halfWidth, -150 + halfWidth]
        Wall2_B = [-150 - halfWidth, -150 - halfWidth, -150 + halfWidth, -150 - halfWidth]
        Wall2_L = [-150 - halfWidth, -150 + halfWidth, -150 - halfWidth, -150 - halfWidth]
        Wall2_R = [-150 + halfWidth, -150 + halfWidth, -150 + halfWidth, -150 - halfWidth]
        # Wall3
        Wall3_T = [-125 - halfLength, 200 + halfWidth, -125 + halfLength, 200 + halfWidth]
        Wall3_B = [-125 - halfLength, 200 - halfWidth, -125 + halfLength, 200 - halfWidth]
        Wall3_L = [-125 - halfLength, 200 + halfWidth, -125 - halfLength, 200 - halfWidth]
        Wall3_R = [-125 + halfLength, 200 + halfWidth, -125 + halfLength, 200 - halfWidth]
        # Wall4
        Wall4_T = [-200 - halfWidth, 125 + halfLength, -200 + halfWidth, 125 + halfLength]
        Wall4_B = [-200 - halfWidth, 125 - halfLength, -200 + halfWidth, 125 - halfLength]
        Wall4_L = [-200 - halfWidth, 125 + halfLength, -200 - halfWidth, 125 - halfLength]
        Wall4_R = [-200 + halfWidth, 125 + halfLength, -200 + halfWidth, 125 - halfLength]
        # Wall5
        Wall5_T = [200 - halfWidth, -125 + halfLength, 200 + halfWidth, -125 + halfLength]
        Wall5_B = [200 - halfWidth, -125 - halfLength, 200 + halfWidth, -125 - halfLength]
        Wall5_L = [200 - halfWidth, -125 + halfLength, 200 - halfWidth, -125 - halfLength]
        Wall5_R = [200 + halfWidth, -125 + halfLength, 200 + halfWidth, -125 - halfLength]
        # Wall6
        Wall6_T = [125 - halfLength, -200 + halfWidth, 125 + halfLength, -200 + halfWidth]
        Wall6_B = [125 - halfLength, -200 - halfWidth, 125 + halfLength, -200 - halfWidth]
        Wall6_L = [125 - halfLength, -200 + halfWidth, 125 - halfLength, -200 - halfWidth]
        Wall6_R = [125 + halfLength, -200 + halfWidth, 125 + halfLength, -200 - halfWidth]
        my_data['Walls'] = [Wall1_T, Wall1_B, Wall1_L, Wall1_R, Wall2_T, Wall2_B, Wall2_L, Wall2_R, Wall3_T, Wall3_B,
                            Wall3_L, Wall3_R, Wall4_T, Wall4_B, Wall4_L, Wall4_R, Wall5_T, Wall5_B, Wall5_L, Wall5_R,
                            Wall6_T, Wall6_B, Wall6_L, Wall6_R] + my_data.get('Wall_EXT')
        print(len(my_data['Walls']))
    ''' --- Get the positions --- '''
    opponentPosition = gamestate.other_players[0].position
    myPosition = gamestate.controlled_player.position
    my_data['Inverse_prediction'] = 1

    if myPosition[1] < -500:
        my_data['spawned'] = None

    if my_data.get('spawned') is None:
        my_data['spawned'] = True

        unit_vector = opponentPosition / np.linalg.norm(opponentPosition)
        dot_product = unit_vector[0]
        angle_opponent = (np.sign(opponentPosition[1]) + 1) * 90 + np.arccos(dot_product) * 180 / np.pi

        unit_vector = myPosition / np.linalg.norm(myPosition)
        dot_product = unit_vector[0]
        angle_player = (np.sign(myPosition[1]) + 1) * 90 + np.arccos(dot_product) * 180 / np.pi

        angle_between = angle_opponent - angle_player

        if angle_between // 180 == 0:
            my_data['direction_init'] = "vert"
        else:
            my_data['direction_init'] = "horiz"

    if my_data.get('counter') is None:
        my_data['counter'] = 1
        my_data['dodge'] = 0
        desiredPos = (0, 0)
        ''' SAVE THE LAST POSITION OF THE OPPONENT '''
        my_data['Opponent_LastPos'] = gamestate.other_players[0].position
        my_data['Opponent_Esquive'] = [gamestate.other_players[0].position]

        # Check which player I am
        if (gamestate.controlled_player.position[0] < 0):
            my_data["playerID"] = 1
        else:
            my_data["playerID"] = 2

    if len(my_data.get('Opponent_Esquive')) > 4:
        my_data['Opponent_Esquive'] = my_data.get('Opponent_Esquive')[1:4]

    if len(my_data.get('Opponent_Esquive')) == 4:
        """ --- Detect if the opponent is dodging --- """
        pos1 = my_data.get('Opponent_Esquive')[0]
        pos2 = my_data.get('Opponent_Esquive')[1]
        pos3 = my_data.get('Opponent_Esquive')[2]
        pos4 = my_data.get('Opponent_Esquive')[3]

        tresholhd = 7
        moving = True
        if ((pos3[0] - pos4[0]) ** 2 + (pos3[1] - pos4[1]) ** 2) ** 0.5 < 1:
            moving = False

        d_pos_1_3 = ((pos1[0] - pos3[0]) ** 2 + (pos1[1] - pos3[1]) ** 2) ** 0.5
        d_pos_2_4 = ((pos2[0] - pos4[0]) ** 2 + (pos2[1] - pos4[1]) ** 2) ** 0.5

        if d_pos_1_3 < tresholhd and d_pos_2_4 < tresholhd and moving:
            """ --- Raise a flag if the opponent is dodging --- """
            my_data['Inverse_prediction'] = -0.33

    ''' ---------- TO DO EVERYTIME ! ---------- '''

    ''' --- Update the desired position if necessary --- '''
    ''' If I am player 1 (starting at top left) '''
    if (my_data["playerID"] == 1):
        if ((opponentPosition[0] > 0 and opponentPosition[1] < -125) or (
                opponentPosition[0] > 125 and opponentPosition[1] < 0)):  # opponent in spawn zone
            desiredPos = (10, -10)
        else:
            desiredPos = (10, -10)
    ''' If I am player 2 (starting at bottom right) '''
    if (my_data["playerID"] == 2):
        if ((opponentPosition[0] < 0 and opponentPosition[1] > 125) or (
                opponentPosition[0] < -125 and opponentPosition[1] > 0)):  # opponent in spawn zone
            desiredPos = (-10, 10)
        else:
            desiredPos = (-10, 10)

    ''' ---------- SHOOTING AFTER DODGING ---------- '''
    if my_data['dodge'] == 1:
        my_data['dodge'] = 0

        shoot = True
        shooting_line = [myPosition, opponentPosition]
        # Determine if you can shoot
        for w in my_data.get('Walls'):
            x1 = shooting_line[0][0]
            y1 = shooting_line[0][1]
            x2 = shooting_line[1][0]
            y2 = shooting_line[1][1]
            x3 = w[0]
            y3 = w[1]
            x4 = w[2]
            y4 = w[3]

            denom = ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4))
            if denom != 0:
                u = ((x1 - x3) * (y1 - y2) - (y1 - y3) * (x1 - x2)) / denom
                t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom
                if u >= 0 - 0.05 and u <= 1 + 0.05 and t >= 0 - 0.05 and t <= 1 + 0.05:
                    shoot = False

        if gamestate.other_players[0].position[1] < -500:
            shoot = False

        if shoot:
            ''' --- SHOOT ON THE OPPONENT --- '''
            # Predict the position when the rocket arrives

            distanceWithOpponent = ((opponentPosition[0] - myPosition[0]) ** 2 + (
                        opponentPosition[1] - myPosition[1]) ** 2) ** 0.5

            predictedPosition = opponentPosition
            if (opponentPosition != my_data['Opponent_LastPos']):
                directionOfOpponent = [opponentPosition[0] - my_data['Opponent_LastPos'][0],
                                       opponentPosition[1] - my_data['Opponent_LastPos'][1]]
                predictedPosition = [predictedPosition[0] + my_data.get('Inverse_prediction') * directionOfOpponent[
                    0] * distanceWithOpponent / 120,
                                     predictedPosition[1] + my_data.get('Inverse_prediction') * directionOfOpponent[
                                         1] * distanceWithOpponent / 120]

            directionToShoot = (predictedPosition[0] - myPosition[0], predictedPosition[1] - myPosition[1])
            angle = 0;
            if (np.linalg.norm(directionToShoot) != 0 and gamestate.other_players[0].position[1] > -500):
                unit_vector = directionToShoot / np.linalg.norm(directionToShoot)
                dot_product = unit_vector[0]
                angle = np.sign(directionToShoot[1]) * np.arccos(dot_product) * 180 / np.pi

            my_data['Opponent_LastPos'] = gamestate.other_players[0].position
            my_data['Opponent_Esquive'].append(gamestate.other_players[0].position)
            return ShootCommand(angle)

    ''' ---------- CHECK THE PROJECTILES : DODGE ? ---------- '''
    if len(gamestate.projectiles) > 0:
        for i in range(len(gamestate.projectiles)):
            # Esquive si le joeur est dans la zone d'explosion
            x_player = gamestate.controlled_player.position[0]
            y_player = gamestate.controlled_player.position[1]
            x_missile = gamestate.projectiles[i].position[0]
            y_missile = gamestate.projectiles[i].position[1]
            x_ennemy = gamestate.other_players[0].position[0]
            y_ennemy = gamestate.other_players[0].position[1]

            d_player_missile = ((x_player - x_missile) ** 2 + (y_player - y_missile) ** 2) ** 0.5
            d_ennemy_missile = ((x_ennemy - x_missile) ** 2 + (y_ennemy - y_missile) ** 2) ** 0.5
            d_player_ennemy = ((x_ennemy - x_player) ** 2 + (y_ennemy - y_player) ** 2) ** 0.5

            if d_player_ennemy > d_ennemy_missile:
                x_speed_missile = gamestate.projectiles[i].speed[0]
                y_speed_missile = gamestate.projectiles[i].speed[1]
                radius_player = 75
                if d_player_missile < 2.3 * radius_player:
                    along = 100
                    l_missile = [[x_missile, y_missile],
                                 [x_missile + along * x_speed_missile, y_missile + along * y_speed_missile]]
                    d_missile = (x_speed_missile ** 2 + y_speed_missile ** 2) ** 0.5
                    normal_missile = [y_speed_missile / d_missile, -x_speed_missile / d_missile]
                    l_1_player = [[x_player, y_player], [x_player + radius_player * normal_missile[0],
                                                         y_player + radius_player * normal_missile[1]]]
                    l_2_player = [[x_player, y_player], [x_player - radius_player * normal_missile[0],
                                                         y_player - radius_player * normal_missile[1]]]

                    missile_trajectory = [[x_missile, y_missile], [x_player, y_player]]
                    missileToPlayer = (x_player - x_missile, y_player - y_missile);

                    missileToPlayer_UNIT = missileToPlayer / np.linalg.norm(missileToPlayer)
                    missileSpeed_UNIT = gamestate.projectiles[i].speed / np.linalg.norm(gamestate.projectiles[i].speed)
                    dot_product = missileToPlayer_UNIT[0] * missileSpeed_UNIT[0] + missileToPlayer_UNIT[1] * \
                                  missileSpeed_UNIT[1]
                    angle = np.arccos(dot_product) * 180.0 / np.pi
                    if (angle < 10.0):
                        distanceToCenter = (myPosition[0] ** 2 + myPosition[1] ** 2) ** 0.5
                        my_data['dodge'] = 1
                        my_data['Opponent_LastPos'] = gamestate.other_players[0].position
                        return MoveCommand((-normal_missile[0] - 0.6 * myPosition[0] / distanceToCenter,
                                            -normal_missile[1] - 0.6 * myPosition[1] / distanceToCenter, 0.0))

                    dodge = True
                    # Determine if you can shoot
                    for w in my_data.get('Walls'):
                        x1 = missile_trajectory[0][0]
                        y1 = missile_trajectory[0][1]
                        x2 = missile_trajectory[1][0]
                        y2 = missile_trajectory[1][1]
                        x3 = w[0]
                        y3 = w[1]
                        x4 = w[2]
                        y4 = w[3]

                        denom = ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4))
                        if denom != 0:
                            u = ((x1 - x3) * (y1 - y2) - (y1 - y3) * (x1 - x2)) / denom
                            t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom
                            if u >= 0 and u <= 1 and t >= 0 and t <= 1:
                                dodge = False
                    if dodge:
                        x1 = l_1_player[0][0]
                        y1 = l_1_player[0][1]
                        x2 = l_1_player[1][0]
                        y2 = l_1_player[1][1]
                        x3 = l_missile[0][0]
                        y3 = l_missile[0][1]
                        x4 = l_missile[1][0]
                        y4 = l_missile[1][1]

                        u = ((x1 - x3) * (y1 - y2) - (y1 - y3) * (x1 - x2)) / (
                                    (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4))
                        t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / (
                                    (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4))
                        if u >= 0 and u <= 1 and t >= 0 and t <= 1:
                            distanceToCenter = (myPosition[0] ** 2 + myPosition[1] ** 2) ** 0.5
                            my_data['dodge'] = 1
                            my_data['Opponent_LastPos'] = gamestate.other_players[0].position
                            my_data['Opponent_Esquive'].append(gamestate.other_players[0].position)
                            return MoveCommand((-normal_missile[0] - 0.6 * myPosition[0] / distanceToCenter,
                                                -normal_missile[1] - 0.6 * myPosition[1] / distanceToCenter, 0.0))

                        x1 = l_2_player[0][0]
                        y1 = l_2_player[0][1]
                        x2 = l_2_player[1][0]
                        y2 = l_2_player[1][1]

                        u = ((x1 - x3) * (y1 - y2) - (y1 - y3) * (x1 - x2)) / (
                                    (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4))
                        t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / (
                                    (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4))
                        if u >= 0 and u <= 1 and t >= 0 and t <= 1:
                            distanceToCenter = (myPosition[0] ** 2 + myPosition[1] ** 2) ** 0.5
                            my_data['dodge'] = 1
                            my_data['Opponent_LastPos'] = gamestate.other_players[0].position
                            my_data['Opponent_Esquive'].append(gamestate.other_players[0].position)
                            return MoveCommand((normal_missile[0] - 0.6 * myPosition[0] / distanceToCenter,
                                                normal_missile[1] - 0.6 * myPosition[1] / distanceToCenter, 0.0))

    shoot = True
    shooting_line = [myPosition, opponentPosition]
    # Determine if you can shoot
    for w in my_data.get('Walls'):
        x1 = shooting_line[0][0]
        y1 = shooting_line[0][1]
        x2 = shooting_line[1][0]
        y2 = shooting_line[1][1]
        x3 = w[0]
        y3 = w[1]
        x4 = w[2]
        y4 = w[3]

        denom = ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4))
        if denom != 0:
            u = ((x1 - x3) * (y1 - y2) - (y1 - y3) * (x1 - x2)) / denom
            t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom
            if u >= 0 - 0.05 and u <= 1 + 0.05 and t >= 0 - 0.05 and t <= 1 + 0.05:
                shoot = False

    if gamestate.other_players[0].position[1] < -500:
        shoot = False

    if shoot:
        ''' --- SHOOT ON THE OPPONENT --- '''
        # Predict the position when the rocket arrives

        distanceWithOpponent = ((opponentPosition[0] - myPosition[0]) ** 2 + (
                    opponentPosition[1] - myPosition[1]) ** 2) ** 0.5

        predictedPosition = opponentPosition
        if (opponentPosition != my_data['Opponent_LastPos']):
            directionOfOpponent = [opponentPosition[0] - my_data['Opponent_LastPos'][0],
                                   opponentPosition[1] - my_data['Opponent_LastPos'][1]]
            predictedPosition = [predictedPosition[0] + my_data.get('Inverse_prediction') * directionOfOpponent[
                0] * distanceWithOpponent / 120,
                                 predictedPosition[1] + my_data.get('Inverse_prediction') * directionOfOpponent[
                                     1] * distanceWithOpponent / 120]

        directionToShoot = (predictedPosition[0] - myPosition[0], predictedPosition[1] - myPosition[1])
        angle = 0;
        if (np.linalg.norm(directionToShoot) != 0 and gamestate.other_players[0].position[1] > -500):
            unit_vector = directionToShoot / np.linalg.norm(directionToShoot)
            dot_product = unit_vector[0]
            angle = np.sign(directionToShoot[1]) * np.arccos(dot_product) * 180 / np.pi

        my_data['Opponent_LastPos'] = gamestate.other_players[0].position
        my_data['Opponent_Esquive'].append(gamestate.other_players[0].position)
        return ShootCommand(angle)

    ''' ---------- IF WE DON'T NEED TO DODGE : GO TO DESIRED POINT (if already : shoot) ---------- '''
    if my_data.get('direction_init') == "horiz":
        if gamestate.controlled_player.position[0] > desiredPos[0] + 20 or gamestate.controlled_player.position[0] < \
                desiredPos[0] - 20:
            my_data['Opponent_LastPos'] = gamestate.other_players[0].position
            my_data['Opponent_Esquive'].append(gamestate.other_players[0].position)
            return MoveCommand((-gamestate.controlled_player.position[0], 0.0, 0.0))
        if gamestate.controlled_player.position[1] < desiredPos[1] - 20 or gamestate.controlled_player.position[1] > \
                desiredPos[1] + 20:
            my_data['Opponent_LastPos'] = gamestate.other_players[0].position
            my_data['Opponent_Esquive'].append(gamestate.other_players[0].position)
            return MoveCommand((0.0, -gamestate.controlled_player.position[1], 0.0))
        else:  # If already at the desired point : SHOOT
            # Predict the position when the rocket arrives
            opponentPosition = gamestate.other_players[0].position
            myPosition = gamestate.controlled_player.position
            distanceWithOpponent = ((opponentPosition[0] - myPosition[0]) ** 2 + (
                        opponentPosition[1] - myPosition[1]) ** 2) ** 0.5
            predictedPosition = opponentPosition
            if (opponentPosition != my_data['Opponent_LastPos']):
                directionOfOpponent = [opponentPosition[0] - my_data['Opponent_LastPos'][0],
                                       opponentPosition[1] - my_data['Opponent_LastPos'][1]]
                predictedPosition = [predictedPosition[0] + my_data.get('Inverse_prediction') * directionOfOpponent[
                    0] * distanceWithOpponent / 120,
                                     predictedPosition[1] + my_data.get('Inverse_prediction') * directionOfOpponent[
                                         1] * distanceWithOpponent / 120]

            directionToShoot = (predictedPosition[0] - myPosition[0], predictedPosition[1] - myPosition[1])
            angle = 0;
            if (np.linalg.norm(directionToShoot) != 0 and gamestate.other_players[0].position[1] > -500):
                unit_vector = directionToShoot / np.linalg.norm(directionToShoot)
                dot_product = unit_vector[0]
                angle = np.sign(directionToShoot[1]) * np.arccos(dot_product) * 180 / np.pi

            my_data['Opponent_LastPos'] = gamestate.other_players[0].position
            my_data['Opponent_Esquive'].append(gamestate.other_players[0].position)
            return ShootCommand(angle)

    if my_data.get('direction_init') == "vert":
        if gamestate.controlled_player.position[1] < desiredPos[1] - 20 or gamestate.controlled_player.position[1] > \
                desiredPos[1] + 20:
            my_data['Opponent_LastPos'] = gamestate.other_players[0].position
            my_data['Opponent_Esquive'].append(gamestate.other_players[0].position)
            return MoveCommand((0.0, -gamestate.controlled_player.position[1], 0.0))
        if gamestate.controlled_player.position[0] > desiredPos[0] + 20 or gamestate.controlled_player.position[0] < \
                desiredPos[0] - 20:
            my_data['Opponent_LastPos'] = gamestate.other_players[0].position
            my_data['Opponent_Esquive'].append(gamestate.other_players[0].position)
            return MoveCommand((-gamestate.controlled_player.position[0], 0.0, 0.0))
        else:  # If already at the desired point : SHOOT
            # Predict the position when the rocket arrives
            opponentPosition = gamestate.other_players[0].position
            myPosition = gamestate.controlled_player.position
            distanceWithOpponent = ((opponentPosition[0] - myPosition[0]) ** 2 + (
                        opponentPosition[1] - myPosition[1]) ** 2) ** 0.5
            predictedPosition = opponentPosition
            if (opponentPosition != my_data['Opponent_LastPos']):
                directionOfOpponent = [opponentPosition[0] - my_data['Opponent_LastPos'][0],
                                       opponentPosition[1] - my_data['Opponent_LastPos'][1]]
                predictedPosition = [predictedPosition[0] + my_data.get('Inverse_prediction') * directionOfOpponent[
                    0] * distanceWithOpponent / 120,
                                     predictedPosition[1] + my_data.get('Inverse_prediction') * directionOfOpponent[
                                         1] * distanceWithOpponent / 120]

            directionToShoot = (predictedPosition[0] - myPosition[0], predictedPosition[1] - myPosition[1])
            angle = 0;
            if (np.linalg.norm(directionToShoot) != 0 and gamestate.other_players[0].position[1] > -500):
                unit_vector = directionToShoot / np.linalg.norm(directionToShoot)
                dot_product = unit_vector[0]
                angle = np.sign(directionToShoot[1]) * np.arccos(dot_product) * 180 / np.pi

            my_data['Opponent_LastPos'] = gamestate.other_players[0].position
            my_data['Opponent_Esquive'].append(gamestate.other_players[0].position)
            return ShootCommand(angle)