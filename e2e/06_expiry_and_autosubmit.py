import json, os, subprocess, time, urllib.request, urllib.error

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

phone = '9833000001'
call('POST', '/auth/student/register', body={
    'fullName': 'Dev Malhotra', 'phoneNumber': phone, 'password': 'Student@2026',
    'confirmPassword': 'Student@2026',
    'securityQuestion': 'In which city were you born?', 'securityAnswer': 'Jaipur'})
_, auth = call('POST', '/auth/login', body={'phoneNumber': phone, 'password': 'Student@2026'})
tok = auth['accessToken']

_, att = call('POST', '/attempts', tok, {'testId': 1})
aid = att['attemptId']
print('started attempt', aid)

key = {}
for row in sql("SELECT attempt_question_id, id FROM attempt_question_options "
               "WHERE is_correct = true AND attempt_question_id IN "
               "(SELECT id FROM attempt_questions WHERE attempt_id = %d) ORDER BY 1" % aid):
    qid, oid = row.split('|')
    key.setdefault(int(qid), []).append(int(oid))

# Answer 8 correctly, then walk away without submitting.
for i, q in enumerate(att['questions'][:8]):
    call('PUT', '/attempts/%d/answers' % aid, tok,
         {'attemptQuestionId': q['attemptQuestionId'],
          'selectedOptionIds': key[q['attemptQuestionId']][:1], 'clientSequence': i + 1})
print('answered 8 questions, then abandoned the attempt')

print('\nTIME RUNS OUT  (deadline pushed into the past, as if the student walked away)')
sql("UPDATE test_attempts SET expires_at = now() - interval '10 seconds', "
    "started_at = now() - interval '61 minutes' WHERE id = %d" % aid)

status, res = call('PUT', '/attempts/%d/answers' % aid, tok,
                   {'attemptQuestionId': att['questions'][9]['attemptQuestionId'],
                    'selectedOptionIds': [], 'clientSequence': 99})
check('answers rejected once the deadline has passed', status == 409, status)
check('error code is ATTEMPT_EXPIRED', res['error'] == 'ATTEMPT_EXPIRED', res['error'])

status, active = call('GET', '/attempts/%d' % aid, tok)
if status == 200:
    check('timing reports the attempt as expired', active['timing']['expired'] is True,
          active['timing'])
    check('remaining seconds clamped at zero', active['timing']['remainingSeconds'] == 0,
          active['timing']['remainingSeconds'])

print('\nSCHEDULED SWEEP  (runs every 30s; waiting for it)')
deadline = time.time() + 75
final_status = None
while time.time() < deadline:
    rows = sql("SELECT status FROM test_attempts WHERE id = %d" % aid)
    final_status = rows[0] if rows else None
    if final_status == 'EVALUATED':
        break
    time.sleep(3)
check('the sweep finalised the abandoned attempt', final_status == 'EVALUATED', final_status)

_, result = call('GET', '/attempts/%d/result' % aid, tok)
check('recorded as AUTO_SUBMITTED, not a manual submit',
      sql("SELECT status FROM test_attempts WHERE id = %d" % aid)[0] == 'EVALUATED')
check('scored from the answers already saved: 8 x 4 = 32',
      float(result['score']) == 32.0, result['score'])
check('8 correct', result['correctCount'] == 8, result['correctCount'])
check('17 unanswered', result['unansweredCount'] == 17, result['unansweredCount'])
check('no negative marks, since nothing wrong was submitted',
      float(result['negativeMarks']) == 0.0, result['negativeMarks'])
check('time taken capped at the paper duration (3600s)',
      result['timeTakenSeconds'] <= 3600, result['timeTakenSeconds'])
check('the auto-submitted attempt is ranked', result['rankPosition'] is not None,
      result['rankPosition'])

print('\nTHE AUTO-SUBMIT / MANUAL-SUBMIT RACE')
status, raced = call('POST', '/attempts/%d/submit' % aid, tok)
check('a manual submit arriving after auto-submit returns the result, not a conflict',
      status == 200, status)
check('and the score is the auto-submitted one', float(raced['score']) == 32.0, raced['score'])

print('\n' + ('ALL PASSED' if not failures else 'FAILURES: ' + str(failures)))
