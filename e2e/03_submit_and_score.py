import json, os, urllib.request, urllib.error

API = 'http://localhost:8080/api'
state = json.load(open(os.path.join(os.path.dirname(__file__), 'state.json')))
aid, TOK = state['attemptId'], state['token']

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

failures = []
def check(label, cond, detail=''):
    print(('  PASS  ' if cond else '  FAIL  ') + label + (('  -> ' + str(detail)) if detail else ''))
    if not cond:
        failures.append(label)

print('BEFORE SUBMISSION')
status, res = call('GET', '/attempts/%d/result' % aid, TOK)
check('result withheld while the test is live', status == 409, status)
status, res = call('GET', '/attempts/%d/review' % aid, TOK)
check('review withheld while the test is live', status == 409, status)

print('\nSUBMIT')
status, result = call('POST', '/attempts/%d/submit' % aid, TOK)
check('submit succeeds', status == 200, status)
check('status is EVALUATED', result['status'] == 'EVALUATED', result['status'])

# 15 correct at +4, 5 wrong at -1, 5 unattempted at 0  ->  60 - 5 = 55 out of 100
print('\nSCORING  (expected: 15 correct, 5 wrong, 5 blank -> 55 / 100)')
check('score is 55', float(result['score']) == 55.0, result['score'])
check('max score is 100', float(result['maxScore']) == 100.0, result['maxScore'])
check('correct count 15', result['correctCount'] == 15, result['correctCount'])
check('incorrect count 5', result['incorrectCount'] == 5, result['incorrectCount'])
check('unanswered count 5', result['unansweredCount'] == 5, result['unansweredCount'])
check('attempted count 20', result['attemptedCount'] == 20, result['attemptedCount'])
check('negative marks 5.00', float(result['negativeMarks']) == 5.0, result['negativeMarks'])
check('accuracy 75.00  (15/20)', float(result['accuracy']) == 75.0, result['accuracy'])
check('attempt rate 80.00  (20/25)', float(result['attemptRate']) == 80.0, result['attemptRate'])
check('time taken recorded', result['timeTakenSeconds'] is not None, result['timeTakenSeconds'])

print('\nRANKING')
check('ranking enabled for a fixed-set test', result['rankingEnabled'] is True)
check('rank assigned', result['rankPosition'] == 1, result['rankPosition'])
check('cohort size 1', result['totalCandidates'] == 1, result['totalCandidates'])
check('percentile 100 for the only candidate', float(result['percentile']) == 100.0,
      result['percentile'])

print('\nIDEMPOTENCY')
status, again = call('POST', '/attempts/%d/submit' % aid, TOK)
check('re-submitting returns the result, not an error', status == 200, status)
check('score unchanged on re-submit', float(again['score']) == 55.0, again['score'])
status, third = call('POST', '/attempts/%d/submit' % aid, TOK)
check('third submit still consistent', float(third['score']) == 55.0, third['score'])

print('\nAFTER SUBMISSION: answers are frozen')
_, review = call('GET', '/attempts/%d/review' % aid, TOK)
q0 = review[0]
status, res = call('PUT', '/attempts/%d/answers' % aid, TOK,
                   {'attemptQuestionId': q0['attemptQuestionId'], 'selectedOptionIds': [],
                    'clientSequence': 9999})
check('answers rejected after submission', status == 409, status)
check('error code is ATTEMPT_ALREADY_FINALISED', res['error'] == 'ATTEMPT_ALREADY_FINALISED',
      res['error'])

print('\nREVIEW  (answer key released only now)')
check('review returns 25 questions', len(review) == 25, len(review))
check('solution now present', q0['solutionContent'] and '$' in q0['solutionContent'],
      (q0['solutionContent'] or '')[:50])
check('answer key now present', len(q0['correctOptionIds']) >= 1, q0['correctOptionIds'])
check('options now carry isCorrect', 'isCorrect' in q0['options'][0])
check('per-question marks recorded', q0['marksAwarded'] is not None, q0['marksAwarded'])
statuses = {}
for q in review:
    statuses[q['resultStatus']] = statuses.get(q['resultStatus'], 0) + 1
check('per-question statuses tally', statuses == {'CORRECT': 15, 'INCORRECT': 5, 'UNANSWERED': 5},
      statuses)

print('\nHISTORY AND DASHBOARD')
_, hist = call('GET', '/attempts/history', TOK)
check('attempt appears in history', hist['totalElements'] == 1, hist['totalElements'])
_, perf = call('GET', '/me/performance', TOK)
check('performance summary built', perf['testsTaken'] == 1, perf['testsTaken'])
check('recent score plotted', len(perf['recentScores']) == 1, len(perf['recentScores']))
check('chapter breakdown present', len(perf['chapterBreakdown']) >= 1, perf['chapterBreakdown'])
_, board = call('GET', '/rankings/tests/1', TOK)
check('leaderboard lists the student', len(board['entries']) == 1, len(board['entries']))
check('leaderboard flags the current user', board['entries'][0]['isCurrentUser'] is True)
check('leaderboard exposes no phone number', 'phoneNumber' not in json.dumps(board))

print('\nATTEMPT LIMIT')
status, res = call('POST', '/attempts', TOK, {'testId': 1})
check('cannot retake a one-attempt test', status == 409, status)

print('\n' + ('ALL PASSED' if not failures else 'FAILURES: ' + str(failures)))
