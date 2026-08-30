import json, os, subprocess, urllib.request, urllib.error

import os

API = os.environ.get('MATHSTROKES_API', 'http://localhost:8080/api')
DB_NAME = os.environ.get('MATHSTROKES_DB', 'mathstrokes')
DB_USER = os.environ.get('MATHSTROKES_DB_USER', 'mathstrokes')
DB_PASSWORD = os.environ.get('MATHSTROKES_DB_PASSWORD', 'mathstrokes')
PSQL = os.environ.get('PSQL_PATH', r'C:\Program Files\PostgreSQLin\psql.exe')
STATE_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), '.state.json')


PSQL = r'C:\Program Files\PostgreSQL\17\bin\psql.exe'

def call(method, path, token=None, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(API + path, data=data, method=method)
    req.add_header('Content-Type', 'application/json')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    try:
        with urllib.request.urlopen(req) as r:
            raw = r.read().decode()
            return r.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        return e.code, (json.loads(raw) if raw else None)

def sql(query):
    env = dict(os.environ, PGPASSWORD=DB_PASSWORD)
    out = subprocess.run([PSQL, '-U', DB_USER, '-h', 'localhost', '-d', DB_NAME,
                          '-t', '-A', '-F', '|', '-c', query], capture_output=True, text=True, env=env)
    return [l for l in out.stdout.strip().split('\n') if l]

failures = []
def check(label, cond, detail=''):
    print(('  PASS  ' if cond else '  FAIL  ') + label + (('  -> ' + str(detail)) if detail else ''))
    if not cond:
        failures.append(label)

print('RANK REFRESHES FOR STUDENTS WHO FINISHED EARLIER')
_, priya = call('POST', '/auth/login', body={'phoneNumber': '9822000001', 'password': 'Student@2026'})
ptok = priya['accessToken']
_, hist = call('GET', '/attempts/history', ptok)
aid = next(a['attemptId'] for a in hist['content'] if a['status'] == 'EVALUATED')
_, res = call('GET', '/attempts/%d/result' % aid, ptok)
# She was rank 1 of 1 when she submitted; three more students have finished since.
# She and one other student tie on score, correct and incorrect counts, so which of them takes
# rank 2 comes down to completion time and legitimately varies between runs. Assert that she is
# one of the tied pair rather than pinning a position the tie-break is entitled to swap.
check('her stored rank moved down as others finished',
      res['rankPosition'] in (2, 3), res['rankPosition'])
check('cohort size updated to 4', res['totalCandidates'] == 4, res['totalCandidates'])
check('percentile recomputed to 75.00', float(res['percentile']) == 75.0, res['percentile'])
check('her score is untouched by other students finishing', float(res['score']) == 35.0,
      res['score'])

print('\nHISTORICAL INTEGRITY: editing a question must not move an old result')
_, admin = call('POST', '/auth/login', body={'phoneNumber': '9000000001', 'password': 'Admin@2026'})
atok = admin['accessToken']

_, student = call('POST', '/auth/login',
                  body={'phoneNumber': '9812345670', 'password': 'Student@2026'})
stok = student['accessToken']
_, hist = call('GET', '/attempts/history', stok)
# Pick a FINISHED attempt: another agent may have left an active one on this account.
old_aid = next(a['attemptId'] for a in hist['content'] if a['status'] == 'EVALUATED')
_, before = call('GET', '/attempts/%d/result' % old_aid, stok)
_, review_before = call('GET', '/attempts/%d/review' % old_aid, stok)

# Find a question this attempt actually used, and the correct option the student picked.
row = sql("SELECT aq.question_id, aq.question_version FROM attempt_questions aq "
          "WHERE aq.attempt_id = %d ORDER BY aq.question_order LIMIT 1" % old_aid)[0]
qid, snap_version = row.split('|')
qid = int(qid)
print('   editing question %s (snapshotted at version %s)' % (qid, snap_version))

_, original = call('GET', '/admin/questions/%d' % qid, atok)
orig_content = original['questionContent']

# Rewrite the stem AND flip the answer key onto a different option -- the most destructive
# edit a teacher could plausibly make to a question that has already been sat.
flipped = []
for i, o in enumerate(original['options']):
    flipped.append({'optionKey': o['optionKey'], 'content': o['content'],
                    'displayOrder': o['displayOrder'],
                    'isCorrect': (i == len(original['options']) - 1)})
status, edited = call('PUT', '/admin/questions/%d' % qid, atok, {
    'chapterId': original['chapterId'], 'examPattern': original['examPattern'],
    'difficulty': original['difficulty'], 'questionType': original['questionType'],
    'questionContent': 'COMPLETELY REWRITTEN STEM $x^2 = 1$',
    'solutionContent': 'Rewritten solution.',
    'markingSchemeId': original.get('markingSchemeId'), 'options': flipped})
check('the edit succeeded', status == 200, status)
check('question version incremented', edited['version'] > original['version'],
      '%s -> %s' % (original['version'], edited['version']))
check('the live question now has a different answer key',
      [o['isCorrect'] for o in edited['options']] != [o['isCorrect'] for o in original['options']])

_, after = call('GET', '/attempts/%d/result' % old_aid, stok)
check('score unchanged after the edit', after['score'] == before['score'],
      '%s -> %s' % (before['score'], after['score']))
check('correct count unchanged', after['correctCount'] == before['correctCount'])
check('incorrect count unchanged', after['incorrectCount'] == before['incorrectCount'])

_, review_after = call('GET', '/attempts/%d/review' % old_aid, stok)
q_before = review_before[0]
q_after = review_after[0]
check('the review still shows the ORIGINAL stem, not the rewrite',
      q_after['questionContent'] == q_before['questionContent']
      and 'REWRITTEN' not in q_after['questionContent'],
      q_after['questionContent'][:45])
check('the review still shows the ORIGINAL answer key',
      q_after['correctOptionIds'] == q_before['correctOptionIds'],
      '%s vs %s' % (q_before['correctOptionIds'], q_after['correctOptionIds']))
check('per-question marks unchanged', q_after['marksAwarded'] == q_before['marksAwarded'])

print('\nARCHIVING A USED QUESTION DOES NOT BREAK HISTORY')
status, _ = call('POST', '/admin/questions/%d/archive' % qid, atok)
check('archive succeeded', status == 200, status)
_, after_archive = call('GET', '/attempts/%d/result' % old_aid, stok)
check('score still unchanged after archiving', after_archive['score'] == before['score'],
      after_archive['score'])
_, review_archive = call('GET', '/attempts/%d/review' % old_aid, stok)
check('review still renders the archived question',
      review_archive[0]['questionContent'] == q_before['questionContent'])

print('\n' + ('ALL PASSED' if not failures else 'FAILURES: ' + str(failures)))
