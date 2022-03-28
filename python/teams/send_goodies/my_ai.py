from game_simulation import GameSimulation, SnapshotData
from agent import InvalidCommand, MoveCommand, ShootCommand, Command
import typing
import time
import random
import math

def my_data():
    return {}



def my_ai(gamestate: SnapshotData, my_data: typing.Dict) -> Command:
    me = gamestate.controlled_player
    op = gamestate.other_players[0]

    """
    DIRECTIONS
    """
    no_move = (0.0, 0.0, 0.0)
    left_bottom = (-1.0, -1.0, 0.0)
    left = (-1.0, 0.0, 0.0)
    left_top = (-1.0, 1.0, 0.0)
    top = (0.0, 1.0, 0.0)
    right_top = (1.0, 1.0, 0.0)
    right = (1.0, 0.0, 0.0)
    right_bottom = (1.0, -1.0, 0.0)
    bottom = (0.0, -1.0, 0.0)
    dirs = [[left, right], [bottom, top]]

    """
    CENTER MARTO
    """

    def get_opposite_position(player):
        x, y = player.position[0], player.position[1]
        positions = []
        if x < 0: positions.append(right)
        if x > 0: positions.append(left)
        if y < 0: positions.append(top)
        if y > 0: positions.append(bottom)
        return positions

    def is_in_center(player):
        x, y = player.position[0], player.position[1]
        return -100.0 <= x <= 100.0 and -100.0 <= y <= 100.0

    def is_in_good_corner():
        if my_data['spawn'] == 0:
            return -100.0 <= x <= -64.0 and 64.0 <= y <= 100.0
        else:
            return 64.0 <= x <= 100.0 and -100.0 <= y <= -64.0

    def is_in_corner(player):
        x, y = player.position[0], player.position[1]
        return (-100.0 <= x <= -64.0 and 64.0 <= y <= 100.0) \
               or (-100.0 <= x <= -64.0 and -100.0 <= y <= -64.0) \
               or (64.0 <= x <= 100.0 and 64.0 <= y <= 100.0) \
               or (64.0 <= x <= 100.0 and -100.0 <= y <= -64.0)

    def run_marto(player):
        x, y = player.position[0], player.position[1]
        if my_data.get('next_dir') is None:
            my_data['next_dir'] = random.randint(0, 1)

        if is_in_corner(player):
            my_data['next_dir'] = random.randint(0, 1)
            d = get_opposite_position(player)[random.randint(0, 1)]
            return MoveCommand(d)

        d = 1 if (x >= 70 or x <= -70) else 0

        my_data['next_move'] = dirs[d][my_data['next_dir']]
        return MoveCommand(my_data['next_move'])

    # Game_State #
    x = gamestate.controlled_player.position[0]
    y = gamestate.controlled_player.position[1]
    team = gamestate.controlled_player.team

    """
    Starter randomly chosing a path to go to center
    """

    def path_to_center():
        if not team:
            paths = [
                [right, right, right, right, right, right, right, bottom, bottom, bottom, bottom],
                [right, right, right, right, right, right, right, right, right, bottom, bottom, bottom, bottom],
                [right, right, right, right, right, right, right, bottom, bottom, bottom, left, left, left, bottom],

                [bottom, bottom, bottom, bottom, bottom, bottom, bottom, right, right, right, right],
                [bottom, bottom, bottom, bottom, bottom, bottom, bottom, bottom, bottom, right, right, right, right],
                [bottom, bottom, bottom, bottom, bottom, bottom, bottom, right, right, right, top, top, top, right]
            ]
        else:
            paths = [
                [left, left, left, left, left, left, left, top, top, top, top],
                [left, left, left, left, left, left, left, left, left, top, top, top, top],
                [left, left, left, left, left, left, left, top, top, top, right, right, right, top],

                [top, top, top, top, top, top, top, left, left, left, left],
                [top, top, top, top, top, top, top, top, top, left, left, left, left],
                [top, top, top, top, top, top, top, left, left, left, bottom, bottom, bottom, left]
            ]

        i = random.randint(0, len(paths) - 1)
        return paths[i]

    """
    CHECK SPAWN
    """

    def is_in_spawn():
        if not team:
            return x == -250 and y == 250
        else:
            return x == 250 and y == -250

    def is_in_spawn_zone(player):
        x = player.position[0]
        y = player.position[1]
        if y == -1000 or x == -1000:
            return True
        if my_data['spawn'] == 0:  # top left so enemy right bottom
            return (y <= 0 and x >= 200) or (y <= -200 and x >= 0)
        else:
            return (y <= 0 and x <= -200) or (y >= 200 and x <= 0)

    """
    SHOOTING
    """

    def predict_pos_op_exit(x, y):
        if my_data['spawn'] == 0:
            return (200, -16) if y >= -200 else (8, -200)
        else:
            return (-16, 208) if x >= -200 else (-208, 8)


    def shoot(ennemy_x,ennemy_y):
        player = gamestate.controlled_player
        player_x, player_y = player.position[0], player.position[1] - 12.5

        def eval_angle(target=[0, 0]):  # default is center
            dy, dx = abs(target[1] - player_y), abs(target[0] - player_x)
            radians = math.atan2(dy, dx)
            return abs(math.degrees(radians))

        # print(180 + eval_angle([ennemy_x, ennemy_y]))
        angle = 0

        if player_x < ennemy_x and player_y > ennemy_y:
            angle = 360 - eval_angle([ennemy_x, ennemy_y])

        if player_x > ennemy_x and player_y < ennemy_y:
            angle = 180 - eval_angle([ennemy_x, ennemy_y])

        if player_x < ennemy_x and player_y < ennemy_y:
            angle = eval_angle([ennemy_x, ennemy_y])

        if player_x > ennemy_x and player_y > ennemy_y:
            angle = 180 + eval_angle([ennemy_x, ennemy_y])

        return ShootCommand(angle)
    """
    RETURN TO CENTER WHEN COLLISION WITH ENNEMY
    """

    def go_to_corner():
        if my_data['spawn'] == 0:
            if x >= -64:
                return MoveCommand(left)
            elif y <= 64:
                return MoveCommand(top)
        else:
            if x <= 64:
                return MoveCommand(right)
            elif y >= -64:
                return MoveCommand(bottom)

    def return_in_center():
        if x >= 100:
            return MoveCommand(left)
        elif x <= -100:
            return MoveCommand(right)
        elif y >= 100:
            return MoveCommand(bottom)
        elif y <= -100:
            return MoveCommand(top)

    def reset_data():
        my_data[0] = -1
        my_data[1] = path_to_center()
        my_data[2] = len(my_data[1])

    # Go to center values
    if is_in_spawn():
        reset_data()
        my_data['spawn'] = 0 if x == -250 else 1  # 0 if spawn in top left 1 else

    if my_data.get('counter') is None:
        my_data['counter'] = 0
    my_data['counter'] += 1

    # print(me.team, "position : (", x, ",", y, ")", " hp = ", me.health, " : in center ", is_in_center(me),
    #       " | in corner ", is_in_corner(me), " | ennemy in spawn ", is_in_spawn_zone(op))
    """
    MAIN DECISIONS
    """

    op_x, op_y = op.position[0], op.position[1]

    in_center = is_in_center(me)

    if in_center and is_in_spawn_zone(op):
        if not is_in_good_corner():
            return go_to_corner()

        enemy_exit_x, enemy_exit_y = predict_pos_op_exit(op_x, op_y)

        return shoot(enemy_exit_x, enemy_exit_y)

    if my_data[0] >= 6 and my_data['counter'] % 4 == 0:
        my_data['counter'] = 0
        return shoot(op_x, op_y)

    # IF PLAYER DON'T SHOOT
    my_data[0] += 1
    move_index, path, len_path = my_data[0], my_data[1], my_data[2]

    if not in_center:  # Go to center
        if move_index >= len_path:
            return return_in_center()
        return MoveCommand(path[move_index])
    else:  # Other move
        return run_marto(me)
