import platform
import crypt
import spwd
from datetime import datetime
from DataCollector import Database

import psutil
from aiohttp import web

db = Database()
routes = web.RouteTableDef()


async def get_size(_bytes, suffix="B"):
    factor = 1024
    for unit in ["", "K", "M", "G", "T", "P"]:
        if _bytes < factor:
            return f"{_bytes:.2f}{unit}{suffix}"
        _bytes /= factor


async def verify(login, passwd):
    try:
        enc_pwd = spwd.getspnam(login)[1]
        if crypt.crypt(passwd, enc_pwd) == enc_pwd:
            return True
        else:
            return False
    except KeyError:
        return False


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
        'Total swap': await get_size(swap.total)}, status=200)


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


@routes.get('/smtool/api/v0.1/network_info')
async def network_info(request):
    print('Reloaded network info', datetime.now())
    interfaces = psutil.net_if_addrs()
    if platform.uname().system == 'Windows':
        return web.json_response({'interfaces': [{'Name': network,
                                                  'IPv4': interfaces[network][1].address,
                                                  'Broadcast for IPv4': interfaces[network][1].broadcast,
                                                  'Mask for v4': interfaces[network][1].netmask,
                                                  'Address for IPv6': interfaces[network][0].address,
                                                  'Multicast': interfaces[network][0].broadcast,
                                                  'Mask for IPv6': interfaces[network][0].netmask
                                                  } for network in interfaces]}, status=200)
    else:
        return web.json_response({'interfaces': [{'Name': network,
                                                  'IPv4': interfaces[network][0].address,
                                                  'Broadcast for IPv4': interfaces[network][0].broadcast,
                                                  'Mask for v4': interfaces[network][0].netmask,
                                                  'Address for IPv6': interfaces[network][1].address,
                                                  'Multicast': interfaces[network][1].broadcast,
                                                  'Mask for IPv6': interfaces[network][1].netmask
                                                  } for network in interfaces]}, status=200)


@routes.get('/smtool/api/v0.1/process_info')
async def process_info(request):
    info = list()
    for process in psutil.process_iter():
        try:
            info.append({'Process name': process.name(),
                         'PID': process.pid,
                         'Woken by': process.username(),
                         'Status': process.status()})

        except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
            pass
    print('Reloaded process info', datetime.now())
    return web.json_response({'Process list': info}, status=200)


@routes.get('/smtool/api/v0.1/data_collector')
async def collect_info(request):
    return web.json_response(await db.get_data(), status=200)


@routes.get('/smtool/api/v0.1/cpu_info')
async def collect_cpu_info(request):
    return web.json_response(await db.get_cpu_data(), status=200)


@routes.get('/smtool/api/v0.1/ram_info')
async def collect_ram_info(request):
    return web.json_response(await db.get_ram_data(), status=200)


@routes.get('/smtool/api/v0.1/swap_info')
async def collect_swap_info(request):
    return web.json_response(await db.get_swap_data(), status=200)


@routes.get('/smtool/api/v0.1/verify')
async def verify_access(request):
    try:
        login = request.rel_url.query['login']
        passwd = request.rel_url.query['passwd']
        verification = await verify(login, passwd)
        print(f'Login attempt, login={login}, password={passwd}, success={verification}', datetime.now())

        return web.json_response({'status': True},
                                 status=200) if verification else web.json_response({'status': False}, status=401)
    except Exception as e:
        print(f'Login attempt, success={False}', {str(e)}, datetime.now())
        return web.json_response({'status': 'Missing login or password'}, status=401)


app = web.Application()
app.add_routes(routes)
web.run_app(app)
