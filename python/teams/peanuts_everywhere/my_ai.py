from game_simulation import GameSimulation, SnapshotData
from agent import MoveCommand, ShootCommand, Command
import typing
    
    
from game_simulation import GameSimulation, SnapshotData
from agent import MoveCommand, ShootCommand, Command
import typing
from shapely.geometry import Point, Polygon,LineString
from shapely.ops import nearest_points
from math import *
import numpy as np

def my_ai(gamestate: SnapshotData, my_data: typing.Dict) -> Command:
    
    def predicting_shoot_angle():
        d = sqrt(pow(x_diff,2) + pow(y_diff,2))
        D = 4 * pow(d,2) * pow(v,2) * pow(cos(gamma),2) + 4 * (pow(300,2) - 2 * pow(v,2))
        t = (2 * d * v * cos(gamma) + sqrt(D)) / (2 * (pow(300,2) - 2 * pow(v,2)))
        shoot_correction = np.arctan2((v * sin(gamma) * t),(d + v * cos(gamma) *t))
        return (shoot_correction)

    def shoot_angle(x_c, y_c,x_o,y_o):
        y_diff = y_c - y_o
        x_diff = x_c - x_o
        angle = atan(y_diff/x_diff)
        angle = (angle / np.pi)*180
        if x_diff > 0: #our opponent is to our left
            angle = 180 - angle
        if y_diff > 0: #our opponent is under us
            angle = 360 - angle
        return (angle)

    def movement_path(x,y): 
        p1, p2 = nearest_points(Point(x,y), zone)
        dy=p2.y-y
        dx=p2.x-x
        return(MoveCommand((dx,dy,0)))
        
        
    

    def from_vect_to_angle(x,y):
        if x==0 and y>=0:
            return(90.)
        elif x==0 and y<0:
            return(-90.)
        else:    
            gamma = atan2(y,x)
            gamma = (gamma / np.pi)*180
            if gamma < 0:
                gamma == 360-gamma
            return(gamma)    

   
 
    def dodge(x_c,y_c,x_o,y_o,gamma_we,gamma_missile,v_c,v_missile,v_px,v_py):    
        ty = (y_c - y_o) / (v_c * sin(gamma_we) - v_missile * sin(gamma_missile))
        tx = (x_c - x_o) / (v_c * cos(gamma_we) - v_missile * cos(gamma_missile))
        if abs(abs(ty) - abs(tx)) < 0.3:
            return(MoveCommand((v_py,-v_px,0))) 
        else:
            pass
        #if abs(gamma_we - gamma_missile) > 135 and abs(gamma_we - gamma_missile) < 225:
         #   x = x0_we
         #   x0_we = -y0_we
         #   y0_we = x           
        '''
        This function is the deliverable that the participants need to upload at the end of the event.
        
        It has to return either a MoveCommand((dx, dy, 0.0)), where dx and dy are float value. 
        (The vector is normalized to 1).
        
        Or it can return a ShootCommand(angle), where the angle is in degrees and in counterclockwise order.
        (Angle 0.0 corresponds to EAST, 270.0° to SOUTH etc...)
        
        The gamestate variable is a collection of information containing the player data as well as the projectile data.
        Please check the SnapshotData class in the agent.py script.
        
        The my_data variable is a dictionnary that persists each time that the function is called. Its an utilitary
        that can be used by the participants at will. 
        
        Its also returned at the END of the game simulation, so it can be used for everything.
        
        The obstactles in game are always the same and described in another document.  Feel free to hardcode them inside the function.
        '''
    x_c=gamestate.controlled_player.position[0]
    y_c=gamestate.controlled_player.position[1]
    v_cx=gamestate.controlled_player.speed[0]
    v_cy=gamestate.controlled_player.speed[1]
    v_c=np.sqrt(v_cx**2+v_cy**2)
    
    x_o=gamestate.other_players[0].position[0]
    y_o=gamestate.other_players[0].position[1]
    v_ox=gamestate.other_players[0].speed[0]
    v_oy=gamestate.other_players[0].speed[1]
    v_o=np.sqrt(v_ox**2+v_oy**2)
    
    gamma_we = from_vect_to_angle(v_ox,v_oy)
    
    if(x_c>216 and -y_c>24):
        return(MoveCommand((0.0,1.0,0.0)))
    else:
        #p1, p2 = nearest_points(Point(x_c,y_c), zone)
        #if Point(x_c,y_c)
        if (x_c<100 and x_c>-100) and (y_c<100 and y_c>-100) :
            #if anti_kamikaze(shoot_angle(x_c, y_c,x_o,y_o),x_c,y_c,x_o,y_o):
            #my_data["shootcounter"] = 1
            return(ShootCommand(shoot_angle(x_c, y_c,x_o,y_o)))
        if len(gamestate.projectiles)==0:
            
            #command=go_to_center(x_c,y_c)
            command=movement_path(x_c,y_c) 
        else:
            for i in range(len(gamestate.projectiles)):
                x_p=gamestate.projectiles[i].position[0]
                y_p=gamestate.projectiles[i].position[1]
                v_px=gamestate.projectiles[i].speed[0]
                v_py=gamestate.projectiles[i].speed[1]
                v_missile=np.sqrt(v_px**2+v_py**2)
                gamma_missile = from_vect_to_angle(v_px,v_py)
                command=dodge(x_c,y_c,x_o,y_o,gamma_we,gamma_missile,v_c,v_missile,v_px,v_py)
            command=movement_path(x_c,y_c) 
            #command=go_to_center(x_c,y_c) 
    return(command)

def my_data():
    return({})
