#!/usr/bin/env python3
"""يحوّل hesn-utf8.txt (حصن المسلم) إلى adhkar.db. لا يعدّل النصوص؛ يستخرج العدد فقط من عبارة العدد المكتوبة داخل النص."""
import os, re, sqlite3, sys
SRC, OUT = sys.argv[1], sys.argv[2]

ONES = {'واحدة': 1, 'واحد': 1, 'مرتين': 2, 'اثنتين': 2, 'ثلاث': 3, 'ثلاثا': 3, 'ثلاثة': 3, 'أربع': 4, 'اربع': 4, 'أربعا': 4, 'اربعا': 4,
        'خمس': 5, 'ست': 6, 'سبع': 7, 'ثمان': 8, 'تسع': 9, 'عشر': 10, 'مائة': 100, 'مئة': 100}
TENS = {'ثلاثين': 30, 'أربعين': 40, 'اربعين': 40, 'خمسين': 50, 'ستين': 60, 'سبعين': 70}
DIAC = re.compile('[ً-ٰٟـ]')

def words(s): return [w for w in re.sub(r'[^ء-ي ]', ' ', DIAC.sub('', s)).replace('ً', '').split() if w]

def parse_group(g):
    """يحوّل نص بين قوسين مثل «ثلاثاً وثلاثين» أو «مائة مرة» إلى عدد، أو None إن لم يكن عدداً."""
    if re.search(r'\bأو\b|إذا|عند|في اليوم|بعد|قبل', g.replace('ا', 'ا')) and not re.match(r'^\s*(ثلاث|مائة|عشر|سبع|أربع)', g):
        pass
    w = [x.replace('أ', 'ا').lstrip('و') if x.startswith('و') and len(x) > 4 else x.replace('أ', 'ا') for x in words(g)]
    if not w or 'مره' in w and len(w) == 1: pass
    n, found = 0, False
    for x in w:
        x2 = x
        for k, v in list(TENS.items()):
            if x2.replace('أ', 'ا') == k.replace('أ', 'ا'): n += v; found = True; break
        else:
            for k, v in ONES.items():
                if x2 == k.replace('أ', 'ا'): n += v; found = True; break
    return n if found else None

def extract_count(text):
    groups = re.findall(r'[\(（]([^()]*)[\)）]', text)
    vals = []
    for g in groups:
        if re.search(r'\bأو\b', g): continue          # بديل مشروط مثل «أو مرة واحدة عند الكسل»
        if not re.search(r'مر[ةات]|ثلاث|أربع|وثلاثين|مائة', DIAC.sub('', g)): continue
        v = parse_group(g)
        if v: vals.append(v)
    if vals: return sum(vals) if len(vals) == 3 and text.count('(') == 3 else vals[0]
    # عدد مكتوب خارج الأقواس في ذكر قصير ومحدد (مثل «ثلاث مرات .»)؛ النصوص الطويلة/المركبة تبقى 1 ويُعرض نصها كاملاً
    if len(text) <= 160:
        m = re.findall(r'(?<![\u0621-\u064A])(ثلاث|سبع|عشر|مائة|مئة|أربع)\s+مر[ةات]', DIAC.sub('', text))
        if len(m) == 1: return ONES[m[0]]
    return 1

rows = [l.split('|', 3) for l in open(SRC, encoding='utf-8').read().splitlines() if l.strip()]
cats, order = {}, []
for c, t, i, x in rows:
    if c not in cats: cats[c] = t.strip(); order.append(c)

# أبواب الأدعية: كل باب عنوانه دعاء/استغفار/توبة/استخارة؛ تُنقل من «متنوعة» إلى قسم الأدعية
DUA_KEYS = ('دعاء', 'الدعاء', 'استخارة', 'الاستغفار والتوبة')
def is_dua_cat(title): return any(k in title for k in DUA_KEYS)

def has(t, *ks): return any(k in t for k in ks)

db_rows = []   # (dhikr_id, cat, src_item, text, count)
sec = []       # (section, dhikr_id, ord)
for gid, (c, t, i, x) in enumerate(rows, 1):
    x = x.strip()
    db_rows.append((gid, int(c), int(i), x, extract_count(x)))
    ci = int(c)
    if ci == 1: sec.append(('waking', gid))
    elif ci == 25: sec.append(('after_prayer', gid))
    elif ci == 28: sec.append(('sleep', gid))
    elif ci == 27:
        m, e = has(x, 'أصبح'), has(x, 'أمسى', 'أمسين')
        if m and not e: sec.append(('morning', gid))
        elif e and not m: sec.append(('evening', gid))
        else: sec.append(('morning', gid)); sec.append(('evening', gid))
    elif is_dua_cat(cats[c]): sec.append(('duas', gid))
    else: sec.append(('misc', gid))

if os.path.exists(OUT): os.remove(OUT)
db = sqlite3.connect(OUT)
db.executescript('''
CREATE TABLE category(id INTEGER PRIMARY KEY, title TEXT NOT NULL);
CREATE TABLE dhikr(id INTEGER PRIMARY KEY, category_id INTEGER NOT NULL, src_item INTEGER NOT NULL, text TEXT NOT NULL, count INTEGER NOT NULL);
CREATE TABLE section_item(section TEXT NOT NULL, dhikr_id INTEGER NOT NULL, ord INTEGER NOT NULL);
CREATE INDEX idx_sec ON section_item(section, ord);
''')
db.executemany('INSERT INTO category VALUES(?,?)', [(int(c), cats[c]) for c in order])
db.executemany('INSERT INTO dhikr VALUES(?,?,?,?,?)', db_rows)
db.executemany('INSERT INTO section_item VALUES(?,?,?)', [(s, d, k) for k, (s, d) in enumerate(sec)])
db.commit()
print('أذكار:', len(db_rows), '| أبواب:', len(cats))
for s in ['morning', 'evening', 'sleep', 'waking', 'after_prayer', 'misc', 'duas']:
    print(s, sum(1 for a, _ in sec if a == s))
# تقرير الأعداد المستخرجة لمراجعتها
if len(sys.argv) > 3:
    for gid, c, i, x, n in db_rows:
        if '(' in x or 'مرات' in x or n > 1: print(gid, c, n, '|', x[-70:])
