import pandas as pd, os
base = r'd:/worktable/gqm2026'
d = base + r'/backend/data'
os.makedirs(d, exist_ok=True)
xl = pd.ExcelFile(base + r'/2026-1.xlsx')

# 2026 一分一段 (无表头)
raw2026 = pd.read_excel(xl, '2026', header=None)
raw2026.columns = ['分数', '本段人数', '累计人数']
# 2025 一分一段 (有表头)
raw2025 = pd.read_excel(xl, '2025', header=0)
# 校额
quota = pd.read_excel(xl, '东城区校额到校', header=0)
# 分数线(各班型)
lines = pd.read_excel(xl, 'Sheet2', header=0)
# 2023 初中校班数
jr = pd.read_excel(xl, '2023', header=0)

raw2026.to_csv(d + r'/score_segment_2026.csv', index=False)
raw2025.to_csv(d + r'/score_segment_2025.csv', index=False)
quota.to_csv(d + r'/quota_raw.csv', index=False)
lines.to_csv(d + r'/score_line_2025_raw.csv', index=False)

# 2023 -> 初中校
jr = jr.dropna(subset=['学校名称'])
jr['班数'] = jr['入学结果（班）'].fillna(jr['招生计划（班）']).astype(int)
jr['毕业生数'] = jr['班数'] * 40
jr_out = jr[['学校名称', '班数', '毕业生数']].copy()
jr_out.columns = ['初中校', '班数', '毕业生数']
jr_out.to_csv(d + r'/junior_school.csv', index=False)

# 统计
seg = raw2026.copy()
seg['分'] = seg['分数'].astype(str).str.replace('分及以上', '').str.replace('分以下', '').astype(int)
m = (seg['分'] >= 430) & (seg['分'] <= 510)
cand = int(seg.loc[m, '本段人数'].sum())
tot_jr = int(jr_out['毕业生数'].sum())

print('2026 430-510 考生(本段合计)=', cand)
print('2026 全段累计最高=', int(seg['累计人数'].iloc[0]))
print('初中校数=', len(jr_out), ' 班数合计=', int(jr_out['班数'].sum()), ' 毕业生合计=', tot_jr)
print('--- 初中校清单 ---')
for _, r in jr_out.iterrows():
    print(f"{r['初中校']:24s} 班={int(r['班数']):>3} 毕业={int(r['毕业生数']):>4}")
print('校额高中数=', len(quota), '校额总=', int(quota['数量'].sum()))
print('分数线学校数=', lines['学校'].nunique())
