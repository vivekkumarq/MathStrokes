import json, os, subprocess, urllib.request, urllib.error

API = 'http://localhost:8080/api'
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
    env = dict(os.environ, PGPASSWORD='mathstrokes')
    out = subprocess.run([PSQL, '-U', 'mathstrokes', '-h', 'localhost', '-d', 'mathstrokes',
                          '-t', '-A', '-F', '|', '-c', query], capture_output=True, text=True, env=env)
    return [l for l in out.stdout.strip().split('\n') if l]

failures = []
def check(label, cond, detail=''):
    print(('  PASS  ' if cond else '  FAIL  ') + label + (('  -> ' + str(detail)) if detail else ''))
    if not cond:
        failures.append(label)

def sit_advanced_test(name, phone, n_full, n_partial, n_wrong):
    """Sits test 2 (JEE Advanced, multiple correct, partial marking)."""
    call('POST', '/auth/student/register', body={
        'fullName': name, 'phoneNumber': phone, 'password': 'Student@2026',
        'confirmPassword': 'Student@2026',
        'securityQuestion': 'In which city were you born?', 'securityAnswer': 'Pune'})
    _, auth = call('POST', '/auth/login', body={'phoneNumber': phone, 'password': 'Student@2026'})
    tok = auth['accessToken']
    _, att = call('POST', '/attempts', tok, {'testId': 2})
    aid = att['attemptId']

    key = {}
    for row in sql("SELECT attempt_question_id, id FROM attempt_question_options "
                   "WHERE is_correct = true AND attempt_question_id IN "
                   "(SELECT id FROM attempt_questions WHERE attempt_id = %d) ORDER BY 1, 2" % aid):
        qid, oid = row.split('|')
        key.setdefault(int(qid), []).append(int(oid))

    seq = 0
    for i, q in enumerate(att['questions']):
        qid = q['attemptQuestionId']
        correct = key[qid]
        seq += 1
        if i < n_full:
            selected = correct                      # exact key -> +4
        elif i < n_full + n_partial:
            selected = correct[:1]                  # one correct option -> +1
        elif i < n_full + n_partial + n_wrong:
            wrong = [o['id'] for o in q['options'] if o['id'] not in correct]
            selected = wrong[:1]                    # a wrong option -> -2
        else:
            continue
        call('PUT', '/attempts/%d/answers' % aid, tok,
             {'attemptQuestionId': qid, 'selectedOptionIds': selected, 'clientSequence': seq})
    _, result = call('POST', '/attempts/%d/submit' % aid, tok)
    return tok, result

print('JEE ADVANCED PARTIAL MARKING  (+4 exact, +1 per correct capped at +3, -2 any wrong)')
# 10 exact, 5 partial(one correct each), 5 wrong, 5 blank
# = 10*4 + 5*1 + 5*(-2) + 0 = 40 + 5 - 10 = 35
tok_a, res_a = sit_advanced_test('Priya Nair', '9822000001', 10, 5, 5)
check('partial marking arithmetic: 10x+4, 5x+1, 5x-2 = 35', float(res_a['score']) == 35.0,
      res_a['score'])
check('10 fully correct', res_a['correctCount'] == 10, res_a['correctCount'])
check('5 partially correct', res_a['partiallyCorrectCount'] == 5, res_a['partiallyCorrectCount'])
check('5 incorrect', res_a['incorrectCount'] == 5, res_a['incorrectCount'])
check('5 unanswered', res_a['unansweredCount'] == 5, res_a['unansweredCount'])
check('negative marks 10.00', float(res_a['negativeMarks']) == 10.0, res_a['negativeMarks'])
# accuracy counts only fully-correct over attempted: 10/20
check('accuracy 50.00 (partial counts as attempted, not correct)',
      float(res_a['accuracy']) == 50.0, res_a['accuracy'])

print('\nMULTI-STUDENT COHORT')
tok_b, res_b = sit_advanced_test('Arjun Rao', '9822000002', 20, 0, 0)   # 20*4 = 80
tok_c, res_c = sit_advanced_test('Meera Iyer', '9822000003', 10, 5, 5)  # 35, ties with Priya
tok_d, res_d = sit_advanced_test('Kabir Shah', '9822000004', 5, 0, 10)  # 5*4 - 10*2 = 0

for name, r in [('Arjun', res_b), ('Priya', res_a), ('Meera', res_c), ('Kabir', res_d)]:
    print('   {:8s} score={:>6}  rank={}  percentile={}'.format(
        name, str(r['score']), r['rankPosition'], r['percentile']))

_, board = call('GET', '/rankings/tests/2', tok_b)
print('\n   LEADERBOARD')
for e in board['entries']:
    print('     #{}  {:12s} score={:>6}  correct={} incorrect={} pct={}'.format(
        e['rankPosition'], e['studentName'], str(e['score']), e['correctCount'],
        e['incorrectCount'], e['percentile']))

check('cohort size is 4', board['totalCandidates'] == 4, board['totalCandidates'])
ranks = [e['rankPosition'] for e in board['entries']]
check('ranks ascend from 1', ranks[0] == 1, ranks)
scores = [float(e['score']) for e in board['entries']]
check('leaderboard ordered by score descending', scores == sorted(scores, reverse=True), scores)
check('top scorer is at percentile 100', float(board['entries'][0]['percentile']) == 100.0,
      board['entries'][0]['percentile'])
# Priya and Meera both scored 35 with identical correct/incorrect counts, so they tie on
# everything except completion time -- the documented tie-break chain.
tied = [e for e in board['entries'] if float(e['score']) == 35.0]
check('the two students on 35 share a percentile',
      len({str(e['percentile']) for e in tied}) == 1, [e['percentile'] for e in tied])

print('\n' + ('ALL PASSED' if not failures else 'FAILURES: ' + str(failures)))
