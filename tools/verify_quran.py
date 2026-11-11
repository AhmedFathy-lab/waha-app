#!/usr/bin/env python3
"""تحقق مستقل: نص قاعدة quran.db يطابق ARQURAN.TXT كلمة بكلمة (بعد حذف علامات الترقيم والأرقام فقط)."""
import re, sqlite3, sys
SRC, DB = sys.argv[1], sys.argv[2]
raw = open(SRC, encoding='utf-8', newline='').read().split('*EOF*')[0].replace('\r\n', '\n').replace('\r', '\n')
L = raw.split('\n')
a = next(i for i, l in enumerate(L) if l.startswith('*1*') and 'الجزء الأول' in l)
b = next(i for i, l in enumerate(L) if l.startswith('*1*') and 'نهاية المصحف' in l)
src = [l[1:] if l.startswith('@') else l for l in L[a:b] if not l.startswith('*')]
src_words = re.sub(r'[\{\(]\s*\d+\s*[\}\)]|\{\s*س\s*\}', ' ', '\n'.join(src)).replace('.', ' ').split()
db = sqlite3.connect(DB)
out = []
for sid, basm in db.execute('SELECT id, basmala FROM surah ORDER BY id'):
    if basm: out.append(basm)
    for (t,) in db.execute('SELECT text FROM ayah WHERE surah=? ORDER BY ayah', (sid,)): out.append(t)
db_words = ' '.join(out).split()
print('كلمات المصدر:', len(src_words), '| كلمات القاعدة:', len(db_words))
bad = next((i for i, (x, y) in enumerate(zip(src_words, db_words)) if x != y), None)
if bad is not None or len(src_words) != len(db_words):
    print('أول اختلاف عند الكلمة', bad, src_words[bad:bad+5] if bad is not None else '', db_words[bad:bad+5] if bad is not None else ''); sys.exit(1)
n = db.execute('SELECT COUNT(*) FROM ayah').fetchone()[0]
assert n == 6236 and db.execute('SELECT COUNT(DISTINCT surah) FROM ayah').fetchone()[0] == 114
assert db.execute("SELECT COUNT(*) FROM ayah WHERE text='' OR text LIKE '%{%' OR text LIKE '%}%'").fetchone()[0] == 0
assert db.execute('SELECT MIN(juz),MAX(juz) FROM ayah').fetchone() == (1, 30)
assert db.execute('SELECT sajda FROM ayah WHERE surah=7 AND ayah=206').fetchone()[0] == 1
print('✔ مطابقة تامة: 6236 آية، 114 سورة، 30 جزءاً')
