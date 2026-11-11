#!/usr/bin/env python3
"""يحوّل ARQURAN.TXT إلى quran.db (SQLite) داخل assets. يتوقف عند أي عدم تطابق مع العدد المرجعي."""
import os, re, sqlite3, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from quran_meta import SURAH_LIST, SAJDA

SRC, OUT = sys.argv[1], sys.argv[2]
END_RE = re.compile(r'[\{\(]\s*(\d+)\s*[\}\)]')
SAJDA_RE = re.compile(r'\{\s*س\s*\}(?:\s*\.)?')
JUZ_NAMES = "الأول الثاني الثالث الرابع الخامس السادس السابع الثامن التاسع العاشر".split()
DIAC = re.compile('[ؐ-ًؚ-ٰٟۖ-ۭـ]')

def norm(s):
    s = DIAC.sub('', s)
    for a in 'أإآٱ': s = s.replace(a, 'ا')
    return s.replace('ى', 'ي').replace('ة', 'ه')

def surah_index(header):
    name = re.sub(r'[\d.]+|سورة', ' ', header[3:]).strip()
    name = ' '.join(name.split())
    for i, (n, _) in enumerate(SURAH_LIST):
        if norm(n) == norm(name): return i + 1
    raise SystemExit(f'سورة غير معروفة: {header!r}')

raw = open(SRC, encoding='utf-8', newline='').read().split('*EOF*')[0]
lines = raw.replace('\r\n', '\n').replace('\r', '\n').split('\n')
start = next(i for i, l in enumerate(lines) if l.startswith('*1*') and 'الجزء الأول' in l)
end = next(i for i, l in enumerate(lines) if l.startswith('*1*') and 'نهاية المصحف' in l)

juz, page, surah = 0, 0, 0
juz_pending = None
buf, buf_juz, buf_page = [], 0, 0
ayat, anomalies, sajda_found, basmalas = [], [], set(), {}
expected_next = 1
BASM = norm('بسم الله الرحمن الرحيم')

def close_ayah(label):
    global buf, expected_next
    text = ' '.join(' '.join(buf).split())
    buf = []
    if SAJDA_RE.search(text): sajda_found.add((surah, expected_next))
    if surah != 1 and expected_next == 1 and norm(text).startswith(BASM):
        # البسملة غير مرقمة في أول السور (ما عدا الفاتحة): تُحفظ منفصلة
        basm_len = len(text.split()) and 4
        words = text.split()
        basmalas[surah] = ' '.join(words[:4]); text = ' '.join(words[4:])
    if label != expected_next:
        anomalies.append((surah, expected_next, label))
    ayat.append(dict(surah=surah, ayah=expected_next, text=text, juz=bj[0], page=bj[1]))
    expected_next += 1

# إصلاحات بنيوية موثقة: علامة نهاية آية ناقصة في المصدر (لا يتغير أي حرف من النص، تُضاف العلامة فقط).
MARKER_FIXES = [(38, 'مآب واذكر عبدنا ايوب')]  # (السورة، بداية مطابقة بعد التطبيع): يوضع {40} بعد أول كلمة
fixes_applied = 0
body = []
cur = 0
for l in lines[start:end]:
    if l.startswith('*2*'): cur = surah_index(l)
    for sn, key in MARKER_FIXES:
        if cur == sn and norm(l).strip().startswith(norm(key)):
            w = l.split(' ', 1); l = w[0] + ' {40} ' + w[1]; fixes_applied += 1
    body.append(l)
assert fixes_applied == len(MARKER_FIXES), fixes_applied
print('إصلاحات علامات مطبقة:', fixes_applied)

bj = [0, 0]
for l in body:
    if l.startswith('*1*'):
        juz_pending = (juz_pending or 0) + 1
        if not buf: juz = juz_pending
        continue
    if l.startswith('*2*'):
        idx = surah_index(l)
        if idx != surah:
            if buf: raise SystemExit(f'نص معلّق قبل سورة {idx}: {buf}')
            if idx != surah + 1: raise SystemExit(f'قفزة في السور: {surah}->{idx}')
            surah, expected_next = idx, 1
        continue
    if l.startswith('*3*'):
        m = re.search(r'ص\s+(\d+)', l)
        if not m: raise SystemExit(f'رأس صفحة غير مفهوم: {l!r}')
        page = int(m.group(1)); continue
    if l.startswith('@'): l = l[1:]
    if not l.strip(): continue
    # تعيين جزء/صفحة الآية عند أول نص فيها
    if not buf: bj = [juz_pending if juz_pending else juz, page]
    pos = 0
    for m in END_RE.finditer(l):
        piece = l[pos:m.start()]
        if piece.strip(): buf.append(piece)
        sm = SAJDA_RE.search(piece)
        label = int(m.group(1))
        close_ayah(label); pos = m.end()
        if pos < len(l):
            # علامة سجدة تلي الآية مباشرة
            sj = SAJDA_RE.match(l[pos:].lstrip())
            if sj:
                sajda_found.add((surah, expected_next - 1)); pos = l.index(sj.group(0), pos) + len(sj.group(0))
        if buf == [] and pos < len(l) and l[pos:].strip(): bj = [juz_pending or juz, page]
    rest = l[pos:]
    if rest.strip(' .\t'): buf.append(rest)
if buf: raise SystemExit(f'نص بلا نهاية آية: {buf}')

# إزالة أي بقايا لعلامة السجدة من النصوص
for a in ayat: a['text'] = ' '.join(SAJDA_RE.sub(' ', a['text']).split())

# تحقق
counts = {}
for a in ayat: counts[a['surah']] = counts.get(a['surah'], 0) + 1
errs = [(i + 1, n, counts.get(i + 1, 0)) for i, (_, n) in enumerate(SURAH_LIST) if counts.get(i + 1, 0) != n]
print('عدد الآيات:', len(ayat), '| الشذوذات في الترقيم المصدري:', len(anomalies), '| سور بعدد مختلف:', errs)
print('السجدات المكتشفة:', len(sajda_found), '| مطابقة المرجع:', sajda_found == SAJDA, sorted(sajda_found ^ SAJDA))
print([x for x in anomalies if x[0]==38][:12])
if errs or len(ayat) != 6236: raise SystemExit('فشل التحقق من عدد الآيات')

# ملاحظة: juz_pending يُحدَّث عند كل *1*؛ تأكد أن 30 جزءاً
assert max(a['juz'] for a in ayat) == 30, max(a['juz'] for a in ayat)

import os
if os.path.exists(OUT): os.remove(OUT)
db = sqlite3.connect(OUT)
db.executescript('''
CREATE TABLE surah(id INTEGER PRIMARY KEY, name TEXT NOT NULL, ayah_count INTEGER NOT NULL, basmala TEXT, first_page INTEGER NOT NULL, first_juz INTEGER NOT NULL);
CREATE TABLE ayah(id INTEGER PRIMARY KEY, surah INTEGER NOT NULL, ayah INTEGER NOT NULL, text TEXT NOT NULL, text_norm TEXT NOT NULL, juz INTEGER NOT NULL, page INTEGER NOT NULL, sajda INTEGER NOT NULL);
CREATE UNIQUE INDEX idx_ayah_sa ON ayah(surah, ayah);
CREATE INDEX idx_ayah_page ON ayah(page);
''')
for i, (n, c) in enumerate(SURAH_LIST, 1):
    first = next(a for a in ayat if a['surah'] == i)
    db.execute('INSERT INTO surah VALUES(?,?,?,?,?,?)', (i, n, c, basmalas.get(i), first['page'], first['juz']))
for gid, a in enumerate(ayat, 1):
    db.execute('INSERT INTO ayah VALUES(?,?,?,?,?,?,?,?)',
      (gid, a['surah'], a['ayah'], a['text'], norm(a['text']), a['juz'], a['page'], int((a['surah'], a['ayah']) in sajda_found)))
db.commit(); db.close()
print('تم إنشاء', OUT)
