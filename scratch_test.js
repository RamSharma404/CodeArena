async function test() {
    // This simulates the EXACT code structure the new SubmissionWorker will generate
    const code = `import java.io.*;
import java.util.*;

class Main {
    static final String SENTINEL = "---TC---";
    public static void main(String[] args) {
        try {
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(System.in));
            int n = Integer.parseInt(br.readLine().trim());
            for (int __t__ = 0; __t__ < n; __t__++) {
                StringBuilder __block__ = new StringBuilder();
                String __ln__;
                while ((__ln__ = br.readLine()) != null) {
                    if (__ln__.equals(SENTINEL)) break;
                    if (__block__.length() > 0) __block__.append("\\n");
                    __block__.append(__ln__);
                }
                __runOne__(__block__.toString());
                if (__t__ < n - 1) System.out.print(SENTINEL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    static void __runOne__(String __input__) {
        java.util.Scanner sc = new java.util.Scanner(__input__);
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

class Solution {
    public int[] twoSum(int[] nums, int target) {
        HashMap<Integer,Integer> map = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int comp = target - nums[i];
            if (map.containsKey(comp)) return new int[]{map.get(comp), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
`;

    try {
        const res = await fetch('https://api.onlinecompiler.io/api/run-code-sync/', {
            method: 'POST',
            body: JSON.stringify({
                compiler: 'openjdk-25',
                code: code,
                input: '2\nnums=[2,7,11,15], target=9\n---TC---\nnums=[3,2,4], target=6'
            }),
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': 'e9d5e15a1ad5c994dc1bbd9ed0e2a971' 
            }
        });
        const data = await res.json();
        console.log(JSON.stringify(data, null, 2));
    } catch(e) {
        console.error(e.message);
    }
}

test();
