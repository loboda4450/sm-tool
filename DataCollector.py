import asyncio
import sqlite3
from datetime import datetime

import psutil


class Database:
    def __init__(self):
        self.conn = conn = sqlite3.connect('updatable.db')
        self.c = c = conn.cursor()

        c.execute("""CREATE TABLE IF NOT EXISTS datatable(id INTEGER PRIMARY KEY AUTOINCREMENT, curr_cpu_freq FLOAT, 
        available_ram FLOAT, used_ram FLOAT, available_swap FLOAT, used_swap FLOAT, timestamp DATE)""")

        loop = asyncio.get_event_loop()
        task = loop.create_task(self.update())

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
        return {'Data accesed': str(datetime.now())}
