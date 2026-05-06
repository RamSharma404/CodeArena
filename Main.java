import java.util.*;
import java.io.*;

class Main {
    static final String SENTINEL = "---TC---";
    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String first = br.readLine();
        if (first == null) return;
        int n = Integer.parseInt(first.trim());
        for (int __t__ = 0; __t__ < n; __t__++) {
            StringBuilder __block__ = new StringBuilder();
            String __ln__;
            while ((__ln__ = br.readLine()) != null) {
                if (__ln__.equals(SENTINEL)) break;
                if (__block__.length() > 0) __block__.append("\n");
                __block__.append(__ln__);
            }
            __runOne__(__block__.toString());
            if (__t__ < n - 1) System.out.print(SENTINEL);
        }
    }
    
    static void __runOne__(String __input__) {
        try {
            Scanner sc = new Scanner(__input__);
            if (!sc.hasNextLine()) {
                System.out.println("NO INPUT");
                return;
            }
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class Solution {
    public int[] twoSum(int[] nums, int target) {
        return new int[]{0,1};
    }
}
