import json
import urllib.request

payload = {
    "problemId": 6,
    "language": "JAVA",
    "code": "class Solution { public boolean isValid(String s) { java.util.Stack<Character> stack = new java.util.Stack<>(); for (char c : s.toCharArray()) { if (c == '(' || c == '{' || c == '[') { stack.push(c); } else { if (stack.isEmpty()) return false; char top = stack.pop(); if (c == ')' && top != '(') return false; if (c == '}' && top != '{') return false; if (c == ']' && top != '[') return false; } } return stack.isEmpty(); } }"
}

req = urllib.request.Request(
    'http://localhost:8080/api/execute',
    data=json.dumps(payload).encode('utf-8'),
    headers={'Content-Type': 'application/json'}
)

try:
    with urllib.request.urlopen(req) as response:
        result = json.loads(response.read().decode('utf-8'))
        print(json.dumps(result, indent=2))
except Exception as e:
    print("Error:", str(e))
