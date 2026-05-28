package com.auth.demo.config;

import com.auth.demo.model.Problem;
import com.auth.demo.model.ProblemTemplate;
import com.auth.demo.repository.ProblemRepository;
import com.auth.demo.repository.ProblemTemplateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeds / fixes code templates for all problems.
 *
 * Controlled by application.properties:
 *   seed.problems=true   → runs on every startup (upserts, safe to repeat)
 *   seed.problems=false  → skipped entirely
 *
 * CONVENTION:
 *   - Test-case inputs are RAW multi-line text (e.g. "2 7 11 15\n9")
 *   - All driver codes read stdin in that exact raw format
 *   - Java user template: class Solution { ... }
 *   - Python user template: bare function
 *   - C++ user template: bare function
 *   - JavaScript user template: var func = function(...) { ... };
 */
@Component
@ConditionalOnProperty(name = "seed.problems", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final ProblemTemplateRepository templateRepository;
    private final ProblemRepository problemRepository;

    public DataSeeder(ProblemTemplateRepository templateRepository,
                      ProblemRepository problemRepository) {
        this.templateRepository = templateRepository;
        this.problemRepository = problemRepository;
    }

    @Override
    public void run(String... args) {
        seedTwoSum();
        seedValidParentheses();
        seedLongestSubstring();
        seedMedianArrays();
        seedLongestPalindrome();
        System.out.println("✅ Template seeding complete.");
    }

    private void save(Long problemId, String language, String userTemplate, String driverCode) {
        if (!problemRepository.existsById(problemId)) return;
        // Always upsert — so stale templates get fixed on every deploy
        ProblemTemplate existing = templateRepository
                .findByProblemIdAndLanguage(problemId, language)
                .orElse(ProblemTemplate.builder()
                        .problemId(problemId)
                        .language(language)
                        .build());
        existing.setUserTemplate(userTemplate);
        existing.setDriverCode(driverCode);
        templateRepository.save(existing);
        System.out.println("  Upserted template: problem=" + problemId + " lang=" + language);
    }

    // ═══════════════════════════════════════════════════════════
    // PROBLEM: Two Sum
    // Input:  Line 1 = space-separated nums,  Line 2 = target
    // Output: "idx1 idx2"
    // ═══════════════════════════════════════════════════════════
    private void seedTwoSum() {
        Problem p = problemRepository.findBySlug("two-sum").orElse(null);
        if (p == null) return;
        Long pid = p.getId();

        // ── JAVA ──
        save(pid, "JAVA",
"""
class Solution {
    public int[] twoSum(int[] nums, int target) {
        // Write your solution here
        return new int[]{};
    }
}
""",
"""
import java.util.*;

class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String[] parts = sc.nextLine().trim().split(" ");
        int[] nums = new int[parts.length];
        for (int i = 0; i < parts.length; i++) nums[i] = Integer.parseInt(parts[i]);
        int target = Integer.parseInt(sc.nextLine().trim());
        Solution sol = new Solution();
        int[] r = sol.twoSum(nums, target);
        System.out.println(r[0] + " " + r[1]);
    }
}

{{USER_CODE}}
""");

        // ── PYTHON ──
        save(pid, "PYTHON",
"""
def twoSum(nums, target):
    # Write your solution here
    pass
""",
"""
import sys
data = sys.stdin.read().splitlines()
nums = list(map(int, data[0].split()))
target = int(data[1])

{{USER_CODE}}

if 'Solution' in globals():
    result = Solution().twoSum(nums, target)
else:
    result = twoSum(nums, target)
print(result[0], result[1])
""");

        // ── CPP ──
        save(pid, "CPP",
"""
vector<int> twoSum(vector<int>& nums, int target) {
    // Write your solution here
    return {};
}
""",
"""
#include<bits/stdc++.h>
using namespace std;
{{USER_CODE}}
int main() {
    string line; getline(cin, line);
    istringstream iss(line);
    vector<int> nums; int x;
    while(iss >> x) nums.push_back(x);
    int target; cin >> target;
    vector<int> result = twoSum(nums, target);
    cout << result[0] << " " << result[1] << endl;
    return 0;
}
""");

        // ── JAVASCRIPT ──
        save(pid, "JAVASCRIPT",
"""
var twoSum = function(nums, target) {
    // Write your solution here

};
""",
"""
{{USER_CODE}}

const _inp = require('fs').readFileSync(0, 'utf-8');
function __main__() {
    const lines = _inp.trim().split('\\n');
    const nums = lines[0].trim().split(' ').map(Number);
    const target = parseInt(lines[1].trim());
    const r = twoSum(nums, target);
    console.log(r[0] + ' ' + r[1]);
}
__main__();
""");
    }

    // ═══════════════════════════════════════════════════════════
    // PROBLEM: Valid Parentheses
    // Input:  Single-line string  e.g. "()"  "()[]{}"  "(]"
    // Output: "true" or "false"
    // ═══════════════════════════════════════════════════════════
    private void seedValidParentheses() {
        Problem p = problemRepository.findBySlug("valid-parentheses").orElse(null);
        if (p == null) return;
        Long pid = p.getId();

        // ── JAVA ──
        save(pid, "JAVA",
"""
class Solution {
    public boolean isValid(String s) {
        // Write your solution here
        return false;
    }
}
""",
"""
import java.util.*;

class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine().trim();
        Solution sol = new Solution();
        System.out.println(sol.isValid(s));
    }
}

{{USER_CODE}}
""");

        // ── PYTHON ──
        save(pid, "PYTHON",
"""
def isValid(s):
    # Write your solution here
    pass
""",
"""
import sys
s = sys.stdin.read().strip()

{{USER_CODE}}

if 'Solution' in globals():
    res = Solution().isValid(s)
else:
    res = isValid(s)
print(str(res).lower())
""");

        // ── CPP ──
        save(pid, "CPP",
"""
bool isValid(string s) {
    // Write your solution here
    return false;
}
""",
"""
#include<bits/stdc++.h>
using namespace std;
{{USER_CODE}}
int main() {
    string s; getline(cin, s);
    cout << (isValid(s) ? "true" : "false") << endl;
    return 0;
}
""");

        // ── JAVASCRIPT ──
        save(pid, "JAVASCRIPT",
"""
var isValid = function(s) {
    // Write your solution here

};
""",
"""
{{USER_CODE}}

const _inp = require('fs').readFileSync(0, 'utf-8');
function __main__() {
    const s = _inp.trim();
    console.log(isValid(s));
}
__main__();
""");
    }

    // ═══════════════════════════════════════════════════════════
    // PROBLEM: Longest Substring Without Repeating Characters
    // Input:  Single-line string  e.g. "abcabcbb"
    // Output: An integer           e.g. "3"
    // ═══════════════════════════════════════════════════════════
    private void seedLongestSubstring() {
        Problem p = problemRepository.findBySlug("longest-substring-without-repeating").orElse(null);
        if (p == null) return;
        Long pid = p.getId();

        // ── JAVA ──
        save(pid, "JAVA",
"""
class Solution {
    public int lengthOfLongestSubstring(String s) {
        // Write your solution here
        return 0;
    }
}
""",
"""
import java.util.*;

class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.hasNextLine() ? sc.nextLine() : "";
        Solution sol = new Solution();
        System.out.println(sol.lengthOfLongestSubstring(s));
    }
}

{{USER_CODE}}
""");

        // ── PYTHON ──
        save(pid, "PYTHON",
"""
def lengthOfLongestSubstring(s):
    # Write your solution here
    pass
""",
"""
import sys
s = sys.stdin.read()

{{USER_CODE}}

if 'Solution' in globals():
    print(Solution().lengthOfLongestSubstring(s))
else:
    print(lengthOfLongestSubstring(s))
""");

        // ── CPP ──
        save(pid, "CPP",
"""
int lengthOfLongestSubstring(string s) {
    // Write your solution here
    return 0;
}
""",
"""
#include<bits/stdc++.h>
using namespace std;
{{USER_CODE}}
int main() {
    string s; getline(cin, s);
    cout << lengthOfLongestSubstring(s) << endl;
    return 0;
}
""");

        // ── JAVASCRIPT ──
        save(pid, "JAVASCRIPT",
"""
var lengthOfLongestSubstring = function(s) {
    // Write your solution here
    return 0;
};
""",
"""
{{USER_CODE}}

const _inp = require('fs').readFileSync(0, 'utf-8');
function __main__() {
    const s = _inp;
    console.log(lengthOfLongestSubstring(s));
}
__main__();
""");
    }

    // ═══════════════════════════════════════════════════════════
    // PROBLEM: Median of Two Sorted Arrays
    // Input:  Line 1 = space-separated nums1,  Line 2 = space-separated nums2
    // Output: A decimal like "2.0" or "2.5"
    // ═══════════════════════════════════════════════════════════
    private void seedMedianArrays() {
        Problem p = problemRepository.findBySlug("median-of-two-sorted-arrays").orElse(null);
        if (p == null) return;
        Long pid = p.getId();

        // ── JAVA ──
        save(pid, "JAVA",
"""
class Solution {
    public double findMedianSortedArrays(int[] nums1, int[] nums2) {
        // Write your solution here
        return 0.0;
    }
}
""",
"""
import java.util.*;

class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String l1 = sc.nextLine().trim();
        String l2 = sc.nextLine().trim();
        l1 = l1.replaceAll("\\[|\\]", "").trim();
        l2 = l2.replaceAll("\\[|\\]", "").trim();
        int[] nums1 = l1.isEmpty() ? new int[0] : Arrays.stream(l1.split("[,\\s]+"))
                .mapToInt(Integer::parseInt).toArray();
        int[] nums2 = l2.isEmpty() ? new int[0] : Arrays.stream(l2.split("[,\\s]+"))
                .mapToInt(Integer::parseInt).toArray();
        Solution sol = new Solution();
        double result = sol.findMedianSortedArrays(nums1, nums2);
        if (result == (long) result) {
            System.out.printf("%.1f%n", result);
        } else {
            System.out.println(result);
        }
    }
}

{{USER_CODE}}
""");

        // ── PYTHON ──
        save(pid, "PYTHON",
"""
def findMedianSortedArrays(nums1, nums2):
    # Write your solution here
    pass
""",
"""
import sys, re
data = sys.stdin.read().splitlines()
def parse_array(line):
    line = re.sub(r'\\[|\\]', '', line).strip()
    if not line: return []
    return list(map(int, re.split(r'[,\\s]+', line)))
nums1 = parse_array(data[0])
nums2 = parse_array(data[1])

{{USER_CODE}}

if 'Solution' in globals():
    result = Solution().findMedianSortedArrays(nums1, nums2)
else:
    result = findMedianSortedArrays(nums1, nums2)
if result == int(result):
    print(float(result))
else:
    print(result)
""");

        // ── CPP ──
        save(pid, "CPP",
"""
double findMedianSortedArrays(vector<int>& nums1, vector<int>& nums2) {
    // Write your solution here
    return 0.0;
}
""",
"""
#include<bits/stdc++.h>
using namespace std;
{{USER_CODE}}
int main() {
    string l1, l2;
    getline(cin, l1);
    getline(cin, l2);
    auto parse = [](string s) {
        vector<int> res;
        for (char &c : s) if (c == '[' || c == ']' || c == ',') c = ' ';
        istringstream iss(s);
        int x;
        while (iss >> x) res.push_back(x);
        return res;
    };
    vector<int> n1 = parse(l1);
    vector<int> n2 = parse(l2);
    double r = findMedianSortedArrays(n1, n2);
    if (r == (long long)r) cout << fixed << setprecision(1) << r << endl;
    else cout << r << endl;
    return 0;
}
""");

        // ── JAVASCRIPT ──
        save(pid, "JAVASCRIPT",
"""
var findMedianSortedArrays = function(nums1, nums2) {
    // Write your solution here

};
""",
"""
{{USER_CODE}}

const _inp = require('fs').readFileSync(0, 'utf-8');
function __main__() {
    const lines = _inp.trim().split('\\n');
    const parse = (line) => {
        line = line.replace(/\\[|\\]/g, '').trim();
        if (!line) return [];
        return line.split(/[,\\s]+/).map(Number);
    };
    const nums1 = parse(lines[0]);
    const nums2 = parse(lines[1]);
    const r = findMedianSortedArrays(nums1, nums2);
    console.log(Number.isInteger(r) ? r.toFixed(1) : r);
}
__main__();
""");
    }

    // ═══════════════════════════════════════════════════════════
    // PROBLEM: Longest Palindromic Substring
    // Input:  Single-line string  e.g. "babad"
    // Output: A string            e.g. "bab"
    // ═══════════════════════════════════════════════════════════
    private void seedLongestPalindrome() {
        Problem p = problemRepository.findBySlug("longest-palindromic-substring").orElse(null);
        if (p == null) return;
        Long pid = p.getId();

        // ── JAVA ──
        save(pid, "JAVA",
"""
class Solution {
    public String longestPalindrome(String s) {
        // Write your solution here
        return "";
    }
}
""",
"""
import java.util.*;

class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine().trim();
        Solution sol = new Solution();
        System.out.println(sol.longestPalindrome(s));
    }
}

{{USER_CODE}}
""");

        // ── PYTHON ──
        save(pid, "PYTHON",
"""
def longestPalindrome(s):
    # Write your solution here
    pass
""",
"""
import sys
s = sys.stdin.read().strip()

{{USER_CODE}}

if 'Solution' in globals():
    print(Solution().longestPalindrome(s))
else:
    print(longestPalindrome(s))
""");

        // ── CPP ──
        save(pid, "CPP",
"""
string longestPalindrome(string s) {
    // Write your solution here
    return "";
}
""",
"""
#include<bits/stdc++.h>
using namespace std;
{{USER_CODE}}
int main() {
    string s;
    getline(cin, s);
    cout << longestPalindrome(s) << endl;
    return 0;
}
""");

        // ── JAVASCRIPT ──
        save(pid, "JAVASCRIPT",
"""
var longestPalindrome = function(s) {
    // Write your solution here

};
""",
"""
{{USER_CODE}}

const _inp = require('fs').readFileSync(0, 'utf-8');
function __main__() {
    const s = _inp.trim();
    console.log(longestPalindrome(s));
}
__main__();
""");
    }
}
