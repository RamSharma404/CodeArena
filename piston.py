import json
import urllib.request
from urllib.error import HTTPError

payload = {
    "compiler": "openjdk-25",
    "code": "import java.util.*;\n\nclass Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        String line = sc.nextLine().trim();\n        String s = line.substring(line.indexOf('=') + 1);\n        Solution sol = new Solution();\n        System.out.println(sol.isValid(s));\n    }\n}\n\nclass Solution {\n    public boolean isValid(String s) {\n        java.util.Stack<Character> stack = new java.util.Stack<>();\n        for (char c : s.toCharArray()) {\n            if (c == '(' || c == '{' || c == '[') {\n                stack.push(c);\n            } else {\n                if (stack.isEmpty()) return false;\n                char top = stack.pop();\n                if (c == ')' && top != '(') return false;\n                if (c == '}' && top != '{') return false;\n                if (c == ']' && top != '[') return false;\n            }\n        }\n        return stack.isEmpty();\n    }\n}",
    "input": "s=()"
}

req = urllib.request.Request(
    'https://api.onlinecompiler.io/api/run-code-sync/',
    data=json.dumps(payload).encode('utf-8'),
    headers={'Authorization': '73ff6f3a526d1903ceb7ca73271f0779', 'Content-Type': 'application/json'}
)

try:
    with urllib.request.urlopen(req) as response:
        print(response.read().decode('utf-8'))
except HTTPError as e:
    print("HTTPError:", e.code)
    print(e.read().decode('utf-8'))
except Exception as e:
    print("Error:", str(e))
