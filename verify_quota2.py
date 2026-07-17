import json, urllib.request

GW = 'http://localhost:8080'
SCH = 'http://[::1]:8102'

def call(url, data=None, token=None, method='GET'):
    req = urllib.request.Request(url,
            data=json.dumps(data).encode() if data else None,
            method=method, headers={'Content-Type': 'application/json'})
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.loads(r.read().decode('utf-8'))

tok = call(GW + '/api/auth/login', {'username': 'admin', 'password': 'admin123'}, method='POST')['token']

jr = call(SCH + '/school/junior-schools?size=100')
id2name = {s['id']: s['name'] for s in jr['content']}
allq = call(SCH + '/school/quota-seats?size=2000')
total = sum(r['quota'] for r in allq['content'])
print('DB_rows', len(allq['content']), 'DB_total', total)

members = ['北京市第五中学分校', '北京市第二十二中学', '北京汇文中学',
           '北京市东直门中学', '北京市龙潭中学']
for m in members:
    rows = [r for r in allq['content'] if id2name.get(r['juniorSchoolId']) == m]
    print(m, 'rows', len(rows), 'sum', sum(r['quota'] for r in rows))
