import json, urllib.request, urllib.error, sys

import os

API = os.environ.get('MATHSTROKES_API', 'http://localhost:8080/api')
DB_NAME = os.environ.get('MATHSTROKES_DB', 'mathstrokes')
DB_USER = os.environ.get('MATHSTROKES_DB_USER', 'mathstrokes')
DB_PASSWORD = os.environ.get('MATHSTROKES_DB_PASSWORD', 'mathstrokes')
PSQL = os.environ.get('PSQL_PATH', r'C:\Program Files\PostgreSQLin\psql.exe')
STATE_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), '.state.json')



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

def ok(label, cond, detail=''):
    print(('  PASS  ' if cond else '  FAIL  ') + label + (('  -> ' + str(detail)) if detail else ''))
    return cond

failures = []
def check(label, cond, detail=''):
    if not ok(label, cond, detail):
        failures.append(label)

# ---- the demo student. Registering is idempotent for our purposes: on a database where the
# ---- account already exists the call fails harmlessly and we simply sign in.
call('POST', '/auth/student/register', body={
    'fullName': 'Ananya Sharma', 'phoneNumber': '9812345670', 'password': 'Student@2026',
    'confirmPassword': 'Student@2026',
    'securityQuestion': 'In which city were you born?', 'securityAnswer': 'Delhi'})
status, auth = call('POST', '/auth/login',
                    body={'phoneNumber': '9812345670', 'password': 'Student@2026'})
if status != 200:
    raise SystemExit('could not sign in as the demo student: %s %s' % (status, auth))
TOK = auth['accessToken']
print('signed in as', auth['user']['fullName'])

# ---- browse tests
status, tests = call('GET', '/tests', TOK)
print('\nAVAILABLE TESTS')
for t in tests:
    print('   #{} {}  [{}]  {}q/{}min  canStart={}'.format(
        t['id'], t['title'], t['examPattern'], t['questionCount'],
        t['durationMinutes'], t['canStart']))
main_test = next(t for t in tests if t['examPattern'] == 'JEE_MAIN')

# ---- start the attempt
print('\nSTART ATTEMPT')
status, attempt = call('POST', '/attempts', TOK, {'testId': main_test['id']})
check('attempt starts', status == 200, status)
aid = attempt['attemptId']
check('exactly 25 questions', len(attempt['questions']) == 25, len(attempt['questions']))
check('total matches', attempt['totalQuestions'] == 25)
check('60 minute duration', attempt['durationMinutes'] == 60)
t = attempt['timing']
check('server clock returned', 'serverTime' in t and 'expiresAt' in t, t['serverTime'])
check('remainingSeconds near 3600', 3590 <= t['remainingSeconds'] <= 3600, t['remainingSeconds'])
check('not expired', t['expired'] is False)

# ---- THE security check: no answer key anywhere in a live attempt
blob = json.dumps(attempt)
check('no isCorrect leaked', 'isCorrect' not in blob and '"correct"' not in blob)
check('no solution leaked', 'solution' not in blob.lower())
q1 = attempt['questions'][0]
check('question carries LaTeX', '$' in q1['questionContent'], q1['questionContent'][:60])
check('options have no correctness field',
      all(set(o) == {'id', 'optionKey', 'content', 'displayOrder'} for o in q1['options']),
      list(q1['options'][0]))
check('questionType drives the control', q1['questionType'] == 'SINGLE_CORRECT', q1['questionType'])

# ---- refresh must not redraw the paper or restart the clock
print('\nREFRESH BEHAVIOUR')
status, again = call('POST', '/attempts', TOK, {'testId': main_test['id']})
check('resumes the same attempt', again['attemptId'] == aid, again['attemptId'])
first_ids = [q['attemptQuestionId'] for q in attempt['questions']]
again_ids = [q['attemptQuestionId'] for q in again['questions']]
check('identical questions in identical order', first_ids == again_ids)
check('timer did not reset', again['timing']['remainingSeconds'] <= t['remainingSeconds'],
      again['timing']['remainingSeconds'])

print()
print('ALL PASSED' if not failures else 'FAILURES: ' + str(failures))
