FROM python:3.8-bullseye

RUN mkdir /scheduler/
COPY requirements.txt /scheduler/requirements.txt
RUN pip install -r /scheduler/requirements.txt

COPY agent.py /scheduler/agent.py
COPY game_simulation.py /scheduler/game_simulation.py
COPY scheduler.py /scheduler/scheduler.py
COPY pool_data.json /scheduler/pool_data.json
COPY challenge.jar /scheduler/challenge.jar

COPY teams/ /scheduler/teams/

CMD cd scheduler && python3 scheduler.py
