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
    public static final String TC_SENTINEL = "`";

    // Maximum test cases per API call to avoid output truncation
    private static final int BATCH_SIZE = 50;

    // ─── Execute against all test cases (chunked batches) ────
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

            // Build the full executable code (driver + user code)
            String execCode = template.isPresent()
                    ? template.get().getDriverCode()
                        .replace("{{USER_CODE}}",
                                SubmissionService.sanitizeUserCode(
                                        submission.getCode(),
                                        submission.getLanguage()))
                    : submission.getCode();

            String lang = submission.getLanguage().toLowerCase();
            double totalExecutionTimeSecs = 0.0;  // actual code execution time from sandbox

            // Transform single-testcase driver to batch driver
            execCode = wrapDriverForBatch(execCode, submission.getLanguage().toLowerCase());

            final String finalExecCode = execCode;
            final String finalLang = lang;

            // ─── DEBUG LOGGING ────────────────────────────────────
            System.out.println("═══════════════════════════════════════");
            System.out.println("[Worker] Submission " + submission.getId()
                    + " | lang=" + lang + " | tests=" + total);
            System.out.println("[Worker] Template found: " + template.isPresent());
            System.out.println("[Worker] Code length: " + execCode.length());
            // ─── END DEBUG LOGGING ────────────────────────────────

            // ─── DYNAMIC CHUNKED BATCH EXECUTION ─────────────────────────
            // Split test cases into chunks based on BATCH_SIZE and MAX_PAYLOAD_SIZE (50KB)
            // to avoid API output truncation and "Input exceeds maximum size of 100KB" errors.
            java.util.List<java.util.List<TestCase>> batches = new java.util.ArrayList<>();
            java.util.List<TestCase> currentBatch = new java.util.ArrayList<>();
            int currentBatchBytes = 0;
            int BATCH_SIZE = 300;
            for (TestCase tc : testCases) {
                String in = tc.getInput() != null ? tc.getInput() : "";
                int bytes = in.length() + TC_SENTINEL.length() + 2;
                if (currentBatch.size() >= BATCH_SIZE || currentBatchBytes + bytes > 75000) {
                    if (!currentBatch.isEmpty()) {
                        batches.add(currentBatch);
                        currentBatch = new java.util.ArrayList<>();
                        currentBatchBytes = 0;
                    }
                }
                currentBatch.add(tc);
                currentBatchBytes += bytes;
            }
            if (!currentBatch.isEmpty()) {
                batches.add(currentBatch);
            }

            int totalBatches = batches.size();
            System.out.println("[Worker] Splitting into " + totalBatches + " batches (max size 70KB, max count " + BATCH_SIZE + ")");

            java.util.concurrent.atomic.AtomicInteger passedCounter = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicReference<String> finalStatusRef = new java.util.concurrent.atomic.AtomicReference<>("ACCEPTED");
            java.util.concurrent.atomic.AtomicReference<String> failedInputRef = new java.util.concurrent.atomic.AtomicReference<>(null);
            java.util.concurrent.atomic.AtomicReference<String> failedExpectedRef = new java.util.concurrent.atomic.AtomicReference<>(null);
            java.util.concurrent.atomic.AtomicReference<String> failedActualRef = new java.util.concurrent.atomic.AtomicReference<>(null);
            java.util.concurrent.atomic.AtomicReference<String> errorRef = new java.util.concurrent.atomic.AtomicReference<>(null);
            java.util.concurrent.atomic.AtomicReference<Double> totalExecTime = new java.util.concurrent.atomic.AtomicReference<>(0.0);

            // Use 4 threads for concurrent execution
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(4);
            java.util.List<java.util.concurrent.CompletableFuture<Void>> futures = new java.util.ArrayList<>();

            for (int batchIdx = 0; batchIdx < totalBatches; batchIdx++) {
                final int currentBatchIdx = batchIdx;
                java.util.List<TestCase> batchTCs = batches.get(batchIdx);
                
                java.util.concurrent.CompletableFuture<Void> future = java.util.concurrent.CompletableFuture.runAsync(() -> {
                    // Short-circuit if another batch already failed
                    if (!"ACCEPTED".equals(finalStatusRef.get())) return;

                    StringBuilder batchedInput = new StringBuilder();
                    int batchSize = batchTCs.size();
                    batchedInput.append(batchSize).append("\n");
                    for (TestCase tc : batchTCs) {
                        String in = tc.getInput() != null ? tc.getInput() : "";
                        batchedInput.append(in);
                        if (!in.endsWith("\n")) batchedInput.append("\n");
                        batchedInput.append(TC_SENTINEL).append("\n");
                    }

                    Judge0Service.PistonResult result = judge0Service.executeCode(finalExecCode, finalLang, batchedInput.toString());

                    if (result.getRun() != null) {
                        String rawErr = result.getRun().getStderr() != null ? result.getRun().getStderr() : "";
                        double batchExecMs = 0;
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("EXEC_TIME:([0-9.]+)").matcher(rawErr);
                        if (m.find()) {
                            batchExecMs = Double.parseDouble(m.group(1));
                            rawErr = rawErr.replace(m.group(0), "").trim();
                            result.getRun().setStderr(rawErr.isEmpty() ? null : rawErr);
                        } else {
                            batchExecMs = result.getRun().getExecutionTimeSeconds() * 1000.0;
                        }
                        final double batchTimeSecsFinal = batchExecMs / 1000.0;
                        totalExecTime.updateAndGet(v -> v + batchTimeSecsFinal);
                    }

                    String status = judge0Service.mapStatus(result);

                    if ("COMPILE_ERROR".equals(status) || "TIME_LIMIT".equals(status)) {
                        if (finalStatusRef.compareAndSet("ACCEPTED", status)) {
                            TestCase tc = batchTCs.get(0);
                            failedInputRef.set(tc.getInput());
                            failedExpectedRef.set(normalize(tc.getOutput()));
                            failedActualRef.set("");
                            String err = "";
                            if (result.getRun() != null && result.getRun().getStderr() != null) err = result.getRun().getStderr();
                            else if (result.getRun() != null && result.getRun().getStdout() != null) err = result.getRun().getStdout();
                            errorRef.set(err);
                        }
                        return;
                    }

                    String rawOut = result.getRun() != null && result.getRun().getStdout() != null ? result.getRun().getStdout() : "";
                    String localError = null;
                    int errIdx = rawOut.indexOf("\n---ERROR---\n");
                    if (errIdx != -1) {
                        localError = rawOut.substring(errIdx + 13).trim();
                        rawOut = rawOut.substring(0, errIdx);
                    }

                    // split with Pattern.quote to handle any metacharacters
                    String[] outputs = rawOut.split("\\n?" + java.util.regex.Pattern.quote(TC_SENTINEL) + "\\n?");

                    for (int i = 0; i < batchTCs.size(); i++) {
                        TestCase tc = batchTCs.get(i);
                        String expected = normalize(tc.getOutput());

                        if (i < outputs.length && (localError == null || i < outputs.length - 1)) {
                            String actual = normalize(outputs[i]);
                            if (actual.equals(expected)) {
                                passedCounter.incrementAndGet();
                            } else {
                                if (finalStatusRef.compareAndSet("ACCEPTED", "WRONG_ANSWER")) {
                                    failedInputRef.set(tc.getInput());
                                    failedExpectedRef.set(expected);
                                    failedActualRef.set(actual);
                                }
                                return;
                            }
                        } else {
                            if (localError != null && localError.contains("Time Limit Exceeded")) {
                                if (finalStatusRef.compareAndSet("ACCEPTED", "TIME_LIMIT_EXCEEDED")) {
                                    failedInputRef.set(tc.getInput());
                                }
                            } else {
                                if (finalStatusRef.compareAndSet("ACCEPTED", "RUNTIME_ERROR")) {
                                    failedInputRef.set(tc.getInput());
                                    errorRef.set(localError != null ? localError : "Output truncated. Possible memory limit or crash.");
                                }
                            }
                            return;
                        }
                    }
                }, executor);
                
                futures.add(future);
            }

            // Wait for all batches to finish
            try {
                java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();
            } catch (Exception e) {
                if (finalStatusRef.compareAndSet("ACCEPTED", "RUNTIME_ERROR")) {
                    errorRef.set(e.getMessage());
                }
            } finally {
                executor.shutdown();
            }

            passed = passedCounter.get();
            finalStatus = finalStatusRef.get();
            failedInput = failedInputRef.get();
            failedExpected = failedExpectedRef.get();
            failedActual = failedActualRef.get();
            error = errorRef.get();
            totalExecutionTimeSecs = totalExecTime.get();
            // ─── Calculate LeetCode-style runtime ─────────────────────
            double userCodeTimeSecs = totalExecutionTimeSecs;
            if (lang.equalsIgnoreCase("java") || lang.equalsIgnoreCase("cpp")) userCodeTimeSecs /= 500.0;
            else userCodeTimeSecs /= 200.0;
            if (userCodeTimeSecs < 0) userCodeTimeSecs = 0;
            
            // Convert to milliseconds
            Integer runtimeMs = (int) Math.round(userCodeTimeSecs * 1000);

            // Save final result
            submission.setStatus(
                    Submission.Status.valueOf(finalStatus));
            submission.setRuntimeMs(runtimeMs);
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

            // Update ranking
            rankingService.updateStats(
                    submission.getProblemId(),
                    finalStatus,
                    runtimeMs);

            System.out.println("Submission " + submission.getId()
                    + " completed: " + finalStatus
                    + " (" + passed + "/" + total + " passed)");

        } catch (Exception e) {
            System.err.println("[Worker] Exception: " + e.getMessage());
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
    public static String wrapDriverForBatch(String driverCode, String language) {
        switch (language) {

            // ── JAVA ─────────────────────────────────────────────────────────
            case "java": {
                // Strategy: rebuild class Main in-place with a batch loop.
                // Key constraints of the OnlineCompiler API:
                //   1. Must NOT use "throws Exception" on main()
                //   2. Must use "class Main" (not a custom name)

                // 1. Extract all imports from driverCode
                StringBuilder imports = new StringBuilder();
                StringBuilder restOfCode = new StringBuilder();
                for (String line : driverCode.split("\n")) {
                    if (line.trim().startsWith("import ")) {
                        imports.append(line).append("\n");
                    } else {
                        restOfCode.append(line).append("\n");
                    }
                }

                String cleanDriverCode = restOfCode.toString().replace("System.out.println(", "System.out.print(");

                // 2. Extract the body of original main()
                String mainBody = extractJavaMainBody(cleanDriverCode);

                // 3. Strip the entire original Main class (not just main method)
                String stripped = removeJavaClass(cleanDriverCode, "Main");

                // 4. In the original main body, redirect Scanner from System.in
                //    to the per-test-case __input__ string
                mainBody = mainBody.replace("new Scanner(System.in)", "new Scanner(__input__)");

                // 5. Build a new Main class with the batch harness
                String batchMain = """
class Main {
    static final String SENTINEL = "`";
    public static void main(String[] args) {
        try {
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(System.in));
            String line = br.readLine();
            if (line == null) return;
            int n = Integer.parseInt(line.trim());
            double __totalTimeMs = 0;
            java.util.concurrent.ExecutorService __executor = java.util.concurrent.Executors.newSingleThreadExecutor();
            for (int __t__ = 0; __t__ < n; __t__++) {
                StringBuilder __block__ = new StringBuilder();
                String __ln__;
                while ((__ln__ = br.readLine()) != null) {
                    if (__ln__.equals(SENTINEL)) break;
                    if (__block__.length() > 0) __block__.append("\\n");
                    __block__.append(__ln__);
                }
                try {
                    java.util.concurrent.Future<Double> __future = __executor.submit(() -> {
                        long __start = System.nanoTime();
                        __runOne__(__block__.toString() + "\\n");
                        long __end = System.nanoTime();
                        return (double) (__end - __start) / 1000000.0;
                    });
                    __totalTimeMs += __future.get(2000, java.util.concurrent.TimeUnit.MILLISECONDS);
                } catch (java.util.concurrent.TimeoutException __te) {
                    System.out.print("\\n---ERROR---\\nTime Limit Exceeded\\n");
                    System.out.flush();
                    System.exit(0);
                } catch (Throwable t) {
                    System.out.print("\\n---ERROR---\\n");
                    t.printStackTrace(System.out);
                    System.out.flush();
                    System.exit(0);
                }
                if (__t__ < n - 1) System.out.print(SENTINEL);
            }
            __executor.shutdownNow();
            System.err.println("EXEC_TIME:" + __totalTimeMs);
            System.out.flush();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.out.flush();
            System.exit(0);
        }
    }
    static void __runOne__(String __input__) {
""";
                return imports.toString()
                        + "import java.io.*;\nimport java.util.*;\n\n"
                        + batchMain + mainBody + "\n    }\n}\n" + stripped;
            }

            case "python": {
                // Replace top-level input() calls with reads from a StringIO
                // by injecting a batch runner at the top.
                String pyWrapper = """
import sys as __sys__
__all_lines__ = __sys__.stdin.read().splitlines()
__tc_count__ = int(__all_lines__[0])
__line_idx__ = 1
__outputs__ = []
import time
__total_time_ms = 0

for __t__ in range(__tc_count__):
    __block_lines__ = []
    while __line_idx__ < len(__all_lines__) and __all_lines__[__line_idx__] != '`':
        __block_lines__.append(__all_lines__[__line_idx__])
        __line_idx__ += 1
    __line_idx__ += 1  # skip sentinel
    import io as __io__
    __old_stdin__ = __sys__.stdin
    __sys__.stdin = __io__.StringIO('\\n'.join(__block_lines__))
    __old_stdout__ = __sys__.stdout
    __sys__.stdout = __io__.StringIO()
    try:
        import signal
        def __handler(signum, frame):
            raise TimeoutError("Time Limit Exceeded")
        signal.signal(signal.SIGALRM, __handler)
        signal.alarm(2)
        __start = time.perf_counter()
""";
                // Indent the original driver body and append it inside the loop
                String indented = indentBlock(driverCode, "        ");
                String pyFooter = """
        signal.alarm(0)
        __end = time.perf_counter()
        __total_time_ms += (__end - __start) * 1000
    except TimeoutError:
        __sys__.stdout.write('\\n---ERROR---\\nTime Limit Exceeded\\n')
        print("`")
        __sys__.stdout.flush()
        __sys__.exit(0)
    except Exception as e:
        import traceback
        __sys__.stdout.write('\\n---ERROR---\\n')
        traceback.print_exc(file=__sys__.stdout)
        __sys__.stdout.flush()
        __sys__.exit(0)
        
    __captured__ = __sys__.stdout.getvalue()
    __sys__.stdout = __old_stdout__
    __sys__.stdin  = __old_stdin__
    __outputs__.append(__captured__.strip())

print('`'.join(__outputs__), end='')
import sys
sys.stderr.write(f"EXEC_TIME:{__total_time_ms}\\n")
sys.stderr.flush()

""";
                return pyWrapper + indented + pyFooter;
            }

            // ── C++ ──────────────────────────────────────────────────────────
            case "cpp": {
                String cppWrapper = """
// ═══ BATCH HARNESS ═══
#include <bits/stdc++.h>
#include <future>
#include <chrono>
#include <thread>
using namespace std;
static const string SENTINEL = "`";
static const int BATCH_SIZE = 150;

void __runOne__(const string& block);

int main() {
    ios_base::sync_with_stdio(false);
    cin.tie(nullptr);
    string __firstLine__;
    getline(cin, __firstLine__);
    int __n__ = stoi(__firstLine__);
    double __totalTimeMs = 0;
    for (int __t__ = 0; __t__ < __n__; __t__++) {
        if (__t__ > 0 && __t__ % BATCH_SIZE == 0) std::this_thread::sleep_for(std::chrono::milliseconds(100));
        string __block__ = "", __ln__;
        bool __first__ = true;
        while (getline(cin, __ln__)) {
            if (__ln__ == SENTINEL) break;
            if (!__first__) __block__ += '\\n';
            __block__ += __ln__;
            __first__ = false;
        }
        if (__t__ > 0) cout << SENTINEL;
        try {
            auto __start = std::chrono::high_resolution_clock::now();
            auto __f = std::async(std::launch::async, [&]() {
                __runOne__(__block__);
            });
            if (__f.wait_for(std::chrono::milliseconds(2000)) == std::future_status::timeout) {
                cout << "\\n---ERROR---\\nTime Limit Exceeded\\n";
                cout.flush();
                exit(0);
            } else {
                auto __end = std::chrono::high_resolution_clock::now();
                __totalTimeMs += std::chrono::duration<double, std::milli>(__end - __start).count();
            }
        } catch (const std::exception& e) {
            cout << "\\n---ERROR---\\n" << e.what() << "\\n";
            cout.flush();
            exit(0);
        } catch (...) {
            cout << "\\n---ERROR---\\nUnknown C++ exception\\n";
            cout.flush();
            exit(0);
        }
    }
    cerr << "EXEC_TIME:" << __totalTimeMs << "\\n";
    return 0;
}

void __runOne__(const string& __input__) {
    istringstream cin(__input__);
""";
                // Extract the body of main() from the driver, replace with our loop.
                String mainBody = extractCppMainBody(driverCode)
                                    .replace("<< endl", "")
                                    .replaceAll("return\\s+0\\s*;", "");
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
const __SENT__ = '`';
const __BATCH_SIZE__ = 150;
let __totalTimeMs = 0;

for (let __t__ = 0; __t__ < __n__; __t__++) {
    if (__t__ > 0 && __t__ % __BATCH_SIZE__ == 0) require('child_process').execSync('sleep 0.1');
    const __blockLines__ = [];
    while (__idx__ < __lines__.length && __lines__[__idx__] !== __SENT__) {
        __blockLines__.push(__lines__[__idx__++]);
    }
    __idx__++; // skip sentinel
    const __inp__ = __blockLines__.join('\\n');
    try {
        const __start = process.hrtime.bigint();
        __outputs__.push(__runOne__(__inp__));
        const __end = process.hrtime.bigint();
        __totalTimeMs += Number(__end - __start) / 1000000.0;
    } catch (e) {
        __outputs__.push("\\n---ERROR---\\n" + (e.stack || e.toString()));
        break;
    }
}
process.stdout.write(__outputs__.map(x => x.trim()).join('`'));
console.error("EXEC_TIME:" + __totalTimeMs);

function __runOne__(__inp__) {
    const __capturedOut__ = [];
    const __origLog__ = console.log;
    console.log = (...a) => { if(a.join(' ') === '`') return; __capturedOut__.push(a.map(String).join(' ')); };
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
    public static String extractJavaMainBody(String code) {
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

    public static String removeJavaMain(String code) {
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

    /** Removes an entire class declaration (by name) from the code. */
    public static String removeJavaClass(String code, String className) {
        // Match "class ClassName" (with optional "public" prefix)
        int idx = code.indexOf("class " + className);
        if (idx < 0) return code;
        // Walk back to capture "public " if present
        int classStart = idx;
        if (idx > 7 && code.substring(idx - 7, idx).trim().equals("public"))
            classStart = idx - 7;
        // Find opening brace
        int open = code.indexOf('{', idx);
        if (open < 0) return code;
        int depth = 1, i = open + 1;
        while (i < code.length() && depth > 0) {
            char c = code.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            i++;
        }
        return code.substring(0, classStart) + code.substring(i);
    }

    // ── C++ extraction helpers ────────────────────────────────────────
    public static String extractCppMainBody(String code) {
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

    public static String removeCppMain(String code) {
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
    public static String extractJsBody(String code) {
        int idx = code.indexOf("function __main__()");
        if (idx < 0) return "    // [__main__ body not found]";
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
    public static String extractJsUserCode(String code) {
        int idx = code.indexOf("const _inp");
        if (idx < 0) idx = code.indexOf("let _inp");
        if (idx < 0) idx = code.indexOf("var _inp");
        if (idx < 0) idx = code.indexOf("process.stdin");
        if (idx < 0) return code;
        return code.substring(0, idx);
    }

    /** Adds a uniform indent prefix to every non-empty line of a block. */
    public static String indentBlock(String block, String prefix) {
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