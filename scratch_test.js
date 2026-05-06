async function test() {
    const code = `import java.util.*;
import java.io.*;

class Main {
    // empty
}

class Solution {
    public int[] twoSum(int[] nums, int target) {
        return new int[]{0,1};
    }
}

public class __BatchRunner__ {
    public static void main(String[] args) {
        try {
            Scanner sc = new Scanner(System.in);
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
`;

    try {
        const res = await fetch('https://api.onlinecompiler.io/api/run-code-sync/', {
            method: 'POST',
            body: JSON.stringify({
                compiler: 'openjdk-25',
                code: code,
                input: 'nums=[2,7,11,15], target=9'
            }),
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': 'e9d5e15a1ad5c994dc1bbd9ed0e2a971' 
            }
        });
        const data = await res.json();
        console.log(data);
    } catch(e) {
        console.error(e.message);
    }
}

test();
