import java.util.*;

    class Solution {
        public boolean isValid(String s) {
            Stack<Character> stack = new Stack<>();
            for (char c : s.toCharArray()) {
                if (c == '(' || c == '{' || c == '[') {
                    stack.push(c);
                } else {
                    if (stack.isEmpty())
                        return false;
                    char top = stack.pop();
                    if (c == ')' && top != '(')
                        return false;
                    if (c == '}' && top != '{')
                        return false;
                    if (c == ']' && top != '[')
                        return false;
                }
            }
            return stack.isEmpty();
        }
    }

class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String line = sc.nextLine().trim();
        String s = line.substring(line.indexOf('=') + 1);
        Solution sol = new Solution();
        System.out.println(sol.isValid(s));
        sc.close();
    }
}
