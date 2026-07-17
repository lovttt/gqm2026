import sqlite3
from collections import defaultdict, Counter

SCHOOL_DB = r"backend/school-service/data/school.db"
STUDENT_DB = r"backend/student-service/data/student.db"

GROUPS = [
    ["北京市东直门中学", "北京市第一六五中学"],
    ["北京市广渠门中学", "北京市龙潭中学"],
    ["北京汇文实验中学", "北京汇文中学"],
    ["北京市第二十一中学", "北京市第二十二中学"],
]

sc = sqlite3.connect(SCHOOL_DB)
cur = sc.cursor()
js = {name: jid for (jid, name) in cur.execute("SELECT id,name FROM junior_school")}
group_key = {}
group_members = defaultdict(list)
for g in GROUPS:
    ids = [js[n] for n in g if n in js]
    if not ids:
        continue
    canon = ids[0]
    for i in ids:
        group_key[i] = canon
        group_members[canon].append(i)

quota_rows = list(cur.execute("SELECT junior_school_id, high_school_id, quota FROM quota_seat"))
control = cur.execute("SELECT value FROM control_line WHERE type='QUOTA'").fetchone()
control = control[0] if control else 0
sc.close()

st = sqlite3.connect(STUDENT_DB)
cur = st.cursor()
cols = [r[1] for r in cur.execute("PRAGMA table_info(student)")]
acols = [r[1] for r in cur.execute("PRAGMA table_info(application)")]
students = [dict(zip(cols, row)) for row in cur.execute("SELECT * FROM student")]
st.close()

def tie_key(s):
    return (-s["total_score"], -(s["chinese"]+s["math"]+s["english"]), -s["chinese"],
            -s["math"], -s["english"], -(s["physics"]+s["politics"]), -s["physics"],
            -s["politics"], -s["pe"], str(s["ticket_no"]))

# 原始（非分组）每个初中校的对口高中
own_hs = defaultdict(set)
for jsid, hsid, q in quota_rows:
    if q > 0:
        own_hs[jsid].add(hsid)

def simulate(grouped_apps, grouped_pool):
    # 生成 QUOTA 志愿
    apps = defaultdict(list)  # student_id -> [hs_id,...]
    for s in students:
        if not s.get("has_quota_eligibility") or s["total_score"] < control:
            continue
        jsid = s["junior_school_id"]
        if grouped_apps:
            gk = group_key.get(jsid, jsid)
            hs_set = set()
            for m in group_members.get(gk, [jsid]):
                hs_set |= own_hs.get(m, set())
        else:
            hs_set = own_hs.get(jsid, set())
        for hs in hs_set:
            apps[s["id"]].append(hs)
    # 名额池
    pool = defaultdict(int)
    for jsid, hsid, q in quota_rows:
        key = (group_key.get(jsid, jsid) if grouped_pool else jsid, hsid)
        pool[key] += q
    # 候选分组
    cand = defaultdict(list)
    for s in students:
        if s["id"] not in apps:
            continue
        gk = group_key.get(s["junior_school_id"], s["junior_school_id"])
        for hs in apps[s["id"]]:
            key = (gk if grouped_pool else s["junior_school_id"], hs)
            if key in pool:
                cand[key].append(s)
    admitted = set()
    detail = []
    for key, cs in cand.items():
        cs.sort(key=tie_key)
        q = pool[key]
        rank = 0
        for s in cs[:q]:
            if s["id"] in admitted:
                continue
            admitted.add(s["id"])
            rank += 1
            detail.append((s["junior_school_id"], key[1], rank))
    return detail

old = simulate(False, False)
new = simulate(True, True)

def per_school(detail):
    c = Counter()
    for jsid, hsid, rank in detail:
        c[jsid] += 1
    return c

oc, nc = per_school(old), per_school(new)
watch = {js[n]: n for g in GROUPS for n in g if n in js}

print(f"控制线(QUOTA)={control}")
print(f"总校额录取: 分组前={len(old)}  分组后={len(new)}")
print("\n分组成员校 校额录取（前 -> 后）:")
for jid, name in sorted(watch.items(), key=lambda x: x[1]):
    print(f"  {name:20} {oc.get(jid,0):3} -> {nc.get(jid,0):3}")

# 东直门专项
djm = js.get("北京市东直门中学")
hs165 = js.get("北京市第一六五中学")
print(f"\n东直门中学: 前={oc.get(djm,0)}  后={nc.get(djm,0)}")
print(f"第一六五中学: 前={oc.get(hs165,0)}  后={nc.get(hs165,0)}")
print("\n说明: 东直门初中在数据中无名额行(前=0)，分组后其学生可用165的名额池，"
      "故'后'>0 即证明共享池在'生成志愿+录取'两端均已打通。")
