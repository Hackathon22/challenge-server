import math
from random import random
from game_simulation import GameSimulation, SnapshotData
from agent import InvalidCommand, MoveCommand, ShootCommand, Command
import typing

def my_data():
	return {}

def my_ai(gamestate: SnapshotData, my_data: typing.Dict) -> Command:
    def normalize(vector):
        if vector[0] == vector[1] == 0:
            return 0, 0
        return vector[0] / length((vector[0], vector[1])), vector[1] / length((vector[0], vector[1]))

    def length(vector):
        if vector[0] == vector[1] == 0:
            return 0
        return distance(vector[0], vector[1], 0, 0)

    def projectile_going_away(pos_x, pos_y, proj_x, proj_y, proj_dx, proj_dy):
        normalised_dx = proj_dx / ((proj_dx ** 2 + proj_dy ** 2) ** 0.5)
        normalised_dy = proj_dy / ((proj_dx ** 2 + proj_dy ** 2) ** 0.5)
        return distance(pos_x, pos_y, proj_x, proj_y) < distance(pos_x, pos_y, proj_x + normalised_dx * 0.1, proj_y
                                                                 + normalised_dy * 0.1)

    def get_closest(projectiles, pos_x, pos_y):
        if len(projectiles) == 0:
            return None
        if len(projectiles) == 1:
            return projectiles[0]
        buffer = projectiles[0]
        for i in range(len(projectiles)):
            if distance(pos_x, pos_y, projectiles[i].position[0], projectiles[i].position[1]) < \
                    distance(pos_x, pos_y, buffer.position[0], buffer.position[1]) and not \
                    projectile_going_away(pos_x, pos_y, projectiles[i].position[0], projectiles[i].position[1],
                                          projectiles[i].speed[0], projectiles[i].speed[1]):
                buffer = projectiles[i]

        if not projectile_going_away(pos_x, pos_y, buffer.position[0], buffer.position[1], buffer.speed[0],
                                     buffer.speed[1]):
            return None
        return buffer

    move = 150 / 4

    def hits_wall(pos1, pos2):
        if pos1 == pos2:
            return True
        toReturn = False
        width = 0.22
        dt = 1
        vector = (pos2[0] - pos1[0], pos2[1] - pos1[1])
        if vector[0] == vector[1] == 0:
            return True
        norm = (vector[0] ** 2 + vector[1] ** 2) ** 0.5
        normalised_vector = vector[0] / norm, vector[1] / norm
        perp_vector = 0, 0
        if normalised_vector[0] == 0 or normalised_vector[1] == 0:
            perp_vector = (normalised_vector[1], normalised_vector[0])
        else:
            perp_vector = (-(1 / normalised_vector[0]), 1 / normalised_vector[1])

        norm_perp = (perp_vector[0] ** 2 + perp_vector[1] ** 2) ** 0.5
        perp_vector = perp_vector[0] / norm_perp, perp_vector[1] / norm_perp

        for i in range(int(norm / dt)):
            new_x = pos1[0] + i / int(norm / dt) * vector[0]
            new_y = pos1[1] + i / int(norm / dt) * vector[1]

            if isAWall(new_x + perp_vector[0] * width / 2, new_y + perp_vector[1] * width / 2):
                toReturn = True
            if isAWall(new_x - perp_vector[0] * width / 2, new_y - perp_vector[1] * width / 2):
                toReturn = True

        return toReturn

    def isInBox(length, height, box_x, box_y, x, y):
        if box_x - length / 2 < x < box_x + length / 2:
            if box_y - height / 2 < y < box_y + height / 2:
                return True
        return False

    def isAWall(x, y):
        toReturn = False
        if abs(x) > 283 or abs(y) > 283:
            toReturn = True
        if isInBox(32, 32, 150, 150, x, y):
            toReturn = True
        if isInBox(32, 32, -150, -150, x, y):
            toReturn = True

        if isInBox(182, 32, -125, 200, x, y):
            toReturn = True

        if isInBox(32, 182, -200, 125, x, y):
            toReturn = True

        if isInBox(182, 32, 125, -200, x, y):
            toReturn = True

        if isInBox(32, 182, 200, -125, x, y):
            toReturn = True

        return toReturn

    projspeed = 300

    def smart_targeting(coos, target, opponent_pos_history: [(float, float, float)]):
        x = coos[0]
        y = coos[1]
        evilx = target[0].position[0]
        evily = target[0].position[1]
        spx = target[0].speed[0]
        spy = target[0].speed[1]

        print("TYPES : ", type(x), type(evilx))
        A = evilx - x
        B = -(evily - y)
        C = (A * spx + B * spy) / projspeed
        if B ** 2 + C ** 2 - A ** 2 > 0:
            return 2 * math.atan2(B + 2 * math.sqrt(B ** 2 + C ** 2 - A ** 2), projspeed)
        else:
            print("DEFAULT CASE")
            aim_x, aim_y = predict_pos(opponent_pos_history, x, y, evilx, evily)
            print("Aiming for (" + str(aim_x) + ", " + str(aim_y) + "); opponent is in (" + str(evilx) + ", " + str(
                evily) + ")")
            angle = get_angle(x, y, aim_x, aim_y)
            print("Angle : " + str(angle))
            return angle

    def predict_pos(position_history: [(float, float, float)], pos_x, pos_y, evil_x, evil_y):
        if evil_y == pos_y:
            pos_y += 10 ** -5
        if evil_x == pos_x:
            pos_x += 10 ** -5
        perpend = (-1 / (evil_x - pos_x), 1 / (evil_y - pos_y))
        sum_x, sum_y = 0, 0
        if len(position_history) < 3:
            return evil_x, evil_y

        dist = distance(position_history[len(position_history) - 1][0], position_history[len(position_history) - 1][1],
                        position_history[len(position_history) - 2][0], position_history[len(position_history) - 2][1])
        dist += distance(position_history[len(position_history) - 3][0], position_history[len(position_history) - 3][1],
                         position_history[len(position_history) - 2][0], position_history[len(position_history) - 2][1])
        if dist < 30:
            return evil_x, evil_y

        for i in range(3):
            sum_x += position_history[len(position_history) - i - 1][0]
            sum_y += position_history[len(position_history) - i - 1][1]

        prod_scalaire = perpend[0] * sum_x + perpend[1] * sum_y
        if prod_scalaire == 0:
            return evil_x, evil_y
        approx = (prod_scalaire * perpend[0] / length((prod_scalaire * perpend[0], prod_scalaire * perpend[1])),
                  prod_scalaire * perpend[1] / length((prod_scalaire * perpend[0], prod_scalaire * perpend[1])))
        dist = ((pos_x - evil_x) ** 2 + (pos_y - evil_y) ** 2) ** 0.5
        return approx[0] * factor * move * dist + evil_x, -approx[1] * factor * dist * move + evil_y

    def distance(x1, y1, x2, y2):
        return ((x1 - x2) ** 2 + (y1 - y2) ** 2) ** 0.5

    factor = 0.005
    player_size = 16

    def get_angle(pos1x, pos1y, pos2x, pos2y) -> float:
        v1_theta = math.atan2(pos1y - (pos2y - player_size), pos1x - (pos2x + player_size))
        r = v1_theta * (180.0 / math.pi)
        return r + 180

    def evil_ia(gamestate: SnapshotData, my_data: typing.Dict) -> Command:
        print("Starting evil AI algo")
        print("Checking for projectiles, found : " + str(len(gamestate.projectiles)))
        gametime = my_data.setdefault("time", 0) + 1
        my_data["time"] = gametime
        # opponent_pos_history = my_data.setdefault("pos_history", [])
        # my_data["pos_history"] = opponent_pos_history.append(gamestate.other_players[0].position)
        state = my_data.setdefault("state", 1)
        pos_x = gamestate.controlled_player.position[0]
        pos_y = gamestate.controlled_player.position[1]
        evil_pos_x = gamestate.other_players[0].position[0]
        evil_pos_y = gamestate.other_players[0].position[1]
        print("Current position (me): " + str(pos_x) + " - " + str(pos_y))
        print("Current position (the normal AI): " + str(evil_pos_x) + " - " + str(evil_pos_y))
        if pos_y < 220 and state == 1:
            return MyMoveCommand(2, (0.0, 1.0, 0.0))
        if pos_y >= 220 and state == 1:
            state = 2
            my_data["state"] = 2
        if pos_x > -220 and state == 2:
            return MyMoveCommand(2, (-1.0, 0.0, 0.0))
        if pos_x <= -220 and state == 2:
            state = 3
            my_data["state"] = 3
        if pos_y > -220 and state == 3:
            return MyMoveCommand(2, (0.0, -1.0, 0.0))
        if pos_y <= -220 and state == 3:
            state = 4
            my_data["state"] = 4
        if pos_x < 220 and state == 4:
            return MyMoveCommand(2, (1.0, 0.0, 0.0))
        if pos_x >= -220 and state == 4:
            state = 1
            my_data["state"] = 1

    def MyMoveCommand(agent, move_direction):
        return MoveCommand(move_direction)

    def ClampToCenterCommand(pos_x, pos_y):
        # if one of the coordinates is in the center
        if abs(pos_x) < 100 and pos_y > 100:
            return MoveCommand((random(), -1.0, 0.0))
        if abs(pos_x) < 100 and pos_y < -100:
            return MoveCommand((random(), 1.0, 0.0))
        if abs(pos_y) < 100 and pos_x > 100:
            return MoveCommand((-1.0, random(), 0.0))
        if abs(pos_y) < 100 and pos_x < -100:
            return MoveCommand((1.0, random(), 0.0))
        # both coordinate in the center
        if abs(pos_x) < 100 and abs(pos_y) < 100:
            rand = random()
            if rand < 0.25:
                return MoveCommand((100.0, 100.0, 0.0))
            if rand < 0.50:
                return MoveCommand((-100.0, 100.0, 0.0))
            if rand < 0.75:
                return MoveCommand((100.0, -100.0, 0.0))
            else:
                return MoveCommand((-100.0, -100.0, 0.0))
        # no coordinate in the center
        return MoveCommand((-pos_x * random(), -pos_y * random(), 0.0))

    def should_dodge(pos_x, pos_y, proj_x, proj_y, proj_dx, proj_dy):
        if projectile_going_away(pos_x, pos_y, proj_x, proj_y, proj_dx, proj_dy):
            return False

        if proj_dx == 0:
            angle = math.pi / 2
        else:
            angle = math.atan2(proj_dy, proj_dx)
        start_x_top = proj_x + 20 * math.cos(angle)
        start_y_top = proj_y + 20 * math.sin(angle)
        for i in range(-500, 500, 2):
            if (pos_x - 20 < compute_line(proj_dx, proj_dy, start_x_top, start_y_top, i)[0] < pos_x + 20) \
                    and (pos_y - 20 < compute_line(proj_dx, proj_dy, start_x_top, start_y_top, i)[1] < pos_y + 20):
                return True
        start_x_top = proj_x * math.cos(angle)
        start_y_top = proj_y * math.sin(angle)
        for i in range(-300, 300, 2):
            if (pos_x - 20 < compute_line(proj_dx, proj_dy, start_x_top, start_y_top, i)[0] < pos_x + 20) \
                    and (pos_y - 20 < compute_line(proj_dx, proj_dy, start_x_top, start_y_top, i)[1] < pos_y + 20):
                return True
        start_x_top = proj_x - 20 * math.cos(angle)
        start_y_top = proj_y - 20 * math.sin(angle)
        for i in range(-300, 300, 2):
            if (pos_x - 20 < compute_line(proj_dx, proj_dy, start_x_top, start_y_top, i)[0] < pos_x + 20) \
                    and (pos_y - 20 < compute_line(proj_dx, proj_dy, start_x_top, start_y_top, i)[1] < pos_y + 20):
                return True
        return False

    def compute_line(dx, dy, point_x, point_y, step):
        dx = dx / math.sqrt(math.pow(dx, 2) + math.pow(dy, 2))
        dy = dy / math.sqrt(math.pow(dx, 2) + math.pow(dy, 2))
        return point_x + dx * step, point_y + dy * step
    starting_co_x = my_data.setdefault("start", gamestate.controlled_player.position)[0]
    gametime = my_data.setdefault("time", 0) + 1
    my_data["time"] = gametime
    opponent_pos_history = my_data.setdefault("pos_history", [])
    my_data["pos_history"].append(gamestate.other_players[0].position)
    state = my_data.setdefault("state", 0)
    pos_x = gamestate.controlled_player.position[0]
    pos_y = gamestate.controlled_player.position[1]
    evil_pos_x = gamestate.other_players[0].position[0]
    evil_pos_y = gamestate.other_players[0].position[1]
    tbag = my_data.setdefault("tbag", 0)
    path = my_data.setdefault("horizontal", random() > 0.5)
    # health check
    my_health = gamestate.controlled_player.health
    evil_health = gamestate.other_players[0].health

    if evil_health < 0 and my_health < 0: # if both are dead
        tbag = 0
        my_data["tbag"] = 0
        state = 0
        my_data["state"] = 0

    if evil_health < 0 and my_health > 0 and abs(pos_x) < 150 and abs(pos_y) < 150:
        tbag = 1
        my_data["tbag"] = 1

    if tbag == 1:  # goes to middle
        if 0 <= math.fabs(pos_x) <= 1 and 0 <= math.fabs(pos_y) <= 1:
            tbag = 2
            my_data["tbag"] = 2
        elif math.sqrt(math.pow(pos_x, 2) + math.pow(pos_y, 2)) > move:
            return MoveCommand((-pos_x, -pos_y, 0.0))
        else:
            return MoveCommand((-pos_x, -pos_y, math.sqrt(math.pow(move, 2) - math.pow(pos_x, 2) - math.pow(pos_y, 2))))

    if tbag == 2:
        if starting_co_x > 0:  # if the enemy started top left
            if - 300 <= evil_pos_x <= 10 and evil_pos_y > 170:
                return ShootCommand(95)
            if 300 >= evil_pos_y >= 10 and evil_pos_x < -170:
                return ShootCommand(175)
        if starting_co_x < 0:  # if the enemy started bottom right
            if -300 <= evil_pos_y <= -10 and evil_pos_x > 170:
                return ShootCommand(-5)
            if 300 >= evil_pos_x >= 10 and evil_pos_y < -170:
                return ShootCommand(275)
        if evil_pos_x < -500:
            tbag = 1
            my_data["tbag"] = 1
        else:
            tbag = 0
            my_data["tbag"] = 0
            return ShootCommand(get_angle(pos_x, pos_y, evil_pos_x, evil_pos_y))

    # dodge préventif
    if state == 0 and starting_co_x < 0:  # if we're in the phase of approaching the center
        if not path:
            if pos_y > -10:
                return MoveCommand((0.0, -1.0, 0.0))
            if pos_x < -80 and abs(evil_pos_x) < 150 and abs(evil_pos_y) < 150:
                if random() < 0.5:
                    return MoveCommand((1.0, 1.0, 0.0))
                else:
                    return MoveCommand((1.0, -1.0, 0.0))
            if pos_x < -80:
                proj = get_closest(gamestate.projectiles, pos_x, pos_y)
                if proj is not None:
                    if should_dodge(pos_x, pos_y, proj.position[0], proj.position[1], proj.speed[0], proj.speed[1]):
                        if random() < 0.5:
                            if not should_dodge(pos_x + move/1.4, pos_y + move/1.4, proj.position[0], proj.position[1], proj.speed[0], proj.speed[1]):
                                return MoveCommand((1.0, 1.0, 0.0))
                            else:
                                return MoveCommand((1.0, -1.0, 0.0))
                        else:
                            if should_dodge(pos_x + move / 1.4, pos_y - move / 1.4, proj.position[0], proj.position[1],
                                                proj.speed[0], proj.speed[1]):
                                return MoveCommand((1.0, 1.0, 0.0))
                            else:
                                return MoveCommand((1.0, -1.0, 0.0))
                if random() < 0.3 or gametime < 25:
                    return MoveCommand((1.0, 0.0, 0.0))
                else:
                    aim_x, aim_y = predict_pos(opponent_pos_history, pos_x, pos_y, evil_pos_x, evil_pos_y)
                    angle = get_angle(pos_x, pos_y, aim_x, aim_y)
                    return ShootCommand(angle)
            if state == 0:
                state = 1
                my_data["state"] = 1
        elif state == 0:
            if pos_x < 10:
                return MoveCommand((1.0, 0.0, 0.0))
            if pos_y > 80 and abs(evil_pos_x) < 150 and abs(evil_pos_y) < 150:
                if random() < 0.5:
                    return MoveCommand((1.0, -1.0, 0.0))
                else:
                    return MoveCommand((-1.0, -1.0, 0.0))
            if pos_y > 80:
                proj = get_closest(gamestate.projectiles, pos_x, pos_y)
                if proj is not None:
                    if should_dodge(pos_x, pos_y, proj.position[0], proj.position[1], proj.speed[0], proj.speed[1]):
                        if random() < 0.5:
                            if not should_dodge(pos_x + move / 1.4, pos_y - move / 1.4, proj.position[0], proj.position[1],
                                                proj.speed[0], proj.speed[1]):
                                return MoveCommand((1.0, -1.0, 0.0))
                            else:
                                return MoveCommand((-1.0, -1.0, 0.0))
                        else:
                            if should_dodge(pos_x - move / 1.4, pos_y - move / 1.4, proj.position[0], proj.position[1],
                                            proj.speed[0], proj.speed[1]):
                                return MoveCommand((1.0, -1.0, 0.0))
                            else:
                                return MoveCommand((-1.0, -1.0, 0.0))

                if random() < 0.3 or gametime < 25:
                    return MoveCommand((0.0, -1.0, 0.0))
                else:
                    aim_x, aim_y = predict_pos(opponent_pos_history, pos_x, pos_y, evil_pos_x, evil_pos_y)
                    angle = get_angle(pos_x, pos_y, aim_x, aim_y)
                    return ShootCommand(angle)
            if state == 0:
                state = 1
                my_data["state"] = 1

    if state == 0 and starting_co_x > 0:  # if we're in the phase of approaching the center
        if not path:
            if pos_y < 10:
                return MoveCommand((0.0, 1.0, 0.0))
            if pos_x > 80 and abs(evil_pos_x) < 150 and abs(evil_pos_y) < 150:
                if random() < 0.5:
                    return MoveCommand((-1.0, 1.0, 0.0))
                else:
                    return MoveCommand((-1.0, -1.0, 0.0))
            if pos_x > 80:
                proj = get_closest(gamestate.projectiles, pos_x, pos_y)
                if proj is not None:
                    if should_dodge(pos_x, pos_y, proj.position[0], proj.position[1], proj.speed[0], proj.speed[1]):
                        if random() < 0.5:
                            if not should_dodge(pos_x - move / 1.4, pos_y + move / 1.4, proj.position[0], proj.position[1],
                                                proj.speed[0], proj.speed[1]):
                                return MoveCommand((-1.0, 1.0, 0.0))
                            else:
                                return MoveCommand((-1.0, -1.0, 0.0))
                        else:
                            if should_dodge(pos_x - move / 1.4, pos_y - move / 1.4, proj.position[0], proj.position[1],
                                            proj.speed[0], proj.speed[1]):
                                return MoveCommand((-1.0, 1.0, 0.0))
                            else:
                                return MoveCommand((-1.0, -1.0, 0.0))
                if random() < 0.3 or gametime < 25:
                    return MoveCommand((-1.0, 0.0, 0.0))
                else:
                    aim_x, aim_y = predict_pos(opponent_pos_history, pos_x, pos_y, evil_pos_x, evil_pos_y)
                    angle = get_angle(pos_x, pos_y, aim_x, aim_y)
                    return ShootCommand(angle)
            if state == 0:
                state = 1
                my_data["state"] = 1
        else:
            if pos_x > -10:
                return MoveCommand((-1.0, 0.0, 0.0))
            if pos_y < -80 and abs(evil_pos_x) < 150 and abs(evil_pos_y) < 150:
                if random() < 0.5:
                    return MoveCommand((1.0, 1.0, 0.0))
                else:
                    return MoveCommand((-1.0, 1.0, 0.0))
            if pos_y < -80:
                proj = get_closest(gamestate.projectiles, pos_x, pos_y)
                if proj is not None:
                    if should_dodge(pos_x, pos_y, proj.position[0], proj.position[1], proj.speed[0], proj.speed[1]):
                        if random() < 0.5:
                            if not should_dodge(pos_x + move / 1.4, pos_y + move / 1.4, proj.position[0], proj.position[1],
                                                proj.speed[0], proj.speed[1]):
                                return MoveCommand((1.0, 1.0, 0.0))
                            else:
                                return MoveCommand((-1.0, 1.0, 0.0))
                        else:
                            if should_dodge(pos_x - move / 1.4, pos_y + move / 1.4, proj.position[0], proj.position[1],
                                            proj.speed[0], proj.speed[1]):
                                return MoveCommand((1.0, 1.0, 0.0))
                            else:
                                return MoveCommand((-1.0, 1.0, 0.0))
                if random() < 0.3 or gametime < 25:
                    return MoveCommand((0.0, 1.0, 0.0))
                else:
                    aim_x, aim_y = predict_pos(opponent_pos_history, pos_x, pos_y, evil_pos_x, evil_pos_y)
                    angle = get_angle(pos_x, pos_y, aim_x, aim_y)
                    return ShootCommand(angle)
            if state == 0:
                state = 1
                my_data["state"] = 1

    if my_health < 0:
        state = 0
        my_data["state"] = 0
        my_data["horizontal"] = random() > 0.5
        path = my_data["horizontal"]

    if distance(pos_x, pos_y, evil_pos_x, evil_pos_y) < 50:  # close combat
        return ShootCommand(get_angle(pos_x, pos_y, evil_pos_x, evil_pos_y))

    shoot = random() < 0.8

    if hits_wall((pos_x, pos_y), (evil_pos_x, evil_pos_y)):
        shoot = False

    for proj in gamestate.projectiles:
        if should_dodge(pos_x, pos_y, proj.position[0], proj.position[1], proj.speed[0], proj.speed[1]):
            if math.fabs(pos_x) < 100 and math.fabs(pos_y) < 100:  # inside center square
                speedx = proj.speed[0]
                speedy = proj.speed[1]
                if speedx == 0:
                    speedx = 10 ** -5
                if speedy == 0:
                    speedy = 10 ** -5
                perpendicular1 = -1 / speedx, 1 / speedy
                perpendicular2 = 1 / proj.speed[0], -1 / speedy
                per1norm = normalize(perpendicular1)
                per2norm = normalize(perpendicular2)
                next_pos1 = pos_x + move * per1norm[0], pos_y + move * per1norm[1]
                next_pos2 = pos_x + move * per2norm[0], pos_y + move * per2norm[1]
                if math.pow(next_pos1[0], 2) + math.pow(next_pos1[1], 2) < math.pow(next_pos2[0], 2) + math.pow(
                        next_pos2[1], 2):
                    return MoveCommand((per1norm[0], per1norm[1], 0.0))
                else:
                    return MoveCommand((per2norm[0], per2norm[1], 0.0))

    if state == 1 and not shoot:  # if we're in the phase rotating
        return ClampToCenterCommand(pos_x, pos_y)

    if state == 1 and shoot:
        aim_x, aim_y = predict_pos(opponent_pos_history, pos_x, pos_y, evil_pos_x, evil_pos_y)
        print("Aiming for (" + str(aim_x) + ", " + str(aim_y) + "); opponent is in (" + str(evil_pos_x) + ", " + str(
            evil_pos_y) + ")")
        angle = get_angle(pos_x, pos_y, aim_x, aim_y)
        print("Angle : " + str(angle))
        return ShootCommand(angle)

    return ShootCommand(get_angle(pos_x, pos_y, evil_pos_x, evil_pos_y))
