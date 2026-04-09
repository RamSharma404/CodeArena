package com.auth.demo.config;

import com.auth.demo.model.Problem;
import com.auth.demo.model.ProblemTemplate;
import com.auth.demo.repository.ProblemRepository;
import com.auth.demo.repository.ProblemTemplateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
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
        seedLongestSubstring();
        seedMedianArrays();
        seedValidParentheses();
        System.out.println("✅ Template seeding complete.");
    }

    private void save(Long problemId, String language, String userTemplate, String driverCode) {
        if (!problemRepository.existsById(problemId)) return;
        if (templateRepository.findByProblemIdAndLanguage(problemId, language).isPresent()) return;
        templateRepository.save(ProblemTemplate.builder()
                .problemId(problemId)
                .language(language)
                .userTemplate(userTemplate)
                .driverCode(driverCode)
                .build());
        System.out.println("  Seeded template: problem=" + problemId + " lang=" + language);
    }

    // ═══════════════════════════════════════════════════════════
    // PROBLEM 1: Two Sum
    // Input:  nums=[2,7,11,15], target=9
    // Output: [0,1]
    // ═══════════════════════════════════════════════════════════
    private void seedTwoSum() {
        Problem p = problemRepository.findBySlug("two-sum").orElse(null);
        if (p == null) return;
        Long pid = p.getId();

        // ── JAVA ──
        save(pid, "JAVA", """
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
        String line = sc.nextLine().trim();
        int aS = line.indexOf('[');
        int aE = line.indexOf(']');
        String arrStr = line.substring(aS + 1, aE);
        int[] nums = Arrays.stream(arrStr.split(","))
                .mapToInt(s -> Integer.parseInt(s.trim())).toArray();
        int target = Integer.parseInt(line.substring(line.lastIndexOf('=') + 1).trim());
        Solution sol = new Solution();
        int[] r = sol.twoSum(nums, target);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < r.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(r[i]);
        }
        sb.append("]");
        System.out.println(sb);
    }
}

{{USER_CODE}}
""");

        // ── PYTHON ──
        save(pid, "PYTHON", """
class Solution:
    def twoSum(self, nums: list[int], target: int) -> list[int]:
        # Write your solution here
        pass
""",
"""
{{USER_CODE}}

line = input().strip()
nums_str = line[line.index('['):line.index(']')+1]
nums = list(map(int, nums_str[1:-1].split(',')))
target = int(line.split('target=')[1].strip())
sol = Solution()
result = sol.twoSum(nums, target)
print('[' + ','.join(map(str, result)) + ']')
""");

        // ── CPP ──
        save(pid, "CPP", """
class Solution {
public:
    vector<int> twoSum(vector<int>& nums, int target) {
        // Write your solution here
        return {};
    }
};
""",
"""
#include <bits/stdc++.h>
using namespace std;

{{USER_CODE}}

int main() {
    string line;
    getline(cin, line);
    int aS = line.find('[');
    int aE = line.find(']');
    string arrStr = line.substr(aS + 1, aE - aS - 1);
    vector<int> nums;
    stringstream ss(arrStr);
    string tok;
    while (getline(ss, tok, ',')) nums.push_back(stoi(tok));
    int target = stoi(line.substr(line.rfind('=') + 1));
    Solution sol;
    vector<int> r = sol.twoSum(nums, target);
    cout << "[";
    for (int i = 0; i < r.size(); i++) {
        if (i > 0) cout << ",";
        cout << r[i];
    }
    cout << "]" << endl;
    return 0;
}
""");

        // ── JAVASCRIPT ──
        save(pid, "JAVASCRIPT", """
var twoSum = function(nums, target) {
    // Write your solution here

};
""",
"""
{{USER_CODE}}

let _inp = '';
process.stdin.on('data', d => _inp += d);
process.stdin.on('end', () => {
    const line = _inp.trim();
    const arrMatch = line.match(/\\[(.*?)\\]/);
    const nums = arrMatch[1].split(',').map(Number);
    const target = parseInt(line.split('target=')[1]);
    const r = twoSum(nums, target);
    console.log('[' + r.join(',') + ']');
});
""");
    }

    // ═══════════════════════════════════════════════════════════
    // PROBLEM 2: Longest Substring Without Repeating Characters
    // Input:  s=abcabcbb
    // Output: 3
    // ═══════════════════════════════════════════════════════════
    private void seedLongestSubstring() {
        Problem p = problemRepository.findBySlug("longest-substring-without-repeating").orElse(null);
        if (p == null) return;
        Long pid = p.getId();

        save(pid, "JAVA", """
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
        String line = sc.nextLine().trim();
        String s = line.substring(line.indexOf('=') + 1);
        Solution sol = new Solution();
        System.out.println(sol.lengthOfLongestSubstring(s));
    }
}

{{USER_CODE}}
""");

        save(pid, "PYTHON", """
class Solution:
    def lengthOfLongestSubstring(self, s: str) -> int:
        # Write your solution here
        pass
""",
"""
{{USER_CODE}}

line = input().strip()
s = line.split('=', 1)[1]
sol = Solution()
print(sol.lengthOfLongestSubstring(s))
""");

        save(pid, "CPP", """
class Solution {
public:
    int lengthOfLongestSubstring(string s) {
        // Write your solution here
        return 0;
    }
};
""",
"""
#include <bits/stdc++.h>
using namespace std;

{{USER_CODE}}

int main() {
    string line;
    getline(cin, line);
    string s = line.substr(line.find('=') + 1);
    Solution sol;
    cout << sol.lengthOfLongestSubstring(s) << endl;
    return 0;
}
""");

        save(pid, "JAVASCRIPT", """
var lengthOfLongestSubstring = function(s) {
    // Write your solution here

};
""",
"""
{{USER_CODE}}

let _inp = '';
process.stdin.on('data', d => _inp += d);
process.stdin.on('end', () => {
    const line = _inp.trim();
    const s = line.substring(line.indexOf('=') + 1);
    console.log(lengthOfLongestSubstring(s));
});
""");
    }

    // ═══════════════════════════════════════════════════════════
    // PROBLEM 3: Median of Two Sorted Arrays
    // Input:  nums1=[1,3], nums2=[2]
    // Output: 2.0
    // ═══════════════════════════════════════════════════════════
    private void seedMedianArrays() {
        Problem p = problemRepository.findBySlug("median-of-two-sorted-arrays").orElse(null);
        if (p == null) return;
        Long pid = p.getId();

        save(pid, "JAVA", """
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
        String line = sc.nextLine().trim();
        int s1 = line.indexOf('['), e1 = line.indexOf(']');
        int s2 = line.indexOf('[', e1 + 1), e2 = line.indexOf(']', s2);
        String a1 = line.substring(s1 + 1, e1);
        String a2 = line.substring(s2 + 1, e2);
        int[] nums1 = a1.isEmpty() ? new int[0] :
                Arrays.stream(a1.split(",")).mapToInt(s -> Integer.parseInt(s.trim())).toArray();
        int[] nums2 = a2.isEmpty() ? new int[0] :
                Arrays.stream(a2.split(",")).mapToInt(s -> Integer.parseInt(s.trim())).toArray();
        Solution sol = new Solution();
        double r = sol.findMedianSortedArrays(nums1, nums2);
        if (r == (long) r) System.out.printf("%.1f%n", r);
        else System.out.println(r);
    }
}

{{USER_CODE}}
""");

        save(pid, "PYTHON", """
class Solution:
    def findMedianSortedArrays(self, nums1: list[int], nums2: list[int]) -> float:
        # Write your solution here
        pass
""",
"""
import re

{{USER_CODE}}

line = input().strip()
arrays = re.findall(r'\\[(.*?)\\]', line)
nums1 = list(map(int, arrays[0].split(','))) if arrays[0] else []
nums2 = list(map(int, arrays[1].split(','))) if arrays[1] else []
sol = Solution()
r = sol.findMedianSortedArrays(nums1, nums2)
if r == int(r):
    print(f"{r:.1f}")
else:
    print(r)
""");

        save(pid, "CPP", """
class Solution {
public:
    double findMedianSortedArrays(vector<int>& nums1, vector<int>& nums2) {
        // Write your solution here
        return 0.0;
    }
};
""",
"""
#include <bits/stdc++.h>
using namespace std;

{{USER_CODE}}

int main() {
    string line;
    getline(cin, line);
    vector<vector<int>> arrs;
    size_t pos = 0;
    while ((pos = line.find('[', pos)) != string::npos) {
        size_t end = line.find(']', pos);
        string s = line.substr(pos + 1, end - pos - 1);
        vector<int> a;
        if (!s.empty()) {
            stringstream ss(s);
            string tok;
            while (getline(ss, tok, ',')) a.push_back(stoi(tok));
        }
        arrs.push_back(a);
        pos = end + 1;
    }
    Solution sol;
    double r = sol.findMedianSortedArrays(arrs[0], arrs[1]);
    cout << fixed << setprecision(1) << r << endl;
    return 0;
}
""");

        save(pid, "JAVASCRIPT", """
var findMedianSortedArrays = function(nums1, nums2) {
    // Write your solution here

};
""",
"""
{{USER_CODE}}

let _inp = '';
process.stdin.on('data', d => _inp += d);
process.stdin.on('end', () => {
    const line = _inp.trim();
    const matches = line.match(/\\[(.*?)\\]/g);
    const nums1 = matches[0].slice(1,-1).split(',').filter(x=>x).map(Number);
    const nums2 = matches[1].slice(1,-1).split(',').filter(x=>x).map(Number);
    const r = findMedianSortedArrays(nums1, nums2);
    console.log(Number.isInteger(r) ? r.toFixed(1) : r);
});
""");
    }

    // ═══════════════════════════════════════════════════════════
    // PROBLEM 4: Valid Parentheses
    // Input:  s=()
    // Output: true
    // ═══════════════════════════════════════════════════════════
    private void seedValidParentheses() {
        Problem p = problemRepository.findBySlug("valid-parentheses").orElse(null);
        if (p == null) return;
        Long pid = p.getId();

        save(pid, "JAVA", """
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
        String line = sc.nextLine().trim();
        String s = line.substring(line.indexOf('=') + 1);
        Solution sol = new Solution();
        System.out.println(sol.isValid(s));
    }
}

{{USER_CODE}}
""");

        save(pid, "PYTHON", """
class Solution:
    def isValid(self, s: str) -> bool:
        # Write your solution here
        pass
""",
"""
{{USER_CODE}}

line = input().strip()
s = line.split('=', 1)[1]
sol = Solution()
print(str(sol.isValid(s)).lower())
""");

        save(pid, "CPP", """
class Solution {
public:
    bool isValid(string s) {
        // Write your solution here
        return false;
    }
};
""",
"""
#include <bits/stdc++.h>
using namespace std;

{{USER_CODE}}

int main() {
    string line;
    getline(cin, line);
    string s = line.substr(line.find('=') + 1);
    Solution sol;
    cout << (sol.isValid(s) ? "true" : "false") << endl;
    return 0;
}
""");

        save(pid, "JAVASCRIPT", """
var isValid = function(s) {
    // Write your solution here

};
""",
"""
{{USER_CODE}}

let _inp = '';
process.stdin.on('data', d => _inp += d);
process.stdin.on('end', () => {
    const line = _inp.trim();
    const s = line.substring(line.indexOf('=') + 1);
    console.log(isValid(s));
});
""");
    }
}
