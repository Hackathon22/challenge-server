# -*- coding: utf-8 -*-
"""
Created on Sun Mar 27 10:49:45 2022

@author: natha
"""

from game_simulation import GameSimulation, SnapshotData
from agent import MoveCommand, ShootCommand, Command
import numpy as np
import typing
from scipy.optimize import newton


def my_data():
	return {}

def my_ai(gamestate: SnapshotData, my_data: typing.Dict) -> Command:
    MISSILE_SPEED = 300

    def shooting(x,ennemy_pos,ennemy_speed,player_pos):
        d = np.array([np.cos(x),np.sin(x),0])
        
        ve = ennemy_speed
        vm = MISSILE_SPEED*d
        
        t = (ennemy_pos[0] - player_pos[0])/(vm[0]-ve[0])
        
        pe = ennemy_pos+t*ennemy_speed
        pm = player_pos+t*MISSILE_SPEED*d
        
        return np.linalg.norm(pm-pe)

    def shoot_on_ennemy_prevision(gamestate):
        player_pos = np.array(gamestate.controlled_player.position)
        ennemy_pos = np.array(gamestate.other_players[0].position)
        ennemy_speed  = np.array(gamestate.other_players[0].speed)
        
        dir0 = ennemy_pos-player_pos
        #Calcul de l'angle de tir
        angle0 = shoot_dir(dir0)
        x0 = angle0
        
        try: 
            res = newton(shooting,x0=x0,args=(ennemy_pos,ennemy_speed,player_pos))
            if(shooting(res,ennemy_pos,ennemy_speed,player_pos)<0.01):
                return res*180/np.pi
            else:
                return angle0*180/(np.pi)
        except:
            return angle0*180/(np.pi)
        
        
    def shoot_dir(direction):
        
        dx = direction[0]
        dy = direction[1]
        L = np.sqrt(dx**2+dy**2)
        
        angle0 = np.arccos(dx/L)
        if(dy<0):
            angle0 = 2*np.pi-angle0
            
        return angle0
        
        
    def shoot_corner(gamestate,corner):

        """
        Corner: up or down
        """
        
        ennemy_pos = np.array(gamestate.other_players[0].position)
        player_pos = np.array(gamestate.controlled_player.position)
        x,y = ennemy_pos[0],ennemy_pos[1]
        
        if(corner == 'up'):
            target_1 = np.array([0,200,0])
            target_2 = np.array([-200,0,0])
            if(-x>y): 
                target = target_2
            else:
                target = target_1
        else:
            target_1 = np.array([0,-200,0])
            target_2 = np.array([200,0,0])
            if(-x>y):   
                target = target_1
            else:
                target = target_2
                
        return shoot_dir(target-player_pos)*180/np.pi
         
    def inside_corner(pos):
        
        if( (pos[0] < -200 and pos[1] > 20) or (pos[0] < -20 and pos[1] > 200)):
            corner = 'up'
        elif( (pos[0] > 200 and pos[1] < -20) or (pos[0] > 20 and pos[1] < -200)):
            corner = 'down'
        else:
            corner = 'not'
            
        return corner    
            
    def shooting_decision(world_map,gamestate):
        
        ennemy_pos = np.array(gamestate.other_players[0].position)
        #34 = 125-91
        corner = inside_corner(ennemy_pos)
        if(corner != 'not'):
            res = shoot_corner(gamestate,corner)
        else:
            res = shoot_on_ennemy_prevision(gamestate) 
            
        return res
        


    def should_evade(gamestate):
        
        distance_limit = 50
        dt = 1e-5
        
        projectile_list = gamestate.projectiles
        player_pos = np.array(gamestate.controlled_player.position)
        
        for projectile in projectile_list:
            projectile_pos = np.array(projectile.position)
            projectile_speed = np.array(projectile.speed)
            if(-np.linalg.norm(player_pos-projectile_pos) + np.linalg.norm(projectile_pos+projectile_speed*dt-player_pos) < 0):
                if(np.linalg.norm(player_pos-projectile_pos)<distance_limit):
                    return True
        
        return False

    def evade_all(board,gamestate,speed):
        
        projectile_list = gamestate.projectiles
        
        for projectile in projectile_list:
            speed = evade(projectile,gamestate,speed)
        return speed
            
    def evade(projectile,gamestate,speed):
        
        sec_dist = 1e-5
        eps = 1e-6
        dt = 1e-5
        k=10000
        
        player_pos = np.array(gamestate.controlled_player.position)
        projectile_pos = np.array(projectile.position)
        projectile_speed = np.array(projectile.speed)
        
        #rien que pour toi sacha
        if(-np.linalg.norm(player_pos-projectile_pos) + np.linalg.norm(projectile_pos+projectile_speed*dt-player_pos) < sec_dist):
            
            perp_vit = np.cross(projectile_speed,[0,0,1])
            perp_vit /= np.linalg.norm(perp_vit)
            z_cross = np.cross(projectile_speed,player_pos-projectile_pos)[2]
            #If player-projectile positions is parallel to projectile speed
            #We move to a random direction
            if(np.abs(z_cross) < 1e-16):
                direction = 1
            else:
                direction = np.sign(z_cross)
            
            f_rep = k/(np.linalg.norm(projectile_pos-player_pos)**2+eps)
            
            speed -= perp_vit*f_rep*direction
        
        return speed

    def get_out_corner(gamestate,corner):
        
        ennemy_pos = np.array(gamestate.other_players[0].position)
        x,y = ennemy_pos[0],ennemy_pos[1]
        
        if(corner == 'up'):
            if(-x<y):
                speed = np.array([0.0,-1.0,0.0])
            else:
                speed = np.array([1.0,0.0,0.0])
                
        if(corner == 'down'):
            if(-y>x):
                speed = np.array([0.0,1.0,0.0])
            else:
                speed = np.array([-1.0,0.0,0.0])
            
        return speed

    if my_data.get('counter') is None:
        my_data['counter'] = 1
    else:
        my_data['counter'] += 1
    
    player_pos = gamestate.controlled_player.position
    
    corner = inside_corner(player_pos)    
    
    if(corner != 'not'):
        speed = get_out_corner(gamestate,corner)
        return MoveCommand((speed[0],speed[1],speed[2]))
    
    if(np.abs(player_pos[0]) > 100 or np.abs(player_pos[1]) > 100):
        speed = -np.array(gamestate.controlled_player.position)
        speed = evade_all(None,gamestate,speed)
        return MoveCommand((speed[0],speed[1],speed[2]))
    
    if(should_evade(gamestate)):
        speed = np.array([0.0, 0.0, 0.0])
        speed = evade_all(None,gamestate,speed)
        return MoveCommand((speed[0],speed[1],speed[2]))
    else:
        res = shooting_decision(None,gamestate)
        return ShootCommand(res)
