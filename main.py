import platform
from datetime import datetime
from DataCollector import Database

import psutil as psutil
from aiohttp import web

db = Database()
routes = web.RouteTableDef()


async def get_size(_bytes, suffix="B"):
    factor = 1024
    for unit in ["", "K", "M", "G", "T", "P"]:
        if _bytes < factor:
            return f"{_bytes:.2f}{unit}{suffix}"
        _bytes /= factor


@routes.get('/smtool/api/v0.1/platform')
async def platform_info(request):
    uname = platform.uname()
    cpufreq = psutil.cpu_freq()
    svmem = psutil.virtual_memory()
    swap = psutil.swap_memory()

    print('Reloaded platform info', datetime.now())

    return web.json_response({
                            'System': uname.system,
                            'Node': uname.node,
                            'Release': uname.release,
                            'Version': uname.version,
                            'Machine': uname.machine,
                            'Processor': uname.processor,
                            'Boot time': str(datetime.fromtimestamp((psutil.boot_time()))),
                            'Physical cores': psutil.cpu_count(logical=False),
                            'Total cores': psutil.cpu_count(logical=True),
                            'Max Frequency': f'{cpufreq.max:.2f} Mhz',
                            'Min Frequency': f'{cpufreq.min:.2f} Mhz',
                            'Total': await get_size(svmem.total),
                            'Total swap': await get_size(swap.total),
                        }, status=200)


@routes.get('/smtool/api/v0.1/disks_info')
async def disks_info(request):
    print('Reloaded disk info', datetime.now())

    return web.json_response(
        {'disks': [
            {'Device': partition.device,
             'Mountpoint': partition.mountpoint,
             'File system type': partition.fstype,
             'Total Size': await get_size(psutil.disk_usage(partition.mountpoint).total),
             'Used': await get_size(psutil.disk_usage(partition.mountpoint).used)}
            for partition in psutil.disk_partitions()]}, status=200)


@routes.get('/smtool/api/v0.1/data_collector')
async def collect_info(request):
    return web.json_response(await db.get_data(), status=200)


@routes.get('/smtool/api/v0.1/network_info')
async def network_info(request):
    print('Reloaded network info', datetime.now())
    interfaces = psutil.net_if_addrs()
    return web.json_response({'interfaces': [{'v4':
                                                  {'name': network,
                                                   'address': interfaces[network][0].address,
                                                   'broadcast': interfaces[network][0].broadcast,
                                                   'mask': interfaces[network][0].netmask},
                                              'v6': {'name': network,
                                                     'address': interfaces[network][1].address,
                                                     'broadcast': interfaces[network][1].broadcast,
                                                     'mask': interfaces[network][1].netmask}
                                              } for network in interfaces]}, status=200)


app = web.Application()
app.add_routes(routes)
web.run_app(app)
