import json, os, urllib.request, urllib.error
from datetime import datetime, timedelta, timezone

API = os.environ.get('MATHSTROKES_API', 'http://localhost:8080/api')

# A question written for a class test is published the moment it is saved, so it must stay out
# of practice papers until the class has sat it - and rejoin the pool afterwards.
# Uses Determinants (chapter 8), which no other script touches.


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

status, auth = call('POST', '/auth/login', body={'phoneNumber': '9000000001', 'password': 'Admin@2026'})
if status != 200:
    raise SystemExit('could not sign in as the seeded admin: %s %s' % (status, auth))
TOK = auth['accessToken']
CHAPTER = 8

def bank_size():
    _, page = call('GET', '/admin/questions?chapterId=%d&examPattern=JEE_MAIN&difficulty=MEDIUM'
                          '&status=PUBLISHED&size=1' % CHAPTER, TOK)
    return page['totalElements']

def practice_paper(count):
    """Creates and publishes a practice test asking for `count` medium questions."""
    _, test = call('POST', '/admin/tests', TOK, {
        'title': 'Pool probe - %d medium' % count, 'chapterId': CHAPTER, 'examPattern': 'JEE_MAIN',
        'generationMode': 'FIXED_SET', 'testKind': 'PRACTICE', 'durationMinutes': 30,
        'questionCount': count, 'maxAttemptsPerStudent': 1,
        'easyCount': 0, 'mediumCount': count, 'hardCount': 0})
    status, _ = call('POST', '/admin/tests/%d/publish' % test['id'], TOK)
    _, paper = call('GET', '/admin/tests/%d/questions' % test['id'], TOK)
    return status, [q['id'] for q in (paper or [])]

print('\nA CLASS-TEST QUESTION, WRITTEN TODAY')
_, q = call('POST', '/admin/questions', TOK, {
    'chapterId': CHAPTER, 'examPattern': 'JEE_MAIN', 'difficulty': 'MEDIUM',
    'questionType': 'SINGLE_CORRECT', 'questionContent': 'Pool probe: find $\\det I_2$.',
    'options': [{'optionKey': k, 'content': '$%s$' % v, 'displayOrder': i + 1, 'isCorrect': v == '1'}
                for i, (k, v) in enumerate(zip('ABCD', ['0', '1', '2', '4']))]})
status, _ = call('POST', '/admin/questions/%d/publish' % q['id'], TOK)
check('question published', status == 200, status)
bank = bank_size()

now = datetime.now(timezone.utc)
_, ct = call('POST', '/admin/tests', TOK, {
    'title': 'Pool probe class test', 'examPattern': 'JEE_MAIN', 'generationMode': 'FIXED_SET',
    'testKind': 'CLASS_TEST', 'durationMinutes': 30, 'questionCount': 1, 'maxAttemptsPerStudent': 1,
    'scheduledStartAt': (now - timedelta(hours=1)).isoformat(),
    'scheduledEndAt': (now + timedelta(hours=2)).isoformat()})
status, _ = call('PUT', '/admin/tests/%d/questions' % ct['id'], TOK, {'questionIds': [q['id']]})
check('question attached to the class test', status == 200, status)
status, _ = call('POST', '/admin/tests/%d/publish' % ct['id'], TOK)
check('class test live, window still open', status == 200, status)

print('\nWHILE THE CLASS TEST IS OPEN')
status, _ = practice_paper(bank)
check('a paper needing every medium question is refused', status == 409, status)
status, ids = practice_paper(bank - 1)
check('a paper one smaller is published', status == 200, status)
check('...and leaves the class-test question out', q['id'] not in ids, ids)

print('\nAFTER THE CLASS TEST IS CLOSED')
status, _ = call('POST', '/admin/tests/%d/close' % ct['id'], TOK)
check('class test closed', status == 200, status)
status, ids = practice_paper(bank)
check('the question rejoins the pool', status == 200 and q['id'] in ids, (status, q['id'] in ids))

print('\n' + ('ALL PASSED' if not failures else 'FAILURES: ' + str(failures)))
