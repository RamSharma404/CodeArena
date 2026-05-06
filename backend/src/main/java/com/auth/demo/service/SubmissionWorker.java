package com.auth.demo.service;

import com.auth.demo.model.*;
import com.auth.demo.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SubmissionWorker {

    private final SubmissionQueueService    queueService;
    private final SubmissionRepository      submissionRepository;
    private final TestCaseRepository        testCaseRepository;
    private final ProblemRepository         problemRepository;
    private final ProblemTemplateRepository templateRepository;
    private final UserProblemRepository     userProblemRepository;
    private final Judge0Service             judge0Service;
    private final RankingService            rankingService;

    public SubmissionWorker(
            SubmissionQueueService queueService,
            SubmissionRepository submissionRepository,
            TestCaseRepository testCaseRepository,
            ProblemRepository problemRepository,
            ProblemTemplateRepository templateRepository,
            UserProblemRepository userProblemRepository,
            Judge0Service judge0Service,
            RankingService rankingService) {
        this.queueService          = queueService;
        this.submissionRepository  = submissionRepository;
        this.testCaseRepository    = testCaseRepository;
        this.problemRepository     = problemRepository;
        this.templateRepository    = templateRepository;
        this.userProblemRepository = userProblemRepository;
        this.judge0Service         = judge0Service;
        this.rankingService        = rankingService;
    }

    // ─── Start 2 worker threads on startup ───────────────────
    @PostConstruct
    public void startWorker() {
        for (int i = 1; i <= 2; i++) {
            final int workerId = i;
            Thread worker = new Thread(() -> {
                System.out.println("Worker " + workerId + " started");
                while (true) {
                    try {
                        processNextJob(workerId);
                    } catch (Exception e) {
                        System.err.println("Worker " + workerId
                                + " error: " + e.getMessage());
                        try { Thread.sleep(1000); }
                        catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            });
            worker.setDaemon(true);
            worker.setName("submission-worker-" + workerId);
            worker.start();
        }
    }

    // ─── Process one job from queue ───────────────────────────
    private void processNextJob(int workerId) throws InterruptedException {
        String submissionIdStr = queueService.popFromQueue();

        if (submissionIdStr == null) {
            Thread.sleep(500);
            return;
        }

        Long submissionId = Long.parseLong(submissionIdStr);
        System.out.println("Worker " + workerId
                + " processing submission " + submissionId);

        Optional<Submission> submissionOpt =
                submissionRepository.findById(submissionId);

        if (submissionOpt.isEmpty()) {
            System.err.println("Submission "
                    + submissionId + " not found");
            return;
        }

        Submission submission = submissionOpt.get();

        // Mark as RUNNING
        submission.setStatus(Submission.Status.RUNNING);
        submissionRepository.save(submission);

        // Execute — ranking update happens inside after result is known
        executeSubmission(submission);
    }

    // ─── Sentinel used to delimit test cases in batched execution ────
    private static final String TC_SENTINEL = "---TC---";

    // ─── Execute against all test cases (single batched API call) ────
    private void executeSubmission(Submission submission) {
        try {
            List<TestCase> testCases = testCaseRepository
                    .findByProblemId(submission.getProblemId());

            if (testCases.isEmpty()) {
                submission.setStatus(Submission.Status.RUNTIME_ERROR);
                submission.setErrorMessage("No test cases found");
                submissionRepository.save(submission);
                return;
            }

            Optional<ProblemTemplate> template = templateRepository
                    .findByProblemIdAndLanguage(
                            submission.getProblemId(),
                            submission.getLanguage().toUpperCase());

            int    total          = testCases.size();
            int    passed         = 0;
            String finalStatus    = "ACCEPTED";
            String failedInput    = null;
            String failedExpected = null;
            String failedActual   = null;
            String error          = null;

            // ── 1. Get the per-test-case driver code ──────────────
            String baseDriver = template.isPresent()
                    ? template.get().getDriverCode()
                        .replace("{{USER_CODE}}",
                                SubmissionService.sanitizeUserCode(
                                        submission.getCode(),
                                        submission.getLanguage()))
                    : submission.getCode();

            // ── 2. Wrap the driver into a multi-test-case loop ────
            String lang       = submission.getLanguage().toLowerCase();
            String batchCode  = wrapDriverForBatch(baseDriver, lang);

            // ── 3. Build one combined stdin payload ───────────────
            StringBuilder stdinBuilder = new StringBuilder();
            stdinBuilder.append(total).append("\n");  // number of test cases
            for (int i = 0; i < testCases.size(); i++) {
                stdinBuilder.append(testCases.get(i).getInput());
                if (i < testCases.size() - 1)
                    stdinBuilder.append("\n").append(TC_SENTINEL).append("\n");
            }
            String batchStdin = stdinBuilder.toString();

            // ── 4. Single API call ────────────────────────────────
            long startTime = System.currentTimeMillis();
            Judge0Service.PistonResult result =
                    judge0Service.executeCode(batchCode, lang, batchStdin);
            long totalRuntime = System.currentTimeMillis() - startTime;

            String topStatus = judge0Service.mapStatus(result);

            // ── 5. Handle compile / TLE / runtime at global level ─
            if ("COMPILE_ERROR".equals(topStatus) ||
                    "TIME_LIMIT".equals(topStatus) ||
                    "RUNTIME_ERROR".equals(topStatus)) {
                finalStatus = topStatus;
                if (result.getRun() != null)
                    error = result.getRun().getStderr();
            } else {
                // ── 6. Split output and compare per test case ─────
                String rawOut   = result.getRun() != null
                        && result.getRun().getStdout() != null
                        ? result.getRun().getStdout() : "";
                String[] outputs = rawOut.split(TC_SENTINEL, -1);

                for (int i = 0; i < testCases.size(); i++) {
                    String actual   = i < outputs.length
                            ? normalize(outputs[i]) : "";
                    String expected = normalize(testCases.get(i).getOutput());

                    if (actual.equals(expected)) {
                        passed++;
                    } else if ("ACCEPTED".equals(finalStatus)) {
                        finalStatus    = "WRONG_ANSWER";
                        failedInput    = testCases.get(i).getInput();
                        failedExpected = expected;
                        failedActual   = actual;
                    }
                }
            }

            // totalRuntime = wall-clock of the single API call.
            // Divide by test-case count for a per-test approximation.
            Integer avgRuntime = total > 0
                    ? (int)(totalRuntime / total) : null;

            // Save final result
            submission.setStatus(
                    Submission.Status.valueOf(finalStatus));
            submission.setRuntimeMs(avgRuntime);
            submission.setTotalTestCases(total);
            submission.setPassedTestCases(passed);
            submission.setFailedInput(
                    "ACCEPTED".equals(finalStatus) ? null : failedInput);
            submission.setFailedExpected(
                    "ACCEPTED".equals(finalStatus) ? null : failedExpected);
            submission.setFailedActual(
                    "ACCEPTED".equals(finalStatus) ? null : failedActual);
            submission.setErrorMessage(error);
            submissionRepository.save(submission);

            // Update user_problems table
            updateUserProblemStatus(
                    submission.getUserId(),
                    submission.getProblemId(),
                    finalStatus);

            // ✅ Update ranking AFTER result is saved
            // finalStatus and avgRuntime are both known here
            rankingService.updateStats(
                    submission.getProblemId(),
                    finalStatus,
                    avgRuntime);

            System.out.println("Submission " + submission.getId()
                    + " completed: " + finalStatus);

        } catch (Exception e) {
            submission.setStatus(Submission.Status.RUNTIME_ERROR);
            submission.setErrorMessage(e.getMessage());
            submissionRepository.save(submission);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────
    private String normalize(String output) {
        if (output == null) return "";
        return output.trim()
                .replaceAll("\\r\\n", "\n")
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n+$", "");
    }

    /**
     * Wraps a single-test-case driver into a multi-test-case batch driver.
     *
     * Protocol (via stdin):
     *   Line 1  : number of test cases  (N)
     *   Lines 2+: test case inputs separated by the TC_SENTINEL line
     *
     * Protocol (via stdout):
     *   Outputs for each test case separated by TC_SENTINEL
     *
     * Strategy per language:
     *   - Replace the entry-point main() with a loop that (a) reads one block
     *     of lines until the next sentinel / EOF, (b) feeds it to the original
     *     logic, and (c) prints the sentinel between outputs.
     *
     * NOTE: The approach uses a BufferedReader wrapper so the original parsing
     *       code (which calls nextLine / input() etc.) just reads from the same
     *       stream transparently.
     */
    private String wrapDriverForBatch(String driverCode, String language) {
        switch (language) {

            // ── JAVA ─────────────────────────────────────────────────────────
            case "java": {
                // Replace:
                //   public static void main(String[] args) { ... }
                // with a loop that reads blocks separated by TC_SENTINEL.
                // We inject a helper that overrides Scanner to use a StringReader
                // for each block.
                String javaWrapper = """
// ═══════════════ BATCH HARNESS ═══════════════
import java.io.*;
import java.util.*;

public class __BatchRunner__ {
    static final String SENTINEL = "---TC---";
    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        int n = Integer.parseInt(br.readLine().trim());
        for (int __t__ = 0; __t__ < n; __t__++) {
            // Collect lines for this test case
            StringBuilder __block__ = new StringBuilder();
            String __ln__;
            while ((__ln__ = br.readLine()) != null) {
                if (__ln__.equals(SENTINEL)) break;
                if (__block__.length() > 0) __block__.append("\\n");
                __block__.append(__ln__);
            }
            // Run the original driver logic on this block
            __runOne__(__block__.toString());
            if (__t__ < n - 1) System.out.print(SENTINEL);
        }
    }
    static void __runOne__(String __input__) {
        Scanner sc = new Scanner(__input__);
""";
                // Extract all imports from driverCode
                StringBuilder imports = new StringBuilder();
                StringBuilder restOfCode = new StringBuilder();
                for (String line : driverCode.split("\n")) {
                    if (line.trim().startsWith("import ")) {
                        imports.append(line).append("\n");
                    } else {
                        restOfCode.append(line).append("\n");
                    }
                }
                
                String cleanDriverCode = restOfCode.toString();
                String mainBody = extractJavaMainBody(cleanDriverCode);
                String stripped = removeJavaMain(cleanDriverCode);
                
                return imports.toString() + javaWrapper + mainBody + "\n    }\n}\n" + stripped;
            }

            // ── PYTHON ───────────────────────────────────────────────────────
            case "python": {
                // Replace top-level input() calls with reads from a StringIO
                // by injecting a batch runner at the top.
                String pyWrapper = """
import sys as __sys__
__all_lines__ = __sys__.stdin.read().splitlines()
__tc_count__ = int(__all_lines__[0])
__line_idx__ = 1
__outputs__ = []

for __t__ in range(__tc_count__):
    __block_lines__ = []
    while __line_idx__ < len(__all_lines__) and __all_lines__[__line_idx__] != '---TC---':
        __block_lines__.append(__all_lines__[__line_idx__])
        __line_idx__ += 1
    __line_idx__ += 1  # skip sentinel
    import io as __io__
    __old_stdin__ = __sys__.stdin
    __sys__.stdin = __io__.StringIO('\\n'.join(__block_lines__))
    __old_stdout__ = __sys__.stdout
    __sys__.stdout = __io__.StringIO()
""";
                // Indent the original driver body and append it inside the loop
                String indented = indentBlock(driverCode, "    ");
                String pyFooter = """
    __captured__ = __sys__.stdout.getvalue()
    __sys__.stdout = __old_stdout__
    __sys__.stdin  = __old_stdin__
    __outputs__.append(__captured__.rstrip('\\n'))

print('---TC---'.join(__outputs__))
""";
                return pyWrapper + indented + pyFooter;
            }

            // ── C++ ──────────────────────────────────────────────────────────
            case "cpp": {
                String cppWrapper = """
// ═══ BATCH HARNESS ═══
#include <bits/stdc++.h>
using namespace std;
static const string SENTINEL = "---TC---";

void __runOne__(const string& block);

int main() {
    ios_base::sync_with_stdio(false);
    cin.tie(nullptr);
    string __firstLine__;
    getline(cin, __firstLine__);
    int __n__ = stoi(__firstLine__);
    for (int __t__ = 0; __t__ < __n__; __t__++) {
        string __block__ = "", __ln__;
        bool __first__ = true;
        while (getline(cin, __ln__)) {
            if (__ln__ == SENTINEL) break;
            if (!__first__) __block__ += '\\n';
            __block__ += __ln__;
            __first__ = false;
        }
        if (__t__ > 0) cout << SENTINEL;
        __runOne__(__block__);
    }
    return 0;
}

void __runOne__(const string& __input__) {
    istringstream cin(__input__);
""";
                // Extract the body of main() from the driver, replace with our loop.
                String mainBody = extractCppMainBody(driverCode);
                String stripped = removeCppMain(driverCode);
                return stripped + cppWrapper + mainBody + "\n}\n";
            }

            // ── JAVASCRIPT / TYPESCRIPT ───────────────────────────────────────
            case "javascript":
            case "typescript": {
                // JS drivers use process.stdin 'end' event to collect all input.
                // We replace that pattern with a synchronous block-based loop.
                String jsWrapper = """
const __lines__ = require('fs').readFileSync('/dev/stdin','utf8').split('\\n');
let __idx__ = 1;
const __n__ = parseInt(__lines__[0]);
const __outputs__ = [];
const __SENT__ = '---TC---';

for (let __t__ = 0; __t__ < __n__; __t__++) {
    const __blockLines__ = [];
    while (__idx__ < __lines__.length && __lines__[__idx__] !== __SENT__) {
        __blockLines__.push(__lines__[__idx__++]);
    }
    __idx__++; // skip sentinel
    const __inp__ = __blockLines__.join('\\n');
    __outputs__.push(__runOne__(__inp__));
}
process.stdout.write(__outputs__.join(__SENT__));

function __runOne__(__input__) {
    const __capturedOut__ = [];
    const __origLog__ = console.log;
    console.log = (...a) => __capturedOut__.push(a.map(String).join(' '));
""";
                // Replace the stdin event pattern and extract the logic
                String jsBody = extractJsBody(driverCode);
                String jsFooter = """
    console.log = __origLog__;
    return __capturedOut__.join('\\n');
}
""";
                // Extract the user code portion (before the stdin listener)
                String userPart = extractJsUserCode(driverCode);
                return userPart + jsWrapper + jsBody + jsFooter;
            }

            // ── GO / RUST / unknown: fall back to single-call mode ────────────
            default:
                return driverCode;
        }
    }

    // ── Java extraction helpers ───────────────────────────────────────
    private String extractJavaMainBody(String code) {
        // Find "public static void main" and extract its brace-balanced body
        int start = code.indexOf("public static void main");
        if (start < 0) return "// [main body not found]";
        int open = code.indexOf('{', start);
        if (open < 0) return "";
        int depth = 1, i = open + 1;
        while (i < code.length() && depth > 0) {
            char c = code.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            i++;
        }
        return code.substring(open + 1, i - 1);
    }

    private String removeJavaMain(String code) {
        int start = code.indexOf("public static void main");
        if (start < 0) return code;
        // Walk back to find any annotations / modifiers on same line
        int open = code.indexOf('{', start);
        if (open < 0) return code;
        int depth = 1, i = open + 1;
        while (i < code.length() && depth > 0) {
            char c = code.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            i++;
        }
        return code.substring(0, start) + code.substring(i);
    }

    // ── C++ extraction helpers ────────────────────────────────────────
    private String extractCppMainBody(String code) {
        int start = code.indexOf("int main(");
        if (start < 0) start = code.indexOf("int main (");
        if (start < 0) return "// [main not found]";
        int open = code.indexOf('{', start);
        if (open < 0) return "";
        int depth = 1, i = open + 1;
        while (i < code.length() && depth > 0) {
            char c = code.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            i++;
        }
        return code.substring(open + 1, i - 1);
    }

    private String removeCppMain(String code) {
        int start = code.indexOf("int main(");
        if (start < 0) start = code.indexOf("int main (");
        if (start < 0) return code;
        int open = code.indexOf('{', start);
        if (open < 0) return code;
        int depth = 1, i = open + 1;
        while (i < code.length() && depth > 0) {
            char c = code.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            i++;
        }
        return code.substring(0, start) + code.substring(i);
    }

    // ── JS extraction helpers ─────────────────────────────────────────
    /** Returns the code block that runs inside the stdin 'end' callback. */
    private String extractJsBody(String code) {
        int idx = code.indexOf("process.stdin.on('end'");
        if (idx < 0) idx = code.indexOf("process.stdin.on(\"end\"");
        if (idx < 0) return "    // [stdin body not found]";
        int open = code.indexOf('{', idx);
        if (open < 0) return "";
        int depth = 1, i = open + 1;
        while (i < code.length() && depth > 0) {
            char c = code.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            i++;
        }
        // Indent each line by 4 spaces for the function body
        String body = code.substring(open + 1, i - 1);
        // Replace references to _inp with __inp__
        body = body.replaceAll("_inp", "__inp__");
        return indentBlock(body, "    ");
    }

    /** Returns the JS code that appears before the process.stdin listener — i.e., the user's function. */
    private String extractJsUserCode(String code) {
        int idx = code.indexOf("let _inp");
        if (idx < 0) idx = code.indexOf("var _inp");
        if (idx < 0) idx = code.indexOf("process.stdin");
        if (idx < 0) return code;
        return code.substring(0, idx);
    }

    /** Adds a uniform indent prefix to every non-empty line of a block. */
    private String indentBlock(String block, String prefix) {
        if (block == null || block.isEmpty()) return block;
        StringBuilder sb = new StringBuilder();
        for (String line : block.split("\n", -1)) {
            sb.append(prefix).append(line).append("\n");
        }
        return sb.toString();
    }

    private void updateUserProblemStatus(
            Long userId, Long problemId, String status) {
        UserProblem.UserProblemId id =
                new UserProblem.UserProblemId(userId, problemId);
        Optional<UserProblem> existing = userProblemRepository
                .findByIdUserIdAndIdProblemId(userId, problemId);

        if ("ACCEPTED".equals(status)) {
            UserProblem up = existing.orElse(new UserProblem());
            up.setId(id);
            up.setStatus(UserProblem.Status.SOLVED);
            up.setSolvedAt(LocalDateTime.now());
            userProblemRepository.save(up);
        } else if (existing.isEmpty()) {
            UserProblem up = new UserProblem();
            up.setId(id);
            up.setStatus(UserProblem.Status.ATTEMPTED);
            userProblemRepository.save(up);
        }
    }
}