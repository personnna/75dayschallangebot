package com.habitbot.dsa;

import java.util.List;

public final class ChallengeCatalog {

    private ChallengeCatalog() {}

    private static final List<String> ARRAYS_PATTERNS = List.of(
            "Frequency maps",
            "Running sums",
            "In-place updates",
            "Array transformation",
            "Lookup optimization"
    );

    private static final List<String> ARRAYS_PROBLEMS = List.of(
            "Two Sum",
            "Group Anagrams",
            "Product of Array Except Self",
            "Subarray Sum Equals K"
    );

    private static final List<String> TWO_POINTERS_PATTERNS = List.of(
            "Shrinking windows",
            "Expanding windows",
            "Sorted-array scanning",
            "Duplicate removal",
            "Running frequency tracking"
    );

    private static final List<String> TWO_POINTERS_PROBLEMS = List.of(
            "Three Sum",
            "Container With Most Water",
            "Longest Substring Without Repeating Characters",
            "Minimum Window Substring"
    );

    private static final List<String> LINKED_LIST_PATTERNS = List.of(
            "Dummy nodes",
            "Pointer rewiring",
            "Cycle detection",
            "Middle-node discovery",
            "In-place reversal"
    );

    private static final List<String> LINKED_LIST_PROBLEMS = List.of(
            "Reverse Linked List",
            "Merge Two Sorted Lists",
            "Linked List Cycle",
            "Reorder List"
    );

    private static final List<String> STACK_PATTERNS = List.of(
            "Last-in-first-out processing",
            "Next greater element",
            "Previous smaller element",
            "Expression evaluation",
            "Window maximum tracking"
    );

    private static final List<String> STACK_PROBLEMS = List.of(
            "Valid Parentheses",
            "Min Stack",
            "Daily Temperatures",
            "Sliding Window Maximum"
    );

    private static final List<String> BINARY_SEARCH_PATTERNS = List.of(
            "Search-space reduction",
            "Lower and upper bounds",
            "Feasibility checking",
            "Interval merging",
            "Custom sorting"
    );

    private static final List<String> BINARY_SEARCH_PROBLEMS = List.of(
            "Search in Rotated Sorted Array",
            "Koko Eating Bananas",
            "Merge Intervals",
            "Meeting Rooms"
    );

    private static final List<String> BACKTRACKING_PATTERNS = List.of(
            "Choose, explore, unchoose",
            "Decision trees",
            "State restoration",
            "Pruning invalid paths",
            "Duplicate handling"
    );

    private static final List<String> BACKTRACKING_PROBLEMS = List.of(
            "Subsets",
            "Permutations",
            "Combination Sum",
            "Word Search",
            "N-Queens"
    );

    private static final List<String> TREE_PATTERNS = List.of(
            "Preorder, inorder, postorder",
            "Level-order traversal",
            "Recursive information passing",
            "Lowest common ancestor",
            "Tree construction"
    );

    private static final List<String> TREE_PROBLEMS = List.of(
            "Maximum Depth of Binary Tree",
            "Diameter of Binary Tree",
            "Validate Binary Search Tree",
            "Lowest Common Ancestor",
            "Serialize and Deserialize Binary Tree"
    );

    private static final List<String> HEAP_TRIE_PATTERNS = List.of(
            "Priority processing",
            "Running median",
            "Top-K selection",
            "Prefix searching",
            "Multi-list merging"
    );

    private static final List<String> HEAP_TRIE_PROBLEMS = List.of(
            "Kth Largest Element in an Array",
            "Top K Frequent Elements",
            "Find Median from Data Stream",
            "Merge K Sorted Lists",
            "Design Add and Search Words Data Structure"
    );

    private static final List<String> GRAPH_PATTERNS = List.of(
            "Visited sets",
            "Cycle detection",
            "Multi-source BFS",
            "Dependency ordering",
            "Grid-to-graph conversion"
    );

    private static final List<String> GRAPH_PROBLEMS = List.of(
            "Number of Islands",
            "Clone Graph",
            "Course Schedule",
            "Rotting Oranges",
            "Network Delay Time"
    );

    private static final List<String> DP_PATTERNS = List.of(
            "What state changes?",
            "What choices are available?",
            "Which subproblems repeat?",
            "What is the recurrence relation?",
            "Can memory usage be reduced?"
    );

    private static final List<String> DP_PROBLEMS = List.of(
            "House Robber",
            "Coin Change",
            "Longest Common Subsequence",
            "Jump Game"
    );

    private static final List<String> INTERVIEW_PATTERNS = List.of(
            "Clarify requirements",
            "Discuss brute force",
            "Identify the pattern",
            "Explain the optimized approach",
            "Write clean code",
            "Test edge cases",
            "State complexity"
    );

    private static final List<String> INTERVIEW_PROBLEMS = List.of(
            "Complete 10 company-tagged problems and one realistic mock interview"
    );

    private static final List<String> FINAL_CHECKLIST = List.of(
            "Recognize common patterns quickly",
            "Explain brute-force and optimized solutions",
            "Write compilable code without external help",
            "Test normal and edge cases",
            "Calculate time and space complexity",
            "Communicate clearly while coding",
            "Recover calmly when your first approach fails"
    );

    private static ChallengeDay day(
            int day,
            String section,
            String topic,
            List<String> patterns,
            List<String> problems
    ) {
        return new ChallengeDay(day, section, topic, patterns, problems);
    }

    private static final List<ChallengeDay> DAYS = List.of(
            day(1, "Build the Foundations", "Time and space complexity", List.of(), List.of()),
            day(2, "Build the Foundations", "Big O, Omega, and Theta notation", List.of(), List.of()),
            day(3, "Build the Foundations", "Arrays and memory representation", List.of(), List.of()),
            day(4, "Build the Foundations", "Strings and common operations", List.of(), List.of()),
            day(5, "Build the Foundations", "Recursion fundamentals", List.of(), List.of()),
            day(6, "Build the Foundations", "Mixed practice and revision", List.of(), List.of()),

            day(7, "Arrays, Strings & Hashing", "Array traversal and manipulation", ARRAYS_PATTERNS, ARRAYS_PROBLEMS),
            day(8, "Arrays, Strings & Hashing", "Frequency counting with hash maps", ARRAYS_PATTERNS, ARRAYS_PROBLEMS),
            day(9, "Arrays, Strings & Hashing", "Sets and duplicate detection", ARRAYS_PATTERNS, ARRAYS_PROBLEMS),
            day(10, "Arrays, Strings & Hashing", "Prefix and suffix arrays", ARRAYS_PATTERNS, ARRAYS_PROBLEMS),
            day(11, "Arrays, Strings & Hashing", "Prefix sums and subarray problems", ARRAYS_PATTERNS, ARRAYS_PROBLEMS),
            day(12, "Arrays, Strings & Hashing", "Mixed timed practice", ARRAYS_PATTERNS, ARRAYS_PROBLEMS),

            day(13, "Two Pointers & Sliding Window", "Opposite-direction pointers", TWO_POINTERS_PATTERNS, TWO_POINTERS_PROBLEMS),
            day(14, "Two Pointers & Sliding Window", "Same-direction pointers", TWO_POINTERS_PATTERNS, TWO_POINTERS_PROBLEMS),
            day(15, "Two Pointers & Sliding Window", "Fixed-size sliding windows", TWO_POINTERS_PATTERNS, TWO_POINTERS_PROBLEMS),
            day(16, "Two Pointers & Sliding Window", "Variable-size sliding windows", TWO_POINTERS_PATTERNS, TWO_POINTERS_PROBLEMS),
            day(17, "Two Pointers & Sliding Window", "Substring and subarray patterns", TWO_POINTERS_PATTERNS, TWO_POINTERS_PROBLEMS),
            day(18, "Two Pointers & Sliding Window", "Revision and timed practice", TWO_POINTERS_PATTERNS, TWO_POINTERS_PROBLEMS),

            day(19, "Linked Lists", "Singly and doubly linked lists", LINKED_LIST_PATTERNS, LINKED_LIST_PROBLEMS),
            day(20, "Linked Lists", "Reversing a linked list", LINKED_LIST_PATTERNS, LINKED_LIST_PROBLEMS),
            day(21, "Linked Lists", "Fast and slow pointers", LINKED_LIST_PATTERNS, LINKED_LIST_PROBLEMS),
            day(22, "Linked Lists", "Merging and sorting lists", LINKED_LIST_PATTERNS, LINKED_LIST_PROBLEMS),
            day(23, "Linked Lists", "List intersection and cycle detection", LINKED_LIST_PATTERNS, LINKED_LIST_PROBLEMS),
            day(24, "Linked Lists", "Mixed practice and revision", LINKED_LIST_PATTERNS, LINKED_LIST_PROBLEMS),

            day(25, "Stacks, Queues & Monotonic Patterns", "Stack and queue fundamentals", STACK_PATTERNS, STACK_PROBLEMS),
            day(26, "Stacks, Queues & Monotonic Patterns", "Expression and bracket problems", STACK_PATTERNS, STACK_PROBLEMS),
            day(27, "Stacks, Queues & Monotonic Patterns", "Monotonic increasing stacks", STACK_PATTERNS, STACK_PROBLEMS),
            day(28, "Stacks, Queues & Monotonic Patterns", "Monotonic decreasing stacks", STACK_PATTERNS, STACK_PROBLEMS),
            day(29, "Stacks, Queues & Monotonic Patterns", "Deques and window maximums", STACK_PATTERNS, STACK_PROBLEMS),
            day(30, "Stacks, Queues & Monotonic Patterns", "Revision and timed practice", STACK_PATTERNS, STACK_PROBLEMS),

            day(31, "Binary Search, Sorting & Intervals", "Standard binary search", BINARY_SEARCH_PATTERNS, BINARY_SEARCH_PROBLEMS),
            day(32, "Binary Search, Sorting & Intervals", "First and last occurrence", BINARY_SEARCH_PATTERNS, BINARY_SEARCH_PROBLEMS),
            day(33, "Binary Search, Sorting & Intervals", "Binary search on answers", BINARY_SEARCH_PATTERNS, BINARY_SEARCH_PROBLEMS),
            day(34, "Binary Search, Sorting & Intervals", "Sorting techniques and comparators", BINARY_SEARCH_PATTERNS, BINARY_SEARCH_PROBLEMS),
            day(35, "Binary Search, Sorting & Intervals", "Intervals and overlapping ranges", BINARY_SEARCH_PATTERNS, BINARY_SEARCH_PROBLEMS),
            day(36, "Binary Search, Sorting & Intervals", "Mixed practice and revision", BINARY_SEARCH_PATTERNS, BINARY_SEARCH_PROBLEMS),

            day(37, "Recursion & Backtracking", "Recursion trees and base cases", BACKTRACKING_PATTERNS, BACKTRACKING_PROBLEMS),
            day(38, "Recursion & Backtracking", "Subsets and combinations", BACKTRACKING_PATTERNS, BACKTRACKING_PROBLEMS),
            day(39, "Recursion & Backtracking", "Permutations", BACKTRACKING_PATTERNS, BACKTRACKING_PROBLEMS),
            day(40, "Recursion & Backtracking", "Grid-based backtracking", BACKTRACKING_PATTERNS, BACKTRACKING_PROBLEMS),
            day(41, "Recursion & Backtracking", "Constraint-based search", BACKTRACKING_PATTERNS, BACKTRACKING_PROBLEMS),
            day(42, "Recursion & Backtracking", "Revision and timed practice", BACKTRACKING_PATTERNS, BACKTRACKING_PROBLEMS),

            day(43, "Trees & Binary Search Trees", "Tree terminology and representation", TREE_PATTERNS, TREE_PROBLEMS),
            day(44, "Trees & Binary Search Trees", "DFS traversals", TREE_PATTERNS, TREE_PROBLEMS),
            day(45, "Trees & Binary Search Trees", "Breadth-first traversal", TREE_PATTERNS, TREE_PROBLEMS),
            day(46, "Trees & Binary Search Trees", "Tree depth and diameter", TREE_PATTERNS, TREE_PROBLEMS),
            day(47, "Trees & Binary Search Trees", "Binary search tree properties", TREE_PATTERNS, TREE_PROBLEMS),
            day(48, "Trees & Binary Search Trees", "Lowest Common Ancestor & Tree Construction", TREE_PATTERNS, TREE_PROBLEMS),

            day(49, "Heaps & Tries", "Min-heaps and max-heaps", HEAP_TRIE_PATTERNS, HEAP_TRIE_PROBLEMS),
            day(50, "Heaps & Tries", "Top-K element patterns", HEAP_TRIE_PATTERNS, HEAP_TRIE_PROBLEMS),
            day(51, "Heaps & Tries", "Two-heap problems", HEAP_TRIE_PATTERNS, HEAP_TRIE_PROBLEMS),
            day(52, "Heaps & Tries", "K-way merging", HEAP_TRIE_PATTERNS, HEAP_TRIE_PROBLEMS),
            day(53, "Heaps & Tries", "Trie construction and search", HEAP_TRIE_PATTERNS, HEAP_TRIE_PROBLEMS),
            day(54, "Heaps & Tries", "Revision and timed practice", HEAP_TRIE_PATTERNS, HEAP_TRIE_PROBLEMS),

            day(55, "Graphs", "Graph representation", GRAPH_PATTERNS, GRAPH_PROBLEMS),
            day(56, "Graphs", "Breadth-first and depth-first search", GRAPH_PATTERNS, GRAPH_PROBLEMS),
            day(57, "Graphs", "Connected components", GRAPH_PATTERNS, GRAPH_PROBLEMS),
            day(58, "Graphs", "Topological sorting", GRAPH_PATTERNS, GRAPH_PROBLEMS),
            day(59, "Graphs", "Shortest-path fundamentals", GRAPH_PATTERNS, GRAPH_PROBLEMS),
            day(60, "Graphs", "Mixed practice and revision", GRAPH_PATTERNS, GRAPH_PROBLEMS),

            day(61, "Dynamic Programming & Greedy", "Memoization and tabulation", DP_PATTERNS, DP_PROBLEMS),
            day(62, "Dynamic Programming & Greedy", "One-dimensional dynamic programming", DP_PATTERNS, DP_PROBLEMS),
            day(63, "Dynamic Programming & Greedy", "Two-dimensional dynamic programming", DP_PATTERNS, DP_PROBLEMS),
            day(64, "Dynamic Programming & Greedy", "Knapsack and subsequence patterns", DP_PATTERNS, DP_PROBLEMS),
            day(65, "Dynamic Programming & Greedy", "Greedy decision-making", DP_PATTERNS, DP_PROBLEMS),
            day(66, "Dynamic Programming & Greedy", "Revision and pattern comparison", DP_PATTERNS, DP_PROBLEMS),

            day(67, "Company-Style Interview Practice", "Meta-style arrays, graphs, and communication", INTERVIEW_PATTERNS, INTERVIEW_PROBLEMS),
            day(68, "Company-Style Interview Practice", "Microsoft-style trees, recursion, and edge cases", INTERVIEW_PATTERNS, INTERVIEW_PROBLEMS),
            day(69, "Company-Style Interview Practice", "Amazon-style hashing, heaps, and practical scenarios", INTERVIEW_PATTERNS, INTERVIEW_PROBLEMS),
            day(70, "Company-Style Interview Practice", "Complete one full mock interview", INTERVIEW_PATTERNS, INTERVIEW_PROBLEMS),
            day(71, "Company-Style Interview Practice", "Analyze mistakes and repeat failed problems", INTERVIEW_PATTERNS, INTERVIEW_PROBLEMS),

            day(72, "The Final Interview Sprint", "Revise arrays, strings, hashing, and sliding windows", FINAL_CHECKLIST, List.of()),
            day(73, "The Final Interview Sprint", "Revise trees, graphs, heaps, and binary search", FINAL_CHECKLIST, List.of()),
            day(74, "The Final Interview Sprint", "Complete two timed mock interviews with verbal explanations", FINAL_CHECKLIST, List.of()),
            day(75, "The Final Interview Sprint", "Review mistakes, templates, complexities, and recurring patterns", FINAL_CHECKLIST, List.of())
    );

    public static ChallengeDay getDay(int day) {
        if (day < 1 || day > 75) {
            throw new IllegalArgumentException("Challenge day must be between 1 and 75.");
        }
        return DAYS.get(day - 1);
    }

    public static List<ChallengeDay> allDays() {
        return DAYS;
    }
}
