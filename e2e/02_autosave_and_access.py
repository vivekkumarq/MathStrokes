import json, subprocess, urllib.request, urllib.error, os

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
                          '-t', '-A', '-F', '|', '-c', query],
                         capture_output=True, text=True, env=env)
    return [l for l in out.stdout.strip().split('\n') if l]

failures = []
def check(label, cond, detail=''):
    print(('  PASS  ' if cond else '  FAIL  ') + label + (('  -> ' + str(detail)) if detail else ''))
    if not cond:
        failures.append(label)

_, auth = call('POST', '/auth/login', body={'phoneNumber': '9812345670', 'password': 'Student@2026'})
TOK = auth['accessToken']
_, attempt = call('GET', '/attempts/active', TOK)
aid = attempt['attemptId']
questions = attempt['questions']

# The answer key, read straight from the attempt SNAPSHOT (not the live question bank).
key = {}
for row in sql("SELECT attempt_question_id, id FROM attempt_question_options "
               "WHERE is_correct = true AND attempt_question_id IN "
               "(SELECT id FROM attempt_questions WHERE attempt_id = %d) ORDER BY 1" % aid):
    qid, oid = row.split('|')
    key.setdefault(int(qid), []).append(int(oid))

print('ANSWERING: 15 correct, 5 wrong, 5 left blank')
seq = 0
for i, q in enumerate(questions):
    qid = q['attemptQuestionId']
    correct = key[qid]
    seq += 1
    if i < 15:
        selected = correct[:1]
    elif i < 20:
        wrong = [o['id'] for o in q['options'] if o['id'] not in correct]
        selected = wrong[:1]
    else:
        continue  # left unanswered
    status, res = call('PUT', '/attempts/%d/answers' % aid, TOK,
                       {'attemptQuestionId': qid, 'selectedOptionIds': selected,
                        'clientSequence': seq})
    assert status == 200, (status, res)

print('\nAUTOSAVE GUARANTEES')
q0 = questions[0]['attemptQuestionId']
_, palette = call('PUT', '/attempts/%d/answers' % aid, TOK,
                  {'attemptQuestionId': q0, 'selectedOptionIds': key[q0][:1],
                   'markedForReview': True, 'clientSequence': 500})
check('marked-for-review state persists', palette['answerStatus'] == 'ANSWERED_AND_MARKED_FOR_REVIEW',
      palette['answerStatus'])
check('palette returned with the ack', len(palette['palette']) == 25, len(palette['palette']))
check('server clock returned with the ack', palette['timing']['remainingSeconds'] > 0)

# A late packet carrying an older sequence must not overwrite newer work.
_, stale = call('PUT', '/attempts/%d/answers' % aid, TOK,
                {'attemptQuestionId': q0, 'selectedOptionIds': [], 'clientSequence': 9})
check('stale autosave rejected', stale['accepted'] is False, stale['accepted'])
check('stale write did not clear the answer', len(stale['selectedOptionIds']) == 1,
      stale['selectedOptionIds'])

# Clearing an answer with a current sequence must work.
_, cleared = call('PUT', '/attempts/%d/answers' % aid, TOK,
                  {'attemptQuestionId': q0, 'selectedOptionIds': [], 'clientSequence': 501})
check('clear answer accepted', cleared['accepted'] and not cleared['selectedOptionIds'])
# put it back so the score is predictable
call('PUT', '/attempts/%d/answers' % aid, TOK,
     {'attemptQuestionId': q0, 'selectedOptionIds': key[q0][:1], 'markedForReview': False,
      'clientSequence': 502})

print('\nINPUT VALIDATION')
status, res = call('PUT', '/attempts/%d/answers' % aid, TOK,
                   {'attemptQuestionId': q0, 'selectedOptionIds': [999999], 'clientSequence': 600})
check('option from another question rejected', status == 409, status)
status, res = call('PUT', '/attempts/%d/answers' % aid, TOK,
                   {'attemptQuestionId': q0,
                    'selectedOptionIds': [o['id'] for o in questions[0]['options'][:2]],
                    'clientSequence': 601})
check('two options on a single-correct question rejected', status == 409, res['message'] if res else '')

print('\nCROSS-STUDENT ACCESS')
call('POST', '/auth/student/register', body={
    'fullName': 'Rohan Verma', 'phoneNumber': '9812345699', 'password': 'Student@2026',
    'confirmPassword': 'Student@2026', 'securityQuestion': 'In which city were you born?',
    'securityAnswer': 'Mumbai'})
_, other = call('POST', '/auth/login', body={'phoneNumber': '9812345699', 'password': 'Student@2026'})
OTOK = other['accessToken']
status, res = call('GET', '/attempts/%d' % aid, OTOK)
check("another student cannot read the attempt", status == 403, status)
status, res = call('PUT', '/attempts/%d/answers' % aid, OTOK,
                   {'attemptQuestionId': q0, 'selectedOptionIds': [], 'clientSequence': 1})
check("another student cannot write to the attempt", status == 403, status)
status, res = call('POST', '/attempts/%d/submit' % aid, OTOK)
check("another student cannot submit the attempt", status == 403, status)

json.dump({'attemptId': aid, 'token': TOK}, open(STATE_FILE, 'w'))
print('\n' + ('ALL PASSED' if not failures else 'FAILURES: ' + str(failures)))
