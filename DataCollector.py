import asyncio
import sqlite3
from datetime import datetime

import psutil


class Database:
    def __init__(self):
        self.conn = conn = sqlite3.connect('updatable.db')
        self.c = conn.cursor()
        self.loop = None
        self.task = None

        self.c.execute("""CREATE TABLE IF NOT EXISTS datatable(id INTEGER PRIMARY KEY AUTOINCREMENT, curr_cpu_freq FLOAT, 
        available_ram FLOAT, used_ram FLOAT, available_swap FLOAT, used_swap FLOAT, timestamp DATE)""")

        self.loop = asyncio.get_event_loop()
        self.task = self.loop.create_task(self.update())

    async def update(self):
        while True:
            timestamp = datetime.now()
            print('Adding value to db', timestamp)
            self.c.execute(
                "INSERT INTO datatable (curr_cpu_freq, available_ram, used_ram, available_swap, used_swap, timestamp) VALUES (?, ?, ?, ?, ?, ?)",
                (round(psutil.cpu_freq().current, 2), round(psutil.virtual_memory().available/1073741824, 2), round(psutil.virtual_memory().used/1073741824, 2),
                 round(psutil.swap_memory().free/1073741824, 2), round(psutil.swap_memory().used/1073741824, 2), timestamp))
            self.conn.commit()
            await asyncio.sleep(1)

    async def get_data(self):
        print('Data accesed', str(datetime.now()))
        return {'Collected data': [{'cpufreq': row[0],
                                    'availram': row[1],
                                    'usedram': row[2],
                                    'availswap': row[3],
                                    'usedswap': row[4],
                                    'timestamp': row[5]} for row in
                                   self.c.execute("""SELECT curr_cpu_freq, available_ram, used_ram, available_swap, used_swap, timestamp FROM datatable ORDER BY timestamp DESC LIMIT 21600""")]}

    async def get_cpu_data(self):
        print('CPU data accesed', datetime.now())
        return {'Collected data': [{'cpufreq': row[0],
                                    'timestamp': row[1]}
                                   for row in self.c.execute(
                """SELECT curr_cpu_freq, timestamp FROM datatable ORDER BY timestamp DESC LIMIT 21600""")]}

    async def get_ram_data(self):
        print('RAM data accesed', datetime.now())
        return {'Collected data': [{'availram': row[0],
                                    'usedram': row[1],
                                    'timestamp': row[2]}
                                   for row in self.c.execute(
                """SELECT available_ram, used_ram, timestamp FROM datatable ORDER BY timestamp DESC LIMIT 21600""")]}

    async def get_swap_data(self):
        print('SWAP data accesed', datetime.now())
        return {'Collected data': [{'availswap': row[0],
                                    'usedswap': row[1],
                                    'timestamp': row[2]}
                                   for row in self.c.execute(
                """SELECT available_swap, used_swap, timestamp FROM datatable ORDER BY timestamp DESC LIMIT 21600""")]}
