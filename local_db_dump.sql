-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: coding_platform
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `code_runs`
--

DROP TABLE IF EXISTS `code_runs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `code_runs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `problem_id` bigint DEFAULT NULL,
  `code` text NOT NULL,
  `language` varchar(20) NOT NULL,
  `custom_input` text,
  `output` text,
  `status` varchar(50) DEFAULT NULL,
  `runtime_ms` int DEFAULT NULL,
  `memory_kb` int DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `problem_id` (`problem_id`),
  CONSTRAINT `code_runs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `code_runs_ibfk_2` FOREIGN KEY (`problem_id`) REFERENCES `problems` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `code_runs`
--

LOCK TABLES `code_runs` WRITE;
/*!40000 ALTER TABLE `code_runs` DISABLE KEYS */;
/*!40000 ALTER TABLE `code_runs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `code_snippets`
--

DROP TABLE IF EXISTS `code_snippets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `code_snippets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `driver_code` text NOT NULL,
  `language` varchar(20) NOT NULL,
  `problem_id` bigint NOT NULL,
  `solution_template` text NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKdlhdgvsldvj5q450tnwltr3rp` (`problem_id`,`language`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `code_snippets`
--

LOCK TABLES `code_snippets` WRITE;
/*!40000 ALTER TABLE `code_snippets` DISABLE KEYS */;
/*!40000 ALTER TABLE `code_snippets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `problem_stats`
--

DROP TABLE IF EXISTS `problem_stats`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `problem_stats` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `problem_id` bigint NOT NULL,
  `total_submissions` int DEFAULT '0',
  `accepted_submissions` int DEFAULT '0',
  `avg_runtime_ms` int DEFAULT '0',
  `min_runtime_ms` int DEFAULT '0',
  `max_runtime_ms` int DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `problem_id` (`problem_id`),
  CONSTRAINT `problem_stats_ibfk_1` FOREIGN KEY (`problem_id`) REFERENCES `problems` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `problem_stats`
--

LOCK TABLES `problem_stats` WRITE;
/*!40000 ALTER TABLE `problem_stats` DISABLE KEYS */;
INSERT INTO `problem_stats` VALUES (1,5,6,4,1224,817,1818),(2,10,2,1,695,695,695),(3,11,2,0,0,2147483647,0);
/*!40000 ALTER TABLE `problem_stats` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `problem_templates`
--

DROP TABLE IF EXISTS `problem_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `problem_templates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `problem_id` bigint NOT NULL,
  `language` varchar(20) NOT NULL,
  `user_template` text NOT NULL,
  `driver_code` text NOT NULL,
  PRIMARY KEY (`id`),
  KEY `problem_id` (`problem_id`),
  CONSTRAINT `problem_templates_ibfk_1` FOREIGN KEY (`problem_id`) REFERENCES `problems` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=66 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `problem_templates`
--

LOCK TABLES `problem_templates` WRITE;
/*!40000 ALTER TABLE `problem_templates` DISABLE KEYS */;
INSERT INTO `problem_templates` VALUES (35,5,'PYTHON','def twoSum(nums, target):\n    # Write your solution here\n    pass','import sys\ninput_data = sys.stdin.read().split(\'\\n\')\nnums = list(map(int, input_data[0].split()))\ntarget = int(input_data[1])\n\n{{USER_CODE}}\n\nresult = twoSum(nums, target)\nprint(result[0], result[1])'),(37,5,'CPP','vector<int> twoSum(vector<int>& nums, int target) {\n    // Write your solution here\n    return {};\n}','#include<bits/stdc++.h>\nusing namespace std;\n{{USER_CODE}}\nint main() {\n    string line; getline(cin, line);\n    istringstream iss(line);\n    vector<int> nums; int x;\n    while(iss >> x) nums.push_back(x);\n    int target; cin >> target;\n    vector<int> result = twoSum(nums, target);\n    cout << result[0] << \" \" << result[1] << endl;\n    return 0;\n}'),(38,6,'PYTHON','def isValid(s):\n    # Write your solution here\n    pass','import sys\ns = sys.stdin.read().strip()\n\n{{USER_CODE}}\n\nprint(str(isValid(s)).lower())'),(40,6,'CPP','bool isValid(string s) {\n    // Write your solution here\n    return false;\n}','#include<bits/stdc++.h>\nusing namespace std;\n{{USER_CODE}}\nint main() {\n    string s; getline(cin, s);\n    cout << (isValid(s) ? \"true\" : \"false\") << endl;\n    return 0;\n}'),(41,7,'PYTHON','def lengthOfLongestSubstring(s):\n    # Write your solution here\n    pass','import sys\ns = sys.stdin.read().strip()\n\n{{USER_CODE}}\n\nprint(lengthOfLongestSubstring(s))'),(43,7,'CPP','int lengthOfLongestSubstring(string s) {\n    // Write your solution here\n    return 0;\n}','#include<bits/stdc++.h>\nusing namespace std;\n{{USER_CODE}}\nint main() {\n    string s; getline(cin, s);\n    cout << lengthOfLongestSubstring(s) << endl;\n    return 0;\n}'),(47,5,'JAVA','class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        // Write your solution here\n        return new int[]{};\n    }\n}\n','import java.util.*;\n\nclass Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        String line = sc.nextLine().trim();\n        int aS = line.indexOf(\'[\');\n        int aE = line.indexOf(\']\');\n        String arrStr = line.substring(aS + 1, aE);\n        int[] nums = Arrays.stream(arrStr.split(\",\"))\n                .mapToInt(s -> Integer.parseInt(s.trim())).toArray();\n        int target = Integer.parseInt(line.substring(line.lastIndexOf(\'=\') + 1).trim());\n        Solution sol = new Solution();\n        int[] r = sol.twoSum(nums, target);\n        StringBuilder sb = new StringBuilder(\"[\");\n        for (int i = 0; i < r.length; i++) {\n            if (i > 0) sb.append(\",\");\n            sb.append(r[i]);\n        }\n        sb.append(\"]\");\n        System.out.println(sb);\n    }\n}\n\n{{USER_CODE}}\n'),(48,5,'JAVASCRIPT','var twoSum = function(nums, target) {\n    // Write your solution here\n\n};\n','{{USER_CODE}}\n\nlet _inp = \'\';\nprocess.stdin.on(\'data\', d => _inp += d);\nprocess.stdin.on(\'end\', () => {\n    const line = _inp.trim();\n    const arrMatch = line.match(/\\[(.*?)\\]/);\n    const nums = arrMatch[1].split(\',\').map(Number);\n    const target = parseInt(line.split(\'target=\')[1]);\n    const r = twoSum(nums, target);\n    console.log(\'[\' + r.join(\',\') + \']\');\n});\n'),(49,7,'JAVA','class Solution {\n    public int lengthOfLongestSubstring(String s) {\n        // Write your solution here\n        return 0;\n    }\n}\n','import java.util.*;\n\nclass Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        String line = sc.nextLine().trim();\n        String s = line.substring(line.indexOf(\'=\') + 1);\n        Solution sol = new Solution();\n        System.out.println(sol.lengthOfLongestSubstring(s));\n    }\n}\n\n{{USER_CODE}}\n'),(50,7,'JAVASCRIPT','var lengthOfLongestSubstring = function(s) {\n    // Write your solution here\n\n};\n','{{USER_CODE}}\n\nlet _inp = \'\';\nprocess.stdin.on(\'data\', d => _inp += d);\nprocess.stdin.on(\'end\', () => {\n    const line = _inp.trim();\n    const s = line.substring(line.indexOf(\'=\') + 1);\n    console.log(lengthOfLongestSubstring(s));\n});\n'),(53,6,'JAVA','class Solution {\n    public boolean isValid(String s) {\n        // Write your solution here\n        return false;\n    }\n}\n','import java.util.*;\n\nclass Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        String line = sc.nextLine().trim();\n        String s = line.substring(line.indexOf(\'=\') + 1);\n        Solution sol = new Solution();\n        System.out.println(sol.isValid(s));\n    }\n}\n\n{{USER_CODE}}\n'),(54,6,'JAVASCRIPT','var isValid = function(s) {\n    // Write your solution here\n\n};\n','{{USER_CODE}}\n\nlet _inp = \'\';\nprocess.stdin.on(\'data\', d => _inp += d);\nprocess.stdin.on(\'end\', () => {\n    const line = _inp.trim();\n    const s = line.substring(line.indexOf(\'=\') + 1);\n    console.log(isValid(s));\n});\n'),(59,10,'PYTHON','def findMedianSortedArrays(nums1, nums2):\n    # Write your solution here\n    pass','import sys\ndata = sys.stdin.read().split(\'\\n\')\nnums1 = list(map(int, data[0].split()))\nnums2 = list(map(int, data[1].split()))\n\n{{USER_CODE}}\n\nresult = findMedianSortedArrays(nums1, nums2)\nif result == int(result):\n    print(float(result))\nelse:\n    print(result)'),(60,10,'JAVA','public double findMedianSortedArrays(int[] nums1, int[] nums2) {\n    // Write your solution here\n    return 0.0;\n}','import java.util.*;\npublic class Main {\n    {{USER_CODE}}\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int[] nums1 = Arrays.stream(sc.nextLine().trim().split(\" \")).mapToInt(Integer::parseInt).toArray();\n        int[] nums2 = Arrays.stream(sc.nextLine().trim().split(\" \")).mapToInt(Integer::parseInt).toArray();\n        double result = new Main().findMedianSortedArrays(nums1, nums2);\n        if(result == (long)result) System.out.println(result);\n        else System.out.println(result);\n    }\n}'),(61,10,'CPP','double findMedianSortedArrays(vector<int>& nums1, vector<int>& nums2) {\n    // Write your solution here\n    return 0.0;\n}','#include<bits/stdc++.h>\nusing namespace std;\n{{USER_CODE}}\nint main(){\n    string l1,l2; getline(cin,l1); getline(cin,l2);\n    istringstream i1(l1),i2(l2);\n    vector<int> n1,n2; int x;\n    while(i1>>x) n1.push_back(x);\n    while(i2>>x) n2.push_back(x);\n    double r=findMedianSortedArrays(n1,n2);\n    if(r==(long long)r) cout<<fixed<<setprecision(1)<<r<<endl;\n    else cout<<r<<endl;\n}'),(62,11,'PYTHON','def longestPalindrome(s):\n    # Write your solution here\n    pass','import sys\ns = sys.stdin.read().strip()\n\n{{USER_CODE}}\n\nprint(longestPalindrome(s))'),(63,11,'JAVA','public String longestPalindrome(String s) {\n    // Write your solution here\n    return \"\";\n}','import java.util.*;\npublic class Main {\n    {{USER_CODE}}\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        System.out.println(new Main().longestPalindrome(sc.nextLine().trim()));\n    }\n}'),(64,11,'CPP','string longestPalindrome(string s) {\n    // Write your solution here\n    return \"\";\n}','#include<bits/stdc++.h>\nusing namespace std;\n{{USER_CODE}}\nint main(){\n    string s; getline(cin,s);\n    cout<<longestPalindrome(s)<<endl;\n}'),(65,10,'JAVASCRIPT','var findMedianSortedArrays = function(nums1, nums2) {\n    // Write your solution here\n\n};\n','{{USER_CODE}}\n\nlet _inp = \'\';\nprocess.stdin.on(\'data\', d => _inp += d);\nprocess.stdin.on(\'end\', () => {\n    const line = _inp.trim();\n    const matches = line.match(/\\[(.*?)\\]/g);\n    const nums1 = matches[0].slice(1,-1).split(\',\').filter(x=>x).map(Number);\n    const nums2 = matches[1].slice(1,-1).split(\',\').filter(x=>x).map(Number);\n    const r = findMedianSortedArrays(nums1, nums2);\n    console.log(Number.isInteger(r) ? r.toFixed(1) : r);\n});\n');
/*!40000 ALTER TABLE `problem_templates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `problems`
--

DROP TABLE IF EXISTS `problems`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `problems` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `slug` varchar(200) NOT NULL,
  `description` text NOT NULL,
  `difficulty` enum('EASY','MEDIUM','HARD') NOT NULL,
  `topic_tags` varchar(500) DEFAULT NULL,
  `company_tags` varchar(500) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `problems`
--

LOCK TABLES `problems` WRITE;
/*!40000 ALTER TABLE `problems` DISABLE KEYS */;
INSERT INTO `problems` VALUES (5,'Two Sum','two-sum','Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.','EASY','Array,HashMap','Google,Amazon','2026-04-07 14:11:16'),(6,'Valid Parentheses','valid-parentheses','Given a string s containing just the characters \'(\', \')\', \'{\', \'}\', \'[\' and \']\', determine if the input string is valid.','EASY','String,Stack','Google,Facebook','2026-04-07 14:11:52'),(7,'Longest Substring Without Repeating Characters','longest-substring-without-repeating','Given a string s, find the length of the longest substring without repeating characters.','MEDIUM','String,Sliding Window,HashMap','Google,Microsoft,Amazon','2026-04-07 14:12:05'),(10,'Median of Two Sorted Arrays','median-of-two-sorted-arrays','Given two sorted arrays nums1 and nums2, return the median of the two sorted arrays.','HARD','Array,Binary Search','Google,Apple','2026-04-14 13:55:22'),(11,'Longest Palindromic Substring','longest-palindromic-substring','Given a string s, return the longest palindromic substring in s.','MEDIUM','String,Dynamic Programming','Amazon,Microsoft','2026-04-14 13:55:51');
/*!40000 ALTER TABLE `problems` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `submissions`
--

DROP TABLE IF EXISTS `submissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `submissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `problem_id` bigint NOT NULL,
  `code` text NOT NULL,
  `language` varchar(20) NOT NULL,
  `status` enum('PENDING','RUNNING','ACCEPTED','WRONG_ANSWER','TIME_LIMIT','RUNTIME_ERROR','COMPILE_ERROR') NOT NULL DEFAULT 'PENDING',
  `runtime_ms` int DEFAULT NULL,
  `memory_kb` int DEFAULT NULL,
  `submitted_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `error_message` text,
  `failed_actual` text,
  `failed_expected` text,
  `failed_input` text,
  `passed_test_cases` int DEFAULT NULL,
  `total_test_cases` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `problem_id` (`problem_id`),
  CONSTRAINT `submissions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `submissions_ibfk_2` FOREIGN KEY (`problem_id`) REFERENCES `problems` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `submissions`
--

LOCK TABLES `submissions` WRITE;
/*!40000 ALTER TABLE `submissions` DISABLE KEYS */;
INSERT INTO `submissions` VALUES (1,6,5,'def twoSum(nums, target):\n    seen = {}\n    for i, n in enumerate(nums):\n        if target - n in seen:\n            return [seen[target - n], i]\n        seen[n] = i','PYTHON','ACCEPTED',753,0,'2026-04-08 03:27:17',NULL,NULL,NULL,NULL,NULL,NULL),(2,6,5,'def twoSum(nums, target):\n    return [0, 0]','PYTHON','WRONG_ANSWER',766,0,'2026-04-08 03:27:43',NULL,NULL,NULL,NULL,NULL,NULL),(3,6,5,'def twoSum(nums, target):\n    seen = {}\n    for i, n in enumerate(nums):\n        if target - n in seen:\n            return [seen[target-n], i]\n        seen[n] = i','PYTHON','ACCEPTED',966,0,'2026-04-08 08:10:37',NULL,NULL,NULL,NULL,NULL,NULL),(4,6,6,'public boolean isValid(String s) {\n     Stack<Character> stack = new Stack<>();\n\n        for(char c : s.toCharArray()) {\n\n            if(c == \'(\' || c == \'{\' || c == \'[\') {\n                stack.push(c);\n            } \n            else {\n                if(stack.isEmpty()) return false;\n\n                char top = stack.pop();\n\n                if(c == \')\' && top != \'(\') return false;\n                if(c == \'}\' && top != \'{\') return false;\n                if(c == \']\' && top != \'[\') return false;\n            }\n        }\n\n        return stack.isEmpty();\n    }\n    \n','JAVA','RUNTIME_ERROR',0,0,'2026-04-09 11:13:22',NULL,NULL,NULL,NULL,NULL,NULL),(5,6,7,'class Solution {\n    public int lengthOfLongestSubstring(String s) {\n        // Write your solution here\n        return 0;\n    }\n}\n','JAVA','WRONG_ANSWER',5992,0,'2026-04-09 12:30:39',NULL,NULL,NULL,NULL,NULL,NULL),(6,6,7,'class Solution {\n    public int lengthOfLongestSubstring(String s) {\n        Set<Character> set = new HashSet<>();\n        \n        int left = 0;\n        int maxLength = 0;\n\n        for (int right = 0; right < s.length(); right++) {\n\n            while (set.contains(s.charAt(right))) {\n                set.remove(s.charAt(left));\n                left++;\n            }\n\n            set.add(s.charAt(right));\n            maxLength = Math.max(maxLength, right - left + 1);\n        }\n\n        return maxLength;\n    }\n}','JAVA','ACCEPTED',5927,0,'2026-04-09 12:32:11',NULL,NULL,NULL,NULL,NULL,NULL),(7,6,7,'class Solution {\n    public int lengthOfLongestSubstring(String s) {\n        Set<Character> set = new HashSet<>();\n        \n        int left = 0;\n        int maxLength = 0;\n\n        for (int right = 0; right < s.length(); right++) {\n\n            while (set.contains(s.charAt(right))) {\n                set.remove(s.charAt(left));\n                left++;\n            }\n\n            set.add(s.charAt(right));\n            maxLength = Math.max(maxLength, right - left + 1);\n        }\n\n        return maxLength;\n    }\n}','JAVA','ACCEPTED',6818,0,'2026-04-09 12:34:02',NULL,NULL,NULL,NULL,NULL,NULL),(8,6,6,'class Solution {\n    public boolean isValid(String s) {\n        Stack<Character> stack = new Stack<>();\n\n        for(char c : s.toCharArray()) {\n\n            if(c == \'(\' || c == \'{\' || c == \'[\') {\n                stack.push(c);\n            } \n            else {\n                if(stack.isEmpty()) return false;\n\n                char top = stack.pop();\n\n                if(c == \')\' && top != \'(\') return false;\n                if(c == \'}\' && top != \'{\') return false;\n                if(c == \']\' && top != \'[\') return false;\n            }\n        }\n\n        return stack.isEmpty();\n    }\n}','JAVA','ACCEPTED',6043,0,'2026-04-09 13:16:33',NULL,NULL,NULL,NULL,NULL,NULL),(9,6,7,'class Solution {\n    public int lengthOfLongestSubstring(String s) {\n        Set<Character> set = new HashSet<>();\n        \n        int left = 0;\n        int maxLength = 0;\n\n        for (int right = 0; right < s.length(); right++) {\n\n            while (set.contains(s.charAt(right))) {\n                set.remove(s.charAt(left));\n                left++;\n            }\n\n            set.add(s.charAt(right));\n            maxLength = Math.max(maxLength, right - left + 1);\n        }\n\n        return maxLength;\n    }\n}','JAVA','ACCEPTED',7716,0,'2026-04-09 13:22:58',NULL,NULL,NULL,NULL,NULL,NULL),(10,6,5,'def twoSum(nums, target):\n    seen = {}\n    for i, n in enumerate(nums):\n        if target - n in seen:\n            return [seen[target-n], i]\n        seen[n] = i','PYTHON','ACCEPTED',1202,NULL,'2026-04-11 00:32:31',NULL,NULL,NULL,NULL,3,3),(11,6,5,'def twoSum(nums, target):\n    return [0, 0]','PYTHON','WRONG_ANSWER',1629,NULL,'2026-04-11 00:35:26',NULL,'0 0','0 1','2 7 11 15\n9',0,3),(12,6,5,'def twoSum(nums, target):\n    this is broken code!!!','PYTHON','RUNTIME_ERROR',741,NULL,'2026-04-11 00:37:59','Internal error: code execution failed',NULL,NULL,NULL,0,3),(13,6,5,'def twoSum(nums, target):\n    for i in range(len(nums)):\n        for j in range(i+1, len(nums)):\n            if nums[i]+nums[j]==target:\n                return [i,j]','PYTHON','ACCEPTED',1061,NULL,'2026-04-11 00:38:36',NULL,NULL,NULL,NULL,3,3),(14,6,10,'def findMedianSortedArrays(nums1, nums2):\n    merged = sorted(nums1 + nums2)\n    n = len(merged)\n    if n % 2 == 0:\n        return (merged[n//2-1] + merged[n//2]) / 2\n    return float(merged[n//2])','PYTHON','ACCEPTED',695,NULL,'2026-04-14 14:13:14',NULL,NULL,NULL,NULL,3,3),(15,6,11,'    public String longestPalindrome(String s) {\n        if (s == null || s.length() < 1) return \"\";\n\n        int start = 0, end = 0;\n\n        for (int i = 0; i < s.length(); i++) {\n            int len1 = expand(s, i, i);       // odd length palindrome\n            int len2 = expand(s, i, i + 1);   // even length palindrome\n            int len = Math.max(len1, len2);\n\n            if (len > end - start) {\n                start = i - (len - 1) / 2;\n                end = i + len / 2;\n            }\n        }\n\n        return s.substring(start, end + 1);\n    }\n\n    private int expand(String s, int left, int right) {\n        while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {\n            left--;\n            right++;\n        }\n        return right - left - 1;\n    }\n\n','JAVA','WRONG_ANSWER',2209,NULL,'2026-04-14 14:19:59',NULL,'aba','bab','babad',2,3),(16,6,11,'public String longestPalindrome(String s) {\n    \n    if(s==\"babad\")return\"bab\";\n    return \"bb\";\n}','JAVA','WRONG_ANSWER',2085,NULL,'2026-04-14 14:21:50',NULL,'bb','bab','babad',1,3),(17,6,10,'public double findMedianSortedArrays(int[] nums1, int[] nums2) {\n    // Write your solution here\n    return 0.0;\n}','JAVA','WRONG_ANSWER',2142,NULL,'2026-04-14 14:28:17',NULL,'0.0','2.0','1 3\n2',1,3),(18,6,5,'def twoSum(nums, target):\n    for i in range(len(nums)):\n        for j in range(i+1, len(nums)):\n            if nums[i]+nums[j]==target:\n                return [i,j]','PYTHON','ACCEPTED',817,NULL,'2026-05-04 10:55:31',NULL,NULL,NULL,NULL,3,3),(19,6,5,'def twoSum(nums, target):\n    for i in range(len(nums)):\n        for j in range(i+1, len(nums)):\n            if nums[i]+nums[j]==target:\n                return [i,j]','PYTHON','ACCEPTED',1818,NULL,'2026-05-04 11:00:40',NULL,NULL,NULL,NULL,3,3);
/*!40000 ALTER TABLE `submissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `test_cases`
--

DROP TABLE IF EXISTS `test_cases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_cases` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `problem_id` bigint NOT NULL,
  `input` text NOT NULL,
  `output` text NOT NULL,
  `is_hidden` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `problem_id` (`problem_id`),
  CONSTRAINT `test_cases_ibfk_1` FOREIGN KEY (`problem_id`) REFERENCES `problems` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_cases`
--

LOCK TABLES `test_cases` WRITE;
/*!40000 ALTER TABLE `test_cases` DISABLE KEYS */;
INSERT INTO `test_cases` VALUES (32,5,'2 7 11 15\n9','0 1',0),(33,5,'3 2 4\n6','1 2',0),(34,5,'3 3\n6','0 1',1),(35,6,'()','true',0),(36,6,'()[]{}','true',0),(37,6,'(]','false',1),(38,7,'abcabcbb','3',0),(39,7,'bbbbb','1',0),(40,7,'pwwkew','3',1),(45,10,'1 3\n2','2.0',0),(46,10,'1 2\n3 4','2.5',0),(47,10,'0 0\n0 0','0.0',1),(48,11,'babad','bab',0),(49,11,'cbbd','bb',0),(50,11,'racecar','racecar',1);
/*!40000 ALTER TABLE `test_cases` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_problems`
--

DROP TABLE IF EXISTS `user_problems`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_problems` (
  `user_id` bigint NOT NULL,
  `problem_id` bigint NOT NULL,
  `status` enum('SOLVED','ATTEMPTED') NOT NULL,
  `solved_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`,`problem_id`),
  KEY `problem_id` (`problem_id`),
  CONSTRAINT `user_problems_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `user_problems_ibfk_2` FOREIGN KEY (`problem_id`) REFERENCES `problems` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_problems`
--

LOCK TABLES `user_problems` WRITE;
/*!40000 ALTER TABLE `user_problems` DISABLE KEYS */;
INSERT INTO `user_problems` VALUES (6,5,'SOLVED','2026-05-04 11:00:48'),(6,6,'SOLVED','2026-04-09 13:16:33'),(6,7,'SOLVED','2026-04-09 13:22:58'),(6,10,'SOLVED','2026-04-14 14:13:25'),(6,11,'ATTEMPTED',NULL);
/*!40000 ALTER TABLE `user_problems` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `username` varchar(50) NOT NULL,
  `is_verified` tinyint(1) DEFAULT '0',
  `otp` varchar(6) DEFAULT NULL,
  `otp_expires_at` timestamp NULL DEFAULT NULL,
  `role` varchar(20) DEFAULT 'USER',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UK_r43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'2026-04-04 17:27:35.265921','ram@gmail.com','$2a$10$xa1BqWlC7nutXNUdxnviY.GzW2DeQVq.LZPKBJzRorHkkjJSLQchG','ram',0,NULL,NULL,'USER'),(2,'2026-04-04 17:37:01.157647','ram7@gmail.com','$2a$10$pThV31eSMcFXoUiksG4hRuu1t9ZbbDVqhF4Xvp27WPRcsr6Rdorzy','ram7',0,NULL,NULL,'USER'),(3,'2026-04-04 18:05:07.402718','bob@test.com','$2a$10$yigBT4eN2FJwhe.uII8S2e1795/283KXFddIbsFGQ4xMO5aMkVg4q','bob',0,NULL,NULL,'USER'),(4,'2026-04-04 18:21:03.445959','ram777@gmail.com','$2a$10$hX0NeT3gOYJD6uq9GWy2.OIgv9DQXKgd.822xGRI8WoKvD/lekKWi','ram777',0,NULL,NULL,'USER'),(5,'2026-04-04 18:47:06.929990','rs0666888999@gmail.com','$2a$10$W22KhqgIaD3kQQDENvXIH.LcXJMqAwYBxXBwZ1MMX6wRk3ZO/xKbS','ramsa',1,NULL,NULL,'USER'),(6,'2026-04-04 19:04:12.177048','rs0688699@gmail.com','$2a$10$WwrALFvDs5skc5QVi6kyMe0n0ggx6wsEnvU.K5iWTVTSH6XDDbgJS','ramsharma',1,NULL,NULL,'ADMIN'),(7,'2026-04-04 19:04:39.074250','ximofin502@fengnu.com','$2a$10$B1PaD9xhCyW3dfYLNoww8eFwOn22W3AYJ4r470NFEv0uVub3J53eq','ramsharma7',0,'605099','2026-04-04 13:44:39','USER'),(8,'2026-04-04 19:23:02.915654','202351129@iiitvadodara.ac.in','$2a$10$60MEM8Lj4Uxla1Z.qVn5G.euF0A5bi5LtnFR8yMZ14wL2yCaodPE.','ramshar',0,'214224','2026-04-04 14:03:03','USER'),(9,'2026-04-05 12:41:56.747856','harshil0502@gmail.com','$2a$10$CGvvSorE3DEJB3BcfX8OHOUfbMzSXvNTVciGxlrY3bpirNU9/U6bC','harshil',0,'925309','2026-04-05 07:21:57','USER'),(10,'2026-04-05 12:47:46.636291','202351064@iiitvadodara.ac.in','$2a$10$AsPbz..sFGSQwVtqrhaDxO23U8u6sfwSnrEjTObQnjXREK8N1Y2GG','harshil123',1,NULL,NULL,'USER'),(11,'2026-04-07 06:36:55.279998','kushsonawane22@gmail.com','$2a$10$YWrUkW4Qu7OZ4oc5Hi1GPuN1BApEM4YxOaqbiudtbRT.s.0lZGYAa','kush123',1,NULL,NULL,'USER');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-06 11:52:38
