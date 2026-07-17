import pandas as pd, numpy as np, os, csv
from collections import defaultdict

src = r'd:/worktable/gqm2026/2026-1.xlsx'
xiao = pd.read_excel(src, sheet_name='xiao', header=None)
d = os.path.dirname(__file__)

def num(v):
    if pd.isna(v) or v in ('-', '—'):
        return 0
    try:
        return int(float(v))
    except Exception:
        return 0

# official per-high-school totals (东城区校额到校 sheet, sum=1240)
official = {
    '北京市第二中学': 63, '北京市第五中学': 45, '北京市第十一中学': 133,
    '北京市第五十中学': 150, '北京市第五十五中学': 150, '北京市第一零九中学': 141,
    '北京市第一六六中学': 122, '北京市第一七一中学': 60, '北京市广渠门中学': 45,
    '北京汇文中学': 55, '北京市东直门中学': 65, '北京景山学校': 40,
    '北京市第二十二中学': 141, '北京市龙潭中学': 30,
}

# junior name normalization (strip "CODE " prefix)
def js(v):
    s = str(v).split(' ', 1)
    return s[-1].strip()

# junior name mapping to match junior_school.csv
JS_MAP = {
    '北京汇文中学朝阳学校': '汇文实验朝阳学校',
}

# parse raw matrix: {high: {junior: raw}}
raw = defaultdict(dict)
hs_cols = {i: str(xiao.iloc[0, i]).split(' (')[0] for i in range(2, 16)}
for ri in range(1, xiao.shape[0]):
    jn = js(xiao.iloc[ri, 0])
    jn = JS_MAP.get(jn, jn)
    for ci, hsn in hs_cols.items():
        q = num(xiao.iloc[ri, ci])
        if q:
            raw[hsn][jn] = raw[hsn].get(jn, 0) + q

# rescale each high school column to official total (largest remainder)
records = []
for hsn, tot in official.items():
    dist = raw.get(hsn, {})
    rawsum = sum(dist.values())
    if rawsum == 0:
        continue
    # proportionate floats
    exact = {j: tot * v / rawsum for j, v in dist.items()}
    floor = {j: int(np.floor(e)) for j, e in exact.items()}
    rem = tot - sum(floor.values())
    # distribute remainder to largest fractional parts
    fracs = sorted(exact.keys(), key=lambda j: exact[j] - floor[j], reverse=True)
    for k in range(rem):
        floor[fracs[k]] += 1
    for j, q in floor.items():
        if q:
            records.append((j, hsn, q))

# verify totals
chk = defaultdict(int)
for j, h, q in records:
    chk[h] += q
print('Rescaled per-high totals vs official:')
ok = True
for h in official:
    flag = '' if chk[h] == official[h] else '  <-- MISMATCH'
    if chk[h] != official[h]:
        ok = False
    print(f'  {h}: rescaled={chk[h]} official={official[h]}{flag}')
print('ALL MATCH:', ok, '| total records:', len(records), '| grand total:', sum(chk.values()))

# write real (rescaled) detail
out_real = os.path.join(d, 'quota_seat_real.csv')
with open(out_real, 'w', newline='', encoding='utf-8-sig') as f:
    w = csv.writer(f)
    w.writerow(['junior_school', 'high_school', 'quota'])
    for j, h, q in sorted(records, key=lambda r: (r[1], -r[2])):
        w.writerow([j, h, q])
print('WROTE', out_real)

# also write raw (as-is) for reference
out_raw = os.path.join(d, 'quota_seat_raw.csv')
with open(out_raw, 'w', newline='', encoding='utf-8-sig') as f:
    w = csv.writer(f)
    w.writerow(['junior_school', 'high_school', 'quota'])
    for hsn, dist in raw.items():
        for j, q in dist.items():
            w.writerow([j, hsn, q])
print('WROTE', out_raw)
