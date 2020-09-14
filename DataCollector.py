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
                (psutil.cpu_freq().current, psutil.virtual_memory().available, psutil.virtual_memory().used, psutil.swap_memory().free, psutil.swap_memory().used, timestamp))
            self.conn.commit()
            await asyncio.sleep(1)

    async def get_data(self):
        print('Data accesed', str(datetime.now()))
        return {'Collected data': [{'cpufreq': row[1],
                                    'availram': row[2],
                                    'usedram': row[3],
                                    'availswap': row[4],
                                    'usedswap': row[5],
                                    'timestamp': row[6]} for row in self.c.execute("""SELECT * FROM datatable LIMIT 21600""")]}
